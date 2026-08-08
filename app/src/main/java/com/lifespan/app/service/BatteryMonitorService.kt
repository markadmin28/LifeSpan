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
import com.lifespan.app.domain.alert.AlertEvaluator
import com.lifespan.app.domain.alert.AlertThresholds
import com.lifespan.app.domain.alert.AlertType
import com.lifespan.app.domain.battery.BatteryCalculator
import com.lifespan.app.domain.bubble.BubbleStatusEvaluator
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType
import com.lifespan.app.ui.MainActivity
import com.lifespan.app.util.AlertManager
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Lightweight sticky foreground service that listens for
 * `ACTION_BATTERY_CHANGED`, derives a [BatterySnapshot] (including instantaneous
 * current via [BatteryManager.BATTERY_PROPERTY_CURRENT_NOW]), persists telemetry,
 * evaluates overheat / charge-limit alerts, and keeps a live status notification.
 */
class BatteryMonitorService : LifecycleService() {

    private lateinit var app: LifeSpanApp
    private lateinit var batteryManager: BatteryManager
    private lateinit var alertManager: AlertManager
    private lateinit var bubble: ChargingBubbleController

    @Volatile
    private var thresholds: AlertThresholds = AlertThresholds()

    @Volatile
    private var bubbleEnabled: Boolean = true

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
            app.container.settingsRepository.thresholds.collect { thresholds = it }
        }
        lifecycleScope.launch {
            app.container.settingsRepository.bubbleEnabled.collect { enabled ->
                bubbleEnabled = enabled
                if (!enabled) bubble.remove()
            }
        }
        lifecycleScope.launch { app.container.batteryRepository.restoreActiveSession() }

        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(app.container.batteryRepository.latest.value, emptySet())
        return START_STICKY
    }

    private fun handleBatteryIntent(intent: Intent) {
        val snapshot = readSnapshot(intent)
        lifecycleScope.launch { app.container.batteryRepository.record(snapshot) }

        val alerts = AlertEvaluator.evaluate(snapshot, thresholds)
        if (alerts.isNotEmpty()) alertManager.maybeAlert(alerts)
        startForeground(snapshot, alerts)
        updateBubble(snapshot)
    }

    private fun updateBubble(snapshot: BatterySnapshot) {
        if (bubbleEnabled && snapshot.isCharging) {
            bubble.render(BubbleStatusEvaluator.evaluate(snapshot, thresholds))
        } else {
            bubble.remove()
        }
    }

    private fun readSnapshot(intent: Intent): BatterySnapshot {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        val currentUa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

        val isCharging = plugged > 0 ||
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        return BatterySnapshot(
            timestamp = System.currentTimeMillis(),
            level = BatteryCalculator.levelPercent(level, scale),
            voltageMv = voltageMv,
            currentUa = currentUa,
            temperatureCelsius = BatteryCalculator.tenthsCelsiusToCelsius(tempTenths),
            isCharging = isCharging,
            plugType = PlugType.fromPluggedExtra(plugged),
            health = health,
            technology = technology,
        )
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
                if (alerts.isNotEmpty()) {
                    append("  ·  ⚠ ")
                    append(alerts.joinToString(", ") { it.readableLabel() })
                }
            }
        }

        return NotificationCompat.Builder(this, LifeSpanApp.CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Stop", stopIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(batteryReceiver) }
        bubble.remove()
        app.container.batteryRepository.setMonitoring(false)
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.lifespan.app.action.STOP_MONITORING"

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
}
