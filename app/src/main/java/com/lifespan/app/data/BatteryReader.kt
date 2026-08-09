package com.lifespan.app.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.lifespan.app.domain.battery.BatteryCalculator
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType

/**
 * Builds a [BatterySnapshot] from `ACTION_BATTERY_CHANGED` extras plus live
 * [BatteryManager] properties. Shared by the monitoring service, the periodic
 * sampling worker, and the home-screen widget.
 */
object BatteryReader {

    fun fromIntent(intent: Intent, batteryManager: BatteryManager): BatterySnapshot {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        val currentUa = BatteryCalculator.sanitizeCurrentUa(
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
        )
        val chargeCounter = BatteryCalculator.sanitizeChargeCounterMicroAh(
            batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
        )

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
            chargeCounterMicroAh = chargeCounter,
        )
    }

    /** One-shot read using the sticky battery-changed broadcast. */
    fun sample(context: Context): BatterySnapshot? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val batteryManager =
            context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        return fromIntent(intent, batteryManager)
    }
}
