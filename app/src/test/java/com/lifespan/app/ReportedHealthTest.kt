package com.lifespan.app

import android.os.BatteryManager
import com.lifespan.app.domain.model.ReportedHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportedHealthTest {

    @Test
    fun labelsKnownHealthValues() {
        assertEquals("Good", ReportedHealth.label(BatteryManager.BATTERY_HEALTH_GOOD))
        assertEquals("Overheat", ReportedHealth.label(BatteryManager.BATTERY_HEALTH_OVERHEAT))
        assertEquals("Dead", ReportedHealth.label(BatteryManager.BATTERY_HEALTH_DEAD))
        assertEquals("Over voltage", ReportedHealth.label(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE))
        assertEquals("Failure", ReportedHealth.label(BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE))
        assertEquals("Cold", ReportedHealth.label(BatteryManager.BATTERY_HEALTH_COLD))
    }

    @Test
    fun unknownAndUnreportedHealthHaveNoLabel() {
        assertNull(ReportedHealth.label(BatteryManager.BATTERY_HEALTH_UNKNOWN))
        assertNull(ReportedHealth.label(0))
        assertNull(ReportedHealth.label(-1))
    }

    @Test
    fun flagsConcerningHealthStates() {
        assertFalse(ReportedHealth.isConcerning(BatteryManager.BATTERY_HEALTH_GOOD))
        assertFalse(ReportedHealth.isConcerning(BatteryManager.BATTERY_HEALTH_UNKNOWN))
        assertTrue(ReportedHealth.isConcerning(BatteryManager.BATTERY_HEALTH_OVERHEAT))
        assertTrue(ReportedHealth.isConcerning(BatteryManager.BATTERY_HEALTH_DEAD))
        assertTrue(ReportedHealth.isConcerning(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE))
        assertTrue(ReportedHealth.isConcerning(BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE))
        assertTrue(ReportedHealth.isConcerning(BatteryManager.BATTERY_HEALTH_COLD))
    }
}
