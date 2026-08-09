package com.lifespan.app

import android.os.BatteryManager
import com.lifespan.app.domain.health.BatteryHealthEstimator
import com.lifespan.app.domain.health.CapacityObservation
import com.lifespan.app.domain.health.EstimateConfidence
import com.lifespan.app.domain.health.ReportedHealth
import com.lifespan.app.domain.health.WearGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryHealthEstimatorTest {

    @Test
    fun `implied capacity extrapolates a partial charge to a full one`() {
        // 1900 mAh moved the level 50% => a full charge would take 3800 mAh.
        val implied = BatteryHealthEstimator.impliedFullCapacityMah(
            CapacityObservation(levelDeltaPercent = 50, mahAdded = 1900.0),
        )
        assertNotNull(implied)
        assertEquals(3800.0, implied!!, 0.01)
    }

    @Test
    fun `short charge bursts are rejected as too noisy`() {
        val implied = BatteryHealthEstimator.impliedFullCapacityMah(
            CapacityObservation(levelDeltaPercent = 5, mahAdded = 190.0),
        )
        assertNull(implied)
    }

    @Test
    fun `sessions without measured charge are rejected`() {
        assertNull(
            BatteryHealthEstimator.impliedFullCapacityMah(
                CapacityObservation(levelDeltaPercent = 40, mahAdded = 0.0),
            ),
        )
    }

    @Test
    fun `state of health compares measured capacity against the design rating`() {
        val estimate = BatteryHealthEstimator.estimate(
            observations = listOf(
                CapacityObservation(levelDeltaPercent = 50, mahAdded = 2000.0),
                CapacityObservation(levelDeltaPercent = 40, mahAdded = 1600.0),
                CapacityObservation(levelDeltaPercent = 25, mahAdded = 1000.0),
            ),
            designCapacityMah = 5000.0,
        )

        assertEquals(4000.0, estimate.measuredCapacityMah!!, 0.01)
        assertEquals(80, estimate.statePercent)
        assertEquals(WearGrade.GOOD, estimate.grade)
        assertEquals(3, estimate.sampleCount)
        assertEquals(1000.0, estimate.lostCapacityMah!!, 0.01)
    }

    @Test
    fun `median ignores a single wildly wrong session`() {
        val estimate = BatteryHealthEstimator.estimate(
            observations = listOf(
                CapacityObservation(levelDeltaPercent = 50, mahAdded = 2000.0),
                CapacityObservation(levelDeltaPercent = 50, mahAdded = 2050.0),
                CapacityObservation(levelDeltaPercent = 50, mahAdded = 9000.0),
            ),
            designCapacityMah = 4100.0,
        )

        assertEquals(4100.0, estimate.measuredCapacityMah!!, 0.01)
        assertEquals(100, estimate.statePercent)
    }

    @Test
    fun `even sample counts average the two middle capacities`() {
        val estimate = BatteryHealthEstimator.estimate(
            observations = listOf(
                CapacityObservation(levelDeltaPercent = 50, mahAdded = 1900.0),
                CapacityObservation(levelDeltaPercent = 50, mahAdded = 2100.0),
            ),
        )
        assertEquals(4000.0, estimate.measuredCapacityMah!!, 0.01)
    }

    @Test
    fun `qualifying sessions are counted and unusable ones skipped`() {
        val estimate = BatteryHealthEstimator.estimate(
            observations = listOf(
                CapacityObservation(levelDeltaPercent = 50, mahAdded = 2000.0),
                CapacityObservation(levelDeltaPercent = 3, mahAdded = 120.0),
                CapacityObservation(levelDeltaPercent = 30, mahAdded = 1200.0),
            ),
            designCapacityMah = 4000.0,
        )
        assertEquals(2, estimate.sampleCount)
    }

    @Test
    fun `no usable sessions yields an empty estimate`() {
        val estimate = BatteryHealthEstimator.estimate(
            observations = listOf(CapacityObservation(levelDeltaPercent = 4, mahAdded = 150.0)),
            designCapacityMah = 4000.0,
        )

        assertFalse(estimate.hasEstimate)
        assertNull(estimate.statePercent)
        assertNull(estimate.grade)
        assertNull(estimate.lostCapacityMah)
        assertEquals(EstimateConfidence.NONE, estimate.confidence)
    }

    @Test
    fun `capacity is still measured when the design rating is unknown`() {
        val estimate = BatteryHealthEstimator.estimate(
            observations = listOf(CapacityObservation(levelDeltaPercent = 50, mahAdded = 2000.0)),
            designCapacityMah = null,
        )

        assertTrue(estimate.hasEstimate)
        assertEquals(4000.0, estimate.measuredCapacityMah!!, 0.01)
        assertNull(estimate.statePercent)
        assertNull(estimate.designCapacityMah)
    }

    @Test
    fun `a nonsense design rating is treated as unknown`() {
        val estimate = BatteryHealthEstimator.estimate(
            observations = listOf(CapacityObservation(levelDeltaPercent = 50, mahAdded = 2000.0)),
            designCapacityMah = 0.0,
        )
        assertNull(estimate.designCapacityMah)
        assertNull(estimate.statePercent)
    }

    @Test
    fun `implausible readings are clamped into a believable band`() {
        val estimate = BatteryHealthEstimator.estimate(
            observations = listOf(CapacityObservation(levelDeltaPercent = 50, mahAdded = 9000.0)),
            designCapacityMah = 1000.0,
        )
        assertEquals(150, estimate.statePercent)
    }

    @Test
    fun `wear grades follow the state of health`() {
        assertEquals(WearGrade.EXCELLENT, BatteryHealthEstimator.gradeFor(96))
        assertEquals(WearGrade.EXCELLENT, BatteryHealthEstimator.gradeFor(90))
        assertEquals(WearGrade.GOOD, BatteryHealthEstimator.gradeFor(89))
        assertEquals(WearGrade.GOOD, BatteryHealthEstimator.gradeFor(80))
        assertEquals(WearGrade.FAIR, BatteryHealthEstimator.gradeFor(79))
        assertEquals(WearGrade.FAIR, BatteryHealthEstimator.gradeFor(65))
        assertEquals(WearGrade.WORN, BatteryHealthEstimator.gradeFor(64))
    }

    @Test
    fun `confidence grows with the number of qualifying sessions`() {
        assertEquals(EstimateConfidence.NONE, BatteryHealthEstimator.confidenceFor(0))
        assertEquals(EstimateConfidence.LOW, BatteryHealthEstimator.confidenceFor(1))
        assertEquals(EstimateConfidence.LOW, BatteryHealthEstimator.confidenceFor(2))
        assertEquals(EstimateConfidence.MEDIUM, BatteryHealthEstimator.confidenceFor(3))
        assertEquals(EstimateConfidence.MEDIUM, BatteryHealthEstimator.confidenceFor(5))
        assertEquals(EstimateConfidence.HIGH, BatteryHealthEstimator.confidenceFor(6))
    }

    @Test
    fun `platform health extras map to labelled conditions`() {
        assertEquals(ReportedHealth.GOOD, ReportedHealth.fromExtra(BatteryManager.BATTERY_HEALTH_GOOD))
        assertEquals(ReportedHealth.OVERHEAT, ReportedHealth.fromExtra(BatteryManager.BATTERY_HEALTH_OVERHEAT))
        assertEquals(ReportedHealth.DEAD, ReportedHealth.fromExtra(BatteryManager.BATTERY_HEALTH_DEAD))
        assertEquals(
            ReportedHealth.OVER_VOLTAGE,
            ReportedHealth.fromExtra(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE),
        )
        assertEquals(
            ReportedHealth.FAILURE,
            ReportedHealth.fromExtra(BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE),
        )
        assertEquals(ReportedHealth.COLD, ReportedHealth.fromExtra(BatteryManager.BATTERY_HEALTH_COLD))
        assertEquals(ReportedHealth.UNKNOWN, ReportedHealth.fromExtra(BatteryManager.BATTERY_HEALTH_UNKNOWN))
        assertEquals(ReportedHealth.UNKNOWN, ReportedHealth.fromExtra(0))
    }

    @Test
    fun `only abnormal conditions count as problems`() {
        assertFalse(ReportedHealth.GOOD.isProblem)
        assertFalse(ReportedHealth.UNKNOWN.isProblem)
        assertTrue(ReportedHealth.OVERHEAT.isProblem)
        assertTrue(ReportedHealth.DEAD.isProblem)
        assertTrue(ReportedHealth.COLD.isProblem)
    }

    @Test
    fun `reported health is carried through to the estimate`() {
        val estimate = BatteryHealthEstimator.estimate(
            observations = emptyList(),
            designCapacityMah = 4000.0,
            reportedHealth = ReportedHealth.OVERHEAT,
        )
        assertEquals(ReportedHealth.OVERHEAT, estimate.reportedHealth)
    }
}
