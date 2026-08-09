package com.lifespan.app

import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType
import com.lifespan.app.domain.session.SessionAccumulator
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionAccumulatorTest {

    private fun sample(ts: Long, level: Int, currentMa: Int, temp: Double) = BatterySnapshot(
        timestamp = ts,
        level = level,
        voltageMv = 4200,
        currentUa = currentMa * 1000,
        temperatureCelsius = temp,
        isCharging = true,
        plugType = PlugType.AC,
    )

    @Test
    fun tracksPeaksLevelsAndChargeAdded() {
        val start = 0L
        val acc = SessionAccumulator(startTime = start, startLevel = 40, plugType = "AC")
        // 3000 mA constant for 30 min => 1500 mAh added.
        acc.add(sample(start, level = 40, currentMa = 3000, temp = 30.0))
        acc.add(sample(start + 30 * 60_000L, level = 65, currentMa = 3000, temp = 41.5))

        assertEquals(40, acc.startLevel)
        assertEquals(65, acc.endLevel)
        assertEquals(41.5, acc.peakTempOrZero(), 1e-9)
        assertEquals(3000.0, acc.peakCurrentMa, 1e-9)
        assertEquals(1500.0, acc.totalMahAdded, 1e-6)
    }

    @Test
    fun singleSampleAddsNoCharge() {
        val acc = SessionAccumulator(startTime = 0L, startLevel = 50, plugType = "USB")
        acc.add(sample(0L, level = 50, currentMa = 1000, temp = 25.0))
        assertEquals(0.0, acc.totalMahAdded, 1e-9)
        assertEquals(25.0, acc.peakTempOrZero(), 1e-9)
    }
}
