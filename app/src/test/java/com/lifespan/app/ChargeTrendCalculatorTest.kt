package com.lifespan.app

import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.domain.trends.ChargeTrendCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ChargeTrendCalculatorTest {

    @Test
    fun dailyBucketsUseLocalCalendarBoundariesAndIncludeEmptyDays() {
        val zone = ZoneId.of("America/Los_Angeles")
        val now = Instant.parse("2026-03-11T20:00:00Z")
        val firstDay = LocalDate.of(2026, 3, 5)
        val sessions = listOf(
            session(1, firstDay.atStartOfDay(zone).toInstant()),
            session(2, firstDay.atStartOfDay(zone).toInstant().minusMillis(1)),
            session(3, LocalDate.of(2026, 3, 8).atStartOfDay(zone).toInstant()),
            session(4, LocalDate.of(2026, 3, 12).atStartOfDay(zone).toInstant()),
            session(
                id = 5,
                start = LocalDate.of(2026, 3, 10).atStartOfDay(zone).toInstant(),
                complete = false,
            ),
            session(6, now.plusSeconds(1)), // future time on the current day
        )

        val daily = ChargeTrendCalculator.compute(sessions, now, zone).daily

        assertEquals(7, daily.size)
        assertEquals(firstDay, daily.first().startDate)
        assertEquals(LocalDate.of(2026, 3, 11), daily.last().startDate)
        assertEquals(1, daily[0].sessionCount)
        assertEquals(1, daily[3].sessionCount)
        assertEquals(listOf(1, 0, 0, 1, 0, 0, 0), daily.map { it.sessionCount })
    }

    @Test
    fun aggregatesSessionCountGainCyclesMahAndAveragePeakTemperature() {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-08-09T12:00:00Z")
        val todayStart = LocalDate.of(2026, 8, 9).atStartOfDay(zone).toInstant()
        val sessions = listOf(
            session(
                id = 1,
                start = todayStart,
                startLevel = 20,
                endLevel = 80,
                mah = 1_800.0,
                peakTemperature = 40.0,
            ),
            session(
                id = 2,
                start = todayStart.plusSeconds(3_600),
                startLevel = 80,
                endLevel = 75,
                mah = Double.NaN,
                peakTemperature = 42.0,
            ),
        )

        val bucket = ChargeTrendCalculator.compute(sessions, now, zone).daily.last()

        assertEquals(2, bucket.sessionCount)
        assertEquals(60, bucket.chargeLevelGained)
        assertEquals(0.6, bucket.equivalentCycles, 0.0)
        assertEquals(1_800.0, bucket.totalMahAdded, 0.0)
        assertEquals(41.0, bucket.averagePeakTemperatureCelsius!!, 0.0)

        val empty = ChargeTrendCalculator.compute(emptyList(), now, zone).daily.first()
        assertEquals(0, empty.sessionCount)
        assertEquals(0.0, empty.totalMahAdded, 0.0)
        assertNull(empty.averagePeakTemperatureCelsius)
    }

    @Test
    fun weeklyBucketsStartMondayAndRespectExactFourWeekWindow() {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-08-12T12:00:00Z")
        val starts = listOf(
            Instant.parse("2026-07-20T00:00:00Z"), // oldest bucket, inclusive
            Instant.parse("2026-07-19T23:59:59.999Z"), // outside
            Instant.parse("2026-08-09T23:59:59.999Z"), // previous week
            Instant.parse("2026-08-10T00:00:00Z"), // current week, inclusive
        )

        val weekly = ChargeTrendCalculator.compute(
            sessions = starts.mapIndexed { index, instant -> session(index.toLong(), instant) },
            now = now,
            zoneId = zone,
        ).weekly

        assertEquals(
            listOf(
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 27),
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 10),
            ),
            weekly.map { it.startDate },
        )
        assertEquals(listOf(1, 0, 1, 1), weekly.map { it.sessionCount })
    }

    private fun session(
        id: Long,
        start: Instant,
        startLevel: Int = 20,
        endLevel: Int? = 80,
        mah: Double? = 1_800.0,
        peakTemperature: Double = 38.0,
        complete: Boolean = true,
    ) = ChargeSessionEntity(
        id = id,
        startTime = start.toEpochMilli(),
        endTime = if (complete) start.plusSeconds(3_600).toEpochMilli() else null,
        startLevel = startLevel,
        endLevel = endLevel,
        plugType = "AC",
        peakTempCelsius = peakTemperature,
        peakCurrentMa = 2_000.0,
        totalMahAdded = mah,
    )
}
