package com.lifespan.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lifespan.app.LifeSpanApp
import com.lifespan.app.R
import com.lifespan.app.data.BatteryReader
import com.lifespan.app.domain.alert.AlertEvaluator
import com.lifespan.app.domain.alert.AlertThresholds
import com.lifespan.app.domain.alert.AlertType
import com.lifespan.app.domain.alert.TemperatureTrendMonitor
import com.lifespan.app.domain.battery.TimeEstimator
import com.lifespan.app.domain.bubble.BubbleStatusEvaluator
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.ui.MainActivity
import com.lifespan.app.util.AlertManager
import com.lifespan.app.widget.LifeSpanWidgetProvider
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Lightweight sticky foreground service that listens for `ACTION_BATTERY_CHANGED`,
 * derives a [BatterySnapshot] (including instantaneous current), persists
 * telemetry, evaluates overheat / charge-limit / rapid-rise alerts, drives the
 * charging bubble and home-screen widget, and keeps a live status notification.
 */
class BatteryMonitorService : LifecycleService() {

    private lateinit var app: LifeSpanApp
    private lateinit var batteryManager: BatteryManager
    private lateinit var alertManager: AlertManager
    private lateinit var bubble: ChargingBubbleController
    private val trendMonitor = TemperatureTrendMonitor()

    @Volatile
    private var thresholds: AlertThresholds = AlertThresholds()

    @Volatile
    private var bubbleEnabled: Boolean = true

    @Volatile
    private var persistentAlarmEnabled: Boolean = false

    @Volatile
    private var rapidRiseEnabled: Boolean = true

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                handleBatteryIntent(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = application as LifeSpanApp
        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        alertManager = AlertManager(this)
        bubble = ChargingBubbleController(this)
        app.container.batteryRepository.setMonitoring(true)

        lifecycleScope.launch {
            app.container.settingsRepository.appSettings.collect { settings ->
                thresholds = settings.thresholds
                bubbleEnabled = settings.bubbleEnabled
                persistentAlarmEnabled = settings.persistentChargeAlarm
                rapidRiseEnabled = settings.rapidRiseEnabled
                if (!bubbleEnabled) bubble.remove()
                if (!persistentAlarmEnabled) alertManager.stopPersistentAlarm()
            }
        }
        lifecycleScope.launch { app.container.batteryRepository.restoreActiveSession() }
        lifecycleScope.launch { app.container.batteryRepository.pruneOldTelemetry() }

        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                alertManager.stopPersistentAlarm()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_DISMISS_ALARM -> {
                alertManager.stopPersistentAlarm()
            }
        }
        startForeground(app.container.batteryRepository.latest.value, emptySet())
        return START_STICKY
    }

    private fun handleBatteryIntent(intent: Intent) {
        val snapshot = BatteryReader.fromIntent(intent, batteryManager)
        lifecycleScope.launch { app.container.batteryRepository.record(snapshot) }

        val alerts = AlertEvaluator.evaluate(snapshot, thresholds).toMutableSet()
        if (rapidRiseEnabled && trendMonitor.add(snapshot.timestamp, snapshot.temperatureCelsius)) {
            alerts += AlertType.RAPID_TEMP_RISE
        }

        if (alerts.isNotEmpty()) alertManager.maybeAlert(alerts)

        if (persistentAlarmEnabled && snapshot.isCharging && AlertType.CHARGE_LIMIT in alerts) {
            alertManager.startPersistentAlarm()
        } else if (!snapshot.isCharging || AlertType.CHARGE_LIMIT !in alerts) {
            alertManager.stopPersistentAlarm()
        }

        startForeground(snapshot, alerts)
        updateBubble(snapshot)
        LifeSpanWidgetProvider.update(this, snapshot)
    }

    private fun updateBubble(snapshot: BatterySnapshot) {
        if (bubbleEnabled && snapshot.isCharging) {
            bubble.render(BubbleStatusEvaluator.evaluate(snapshot, thresholds))
        } else {
            bubble.remove()
        }
    }

    private fun startForeground(snapshot: BatterySnapshot?, alerts: Set<AlertType>) {
        val notification = buildNotification(snapshot, alerts)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun buildNotification(snapshot: BatterySnapshot?, alerts: Set<AlertType>): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, BatteryMonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val title: String
        val text: String
        if (snapshot == null) {
            title = "LifeSpan monitoring"
            text = "Waiting for the first battery reading…"
        } else {
            val watts = String.format(Locale.US, "%.2f W", snapshot.powerWatts)
            val temp = String.format(Locale.US, "%.1f°C", snapshot.temperatureCelsius)
            val ma = String.format(Locale.US, "%.0f mA", snapshot.currentMa)
            title = "${snapshot.level}%  ·  $temp  ·  $watts"
            text = buildString {
                append(if (snapshot.isCharging) "Charging (${snapshot.plugType.label})" else "On battery")
                append("  ·  ")
                append(ma)
                timeEstimateLabel(snapshot)?.let { append("  ·  ").append(it) }
                if (alerts.isNotEmpty()) {
                    append("  ·  ⚠ ")
                    append(alerts.joinToString(", ") { it.readableLabel() })
                }
            }
        }

        val builder = NotificationCompat.Builder(this, LifeSpanApp.CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_monitor)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Stop", stopIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (alertManager.persistentAlarmActive) {
            val dismissIntent = PendingIntent.getService(
                this,
                2,
                Intent(this, BatteryMonitorService::class.java).setAction(ACTION_DISMISS_ALARM),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(0, "Dismiss alarm", dismissIntent)
        }

        return builder.build()
    }

    private fun timeEstimateLabel(snapshot: BatterySnapshot): String? {
        val minutes = TimeEstimator.estimateMinutes(
            chargeCounterMicroAh = snapshot.chargeCounterMicroAh,
            currentMicroA = snapshot.currentUa,
            levelPercent = snapshot.level,
            isCharging = snapshot.isCharging,
        ) ?: return null
        val formatted = TimeEstimator.formatMinutes(minutes)
        return if (snapshot.isCharging) "full in $formatted" else "empty in $formatted"
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(batteryReceiver) }
        bubble.remove()
        alertManager.stopPersistentAlarm()
        app.container.batteryRepository.setMonitoring(false)
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.lifespan.app.action.STOP_MONITORING"
        const val ACTION_DISMISS_ALARM = "com.lifespan.app.action.DISMISS_ALARM"

        fun start(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java)
                .setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}

private fun AlertType.readableLabel(): String = when (this) {
    AlertType.OVERHEAT -> "Overheat"
    AlertType.CHARGE_LIMIT -> "Charge limit"
    AlertType.RAPID_TEMP_RISE -> "Rapid heating"
}
