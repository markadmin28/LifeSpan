package com.lifespan.app.domain.trends

import com.lifespan.app.data.db.ChargeSessionEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class ChargeTrendRange {
    DAILY,
    WEEKLY,
}

data class ChargeTrendBucket(
    val startDate: LocalDate,
    val endDateExclusive: LocalDate,
    val sessionCount: Int,
    val chargeLevelGained: Int,
    val equivalentCycles: Double,
    val totalMahAdded: Double,
    val averagePeakTemperatureCelsius: Double?,
)

data class ChargeTrends(
    val zoneId: ZoneId,
    val daily: List<ChargeTrendBucket>,
    val weekly: List<ChargeTrendBucket>,
) {
    companion object {
        val EMPTY = ChargeTrends(
            zoneId = ZoneId.of("UTC"),
            daily = emptyList(),
            weekly = emptyList(),
        )
    }
}

/**
 * Deterministic calendar aggregation derived only from persisted charge sessions.
 *
 * A completed session belongs to the local calendar period in which it started.
 * The entity does not contain interval-level energy data, so sessions crossing a
 * boundary cannot be split accurately between periods.
 */
object ChargeTrendCalculator {
    const val DAILY_BUCKET_COUNT = 7
    const val WEEKLY_BUCKET_COUNT = 4

    fun compute(
        sessions: List<ChargeSessionEntity>,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ChargeTrends {
        val today = now.atZone(zoneId).toLocalDate()
        val dailyStarts = (DAILY_BUCKET_COUNT - 1 downTo 0).map { today.minusDays(it.toLong()) }
        val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeklyStarts = (WEEKLY_BUCKET_COUNT - 1 downTo 0).map { thisWeek.minusWeeks(it.toLong()) }

        val nowMillis = now.toEpochMilli()
        val completed = sessions.filter { it.endTime != null && it.startTime <= nowMillis }
        return ChargeTrends(
            zoneId = zoneId,
            daily = aggregate(
                sessions = completed,
                starts = dailyStarts,
                endForStart = { it.plusDays(1) },
                zoneId = zoneId,
            ),
            weekly = aggregate(
                sessions = completed,
                starts = weeklyStarts,
                endForStart = { it.plusWeeks(1) },
                zoneId = zoneId,
            ),
        )
    }

    private fun aggregate(
        sessions: List<ChargeSessionEntity>,
        starts: List<LocalDate>,
        endForStart: (LocalDate) -> LocalDate,
        zoneId: ZoneId,
    ): List<ChargeTrendBucket> = starts.map { start ->
        val end = endForStart(start)
        val startMillis = start.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = end.atStartOfDay(zoneId).toInstant().toEpochMilli()
        summarize(
            sessions = sessions.filter { it.startTime in startMillis until endMillis },
            start = start,
            end = end,
        )
    }

    private fun summarize(
        sessions: List<ChargeSessionEntity>,
        start: LocalDate,
        end: LocalDate,
    ): ChargeTrendBucket {
        val levelGained = sessions.sumOf { session ->
            val endLevel = session.endLevel
            if (endLevel != null) (endLevel - session.startLevel).coerceAtLeast(0) else 0
        }
        val mahAdded = sessions.mapNotNull { session ->
            session.totalMahAdded?.takeIf { it.isFinite() && it > 0.0 }
        }.sum()
        val temperatures = sessions.mapNotNull { session ->
            session.peakTempCelsius.takeIf { it.isFinite() }
        }
        return ChargeTrendBucket(
            startDate = start,
            endDateExclusive = end,
            sessionCount = sessions.size,
            chargeLevelGained = levelGained,
            equivalentCycles = levelGained / 100.0,
            totalMahAdded = mahAdded,
            averagePeakTemperatureCelsius = temperatures.takeIf { it.isNotEmpty() }?.average(),
        )
    }
}
