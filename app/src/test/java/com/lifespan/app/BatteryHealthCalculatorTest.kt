package com.lifespan.app

import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.domain.health.BatteryGuidanceStatus
import com.lifespan.app.domain.health.BatteryHealthCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryHealthCalculatorTest {

    private fun session(
        id: Long,
        time: Long,
        start: Int = 20,
        end: Int? = 80,
        mah: Double? = 1_800.0,
        complete: Boolean = true,
    ) = ChargeSessionEntity(
        id = id,
        startTime = time,
        endTime = if (complete) time + 1 else null,
        startLevel = start,
        endLevel = end,
        plugType = "AC",
        peakTempCelsius = 35.0,
        peakCurrentMa = 1000.0,
        totalMahAdded = mah,
    )

    @Test
    fun emptyWhenNoSessions() {
        val health = BatteryHealthCalculator.compute(emptyList())
        assertEquals(0.0, health.estimatedCycles, 0.0)
        assertNull(health.estimatedCapacityMah)
        assertNull(health.healthPercent)
        assertEquals(0, health.sampleCount)
        assertEquals(BatteryGuidanceStatus.COLLECTING_DATA, health.guidanceStatus)
    }

    @Test
    fun returnsCapacityHistoryInChronologicalOrderRegardlessOfInputOrder() {
        val sessions = listOf(
            session(id = 3, time = 3_000, mah = 1_680.0), // 2800 mAh
            session(id = 1, time = 1_000, mah = 1_920.0), // 3200 mAh
            session(id = 2, time = 2_000, mah = 1_800.0), // 3000 mAh
        )

        val health = BatteryHealthCalculator.compute(sessions)

        assertEquals(listOf(1L, 2L, 3L), health.capacityHistory.map { it.sessionId })
        assertEquals(listOf(3200, 3000, 2800), health.capacityHistory.map { it.estimatedCapacityMah })
        assertEquals(3000, health.estimatedCapacityMah)
        assertEquals(1.8, health.estimatedCycles, 1e-9)
    }

    @Test
    fun reportsFadeAgainstNoiseResistantObservedBaseline() {
        val capacities = listOf(3_200, 3_180, 3_160, 2_850, 2_800, 2_750)
        val sessions = capacities.mapIndexed { index, capacity ->
            session(
                id = index.toLong(),
                time = index * 1_000L,
                mah = capacity * 0.6,
            )
        }.reversed()

        val health = BatteryHealthCalculator.compute(sessions)

        // Reference rolling median = 3180; recent median = 2800.
        assertEquals(12, health.capacityFadePercent)
        assertEquals(88, health.healthPercent)
        assertEquals(2800, health.estimatedCapacityMah)
        assertEquals(BatteryGuidanceStatus.MODERATE_FADE, health.guidanceStatus)
    }

    @Test
    fun guidanceMovesFromCollectingToStableAndSignificantFade() {
        val collecting = BatteryHealthCalculator.compute(
            listOf(
                session(id = 1, time = 1, mah = 1_800.0),
                session(id = 2, time = 2, mah = 1_770.0),
            ),
        )
        assertNull(collecting.capacityFadePercent)
        assertNull(collecting.healthPercent)
        assertEquals(BatteryGuidanceStatus.COLLECTING_DATA, collecting.guidanceStatus)

        val stable = BatteryHealthCalculator.compute(
            listOf(3_000, 3_060, 2_970).mapIndexed { index, capacity ->
                session(id = index.toLong(), time = index.toLong(), mah = capacity * 0.6)
            },
        )
        assertEquals(0, stable.capacityFadePercent)
        assertEquals(BatteryGuidanceStatus.STABLE, stable.guidanceStatus)

        val significant = BatteryHealthCalculator.compute(
            listOf(3_200, 3_180, 3_160, 2_500, 2_450, 2_400).mapIndexed { index, capacity ->
                session(id = index.toLong(), time = index.toLong(), mah = capacity * 0.6)
            },
        )
        assertEquals(BatteryGuidanceStatus.SIGNIFICANT_FADE, significant.guidanceStatus)
        assertEquals(23, significant.capacityFadePercent)
    }

    @Test
    fun ignoresTinyIncompleteAndInvalidSessionsForCapacityButKeepsEquivalentCycles() {
        val sessions = listOf(
            session(id = 1, time = 1, start = 79, end = 80, mah = 40.0),
            session(id = 2, time = 2, start = 20, end = 80, mah = 1_800.0, complete = false),
            session(id = 3, time = 3, start = 20, end = null, mah = null),
            session(id = 4, time = 4, start = 20, end = 80, mah = Double.NaN),
        )

        val health = BatteryHealthCalculator.compute(sessions)

        assertNull(health.estimatedCapacityMah)
        assertEquals(0, health.sampleCount)
        // Positive level deltas remain equivalent-cycle contributions, including
        // an in-progress session, preserving the existing calculation.
        assertEquals(1.21, health.estimatedCycles, 1e-9)
    }

    @Test
    fun clampsRecoveryAndRejectsSingleCapacityOutlierAsNoise() {
        val capacities = listOf(3_000, 3_020, 20_000, 3_030, 3_040, 3_060)
        val health = BatteryHealthCalculator.compute(
            capacities.mapIndexed { index, capacity ->
                session(id = index.toLong(), time = index.toLong(), mah = capacity * 0.6)
            },
        )

        // The isolated high estimate never becomes a rolling-median baseline.
        assertEquals(0, health.capacityFadePercent)
        assertEquals(100, health.healthPercent)
        assertEquals(BatteryGuidanceStatus.STABLE, health.guidanceStatus)
        assertEquals(6, health.sampleCount)
    }
}
