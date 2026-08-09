package com.lifespan.app

import android.os.BatteryManager
import com.lifespan.app.domain.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Test

class PlugTypeTest {

    @Test
    fun mapsKnownPlugValues() {
        assertEquals(PlugType.AC, PlugType.fromPluggedExtra(BatteryManager.BATTERY_PLUGGED_AC))
        assertEquals(PlugType.USB, PlugType.fromPluggedExtra(BatteryManager.BATTERY_PLUGGED_USB))
        assertEquals(PlugType.WIRELESS, PlugType.fromPluggedExtra(BatteryManager.BATTERY_PLUGGED_WIRELESS))
    }

    @Test
    fun mapsDockPlugValue() {
        // BatteryManager.BATTERY_PLUGGED_DOCK (API 33) == 8.
        assertEquals(PlugType.DOCK, PlugType.fromPluggedExtra(8))
    }

    @Test
    fun zeroMeansUnplugged() {
        assertEquals(PlugType.UNPLUGGED, PlugType.fromPluggedExtra(0))
    }
}
