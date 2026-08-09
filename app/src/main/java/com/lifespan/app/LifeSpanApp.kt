package com.lifespan.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.Configuration
import com.lifespan.app.data.db.LifeSpanDatabase
import com.lifespan.app.data.prefs.SettingsRepository
import com.lifespan.app.data.repository.BatteryRepository
import com.lifespan.app.data.usage.AppUsageRepository
import com.lifespan.app.work.SamplingScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Minimal manual dependency container shared across the app process. */
class AppContainer(context: Context) {
    private val database = LifeSpanDatabase.getInstance(context)
    val batteryRepository = BatteryRepository(
        database.chargeSessionDao(),
        database.telemetryLogDao(),
    )
    val settingsRepository = SettingsRepository(context)
    val appUsageRepository = AppUsageRepository(context.applicationContext)
}

class LifeSpanApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()

        // Keep periodic sampling scheduled in sync with the user's preference.
        appScope.launch {
            container.settingsRepository.appSettings
                .map { it.periodicSamplingEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) SamplingScheduler.schedule(this@LifeSpanApp)
                    else SamplingScheduler.cancel(this@LifeSpanApp)
                }
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val monitor = NotificationChannel(
            CHANNEL_MONITOR,
            getString(R.string.monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.monitor_channel_desc)
            setShowBadge(false)
        }

        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.alert_channel_desc)
        }

        manager.createNotificationChannels(listOf(monitor, alerts))
    }

    companion object {
        const val CHANNEL_MONITOR = "battery_monitor"
        const val CHANNEL_ALERTS = "battery_alerts"
    }
}
