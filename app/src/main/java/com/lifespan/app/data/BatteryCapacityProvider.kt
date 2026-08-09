package com.lifespan.app.data

import android.content.Context
import java.io.File

/**
 * Resolves the battery's factory design capacity in mAh, which Android does not
 * expose through a public API.
 *
 * Two sources are tried in order: the framework's internal `PowerProfile`, then
 * the kernel's power-supply node. Both are best-effort — when neither answers,
 * capacity estimates are reported without a state-of-health percentage rather
 * than against a guessed rating.
 */
class BatteryCapacityProvider(private val context: Context) {

    private val resolved: Double? by lazy { fromPowerProfile() ?: fromSysfs() }

    fun designCapacityMah(): Double? = resolved

    @Suppress("PrivateApi")
    private fun fromPowerProfile(): Double? = runCatching {
        val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
        val powerProfile = powerProfileClass
            .getConstructor(Context::class.java)
            .newInstance(context)
        val capacity = powerProfileClass
            .getMethod("getBatteryCapacity")
            .invoke(powerProfile) as? Double
        capacity?.takeIf { it > 0.0 }
    }.getOrNull()

    /** Kernel nodes report design charge in microamp-hours. */
    private fun fromSysfs(): Double? = SYSFS_DESIGN_CAPACITY_PATHS
        .asSequence()
        .mapNotNull { path ->
            runCatching {
                val raw = File(path).takeIf { it.canRead() }?.readText()?.trim()
                raw?.toLongOrNull()?.takeIf { it > 0L }
            }.getOrNull()
        }
        .map { microAmpHours -> microAmpHours / 1000.0 }
        .firstOrNull { it >= MIN_PLAUSIBLE_MAH }

    private companion object {
        val SYSFS_DESIGN_CAPACITY_PATHS = listOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/bms/charge_full_design",
        )

        /** Guards against nodes that report in mAh already, or report junk. */
        const val MIN_PLAUSIBLE_MAH = 100.0
    }
}
