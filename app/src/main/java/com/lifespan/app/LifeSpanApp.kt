package com.lifespan.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.lifespan.app.data.db.LifeSpanDatabase
import com.lifespan.app.data.prefs.SettingsRepository
import com.lifespan.app.data.repository.BatteryRepository

/** Minimal manual dependency container shared across the app process. */
class AppContainer(context: Context) {
    private val database = LifeSpanDatabase.getInstance(context)
    val batteryRepository = BatteryRepository(
        database.chargeSessionDao(),
        database.telemetryLogDao(),
    )
    val settingsRepository = SettingsRepository(context)
}

class LifeSpanApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
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
