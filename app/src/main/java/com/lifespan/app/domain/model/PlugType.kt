package com.lifespan.app.domain.model

import android.os.BatteryManager

/** How the device is currently receiving power. */
enum class PlugType(val label: String) {
    AC("AC"),
    USB("USB"),
    WIRELESS("Wireless"),
    DOCK("Dock"),
    UNPLUGGED("Unplugged"),
    UNKNOWN("Unknown");

    companion object {
        /**
         * Map the `BatteryManager.EXTRA_PLUGGED` bitmask from
         * `ACTION_BATTERY_CHANGED` to a [PlugType]. A value of `0` means the
         * device is running on battery.
         */
        fun fromPluggedExtra(plugged: Int): PlugType = when {
            plugged <= 0 -> UNPLUGGED
            plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> AC
            plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> USB
            plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> WIRELESS
            else -> UNKNOWN
        }
    }
}
