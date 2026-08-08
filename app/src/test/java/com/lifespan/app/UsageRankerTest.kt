package com.lifespan.app

import com.lifespan.app.domain.usage.AppUsage
import com.lifespan.app.domain.usage.ConsumptionLevel
import com.lifespan.app.domain.usage.UsageRanker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageRankerTest {

    private fun app(pkg: String, minutes: Long) =
        AppUsage(pkg, pkg, minutes * 60_000L)

    @Test
    fun rankSortsDescendingAndDropsIdle() {
        val ranked = UsageRanker.rank(
            listOf(app("a", 10), app("b", 0), app("c", 45), app("d", 20)),
        )
        assertEquals(listOf("c", "d", "a"), ranked.map { it.packageName })
    }

    @Test
    fun rankRespectsLimit() {
        val items = (1..30).map { app("p$it", it.toLong()) }
        assertEquals(5, UsageRanker.rank(items, limit = 5).size)
    }

    @Test
    fun isHighUsesThreshold() {
        assertTrue(UsageRanker.isHigh(app("x", 30)))
        assertFalse(UsageRanker.isHigh(app("y", 29)))
    }

    @Test
    fun highCountCountsAboveThreshold() {
        val items = listOf(app("a", 45), app("b", 10), app("c", 60))
        assertEquals(2, UsageRanker.highCount(items))
    }

    @Test
    fun batteryShareSumsToAboutHundred() {
        val ranked = UsageRanker.rank(listOf(app("a", 30), app("b", 10), app("c", 10)))
        val total = ranked.sumOf { it.batteryPercent }
        assertEquals(100.0, total, 0.01)
        // a is 30 of 50 minutes -> 60%
        assertEquals(60.0, ranked.first { it.packageName == "a" }.batteryPercent, 0.01)
    }

    @Test
    fun consumptionTiersByShare() {
        val ranked = UsageRanker.rank(listOf(app("heavy", 60), app("mod", 20), app("light", 8)))
        assertEquals(ConsumptionLevel.HEAVY, UsageRanker.consumption(ranked.first { it.packageName == "heavy" }))
        assertEquals(ConsumptionLevel.MODERATE, UsageRanker.consumption(ranked.first { it.packageName == "mod" }))
        assertEquals(ConsumptionLevel.LIGHT, UsageRanker.consumption(ranked.first { it.packageName == "light" }))
    }
}
