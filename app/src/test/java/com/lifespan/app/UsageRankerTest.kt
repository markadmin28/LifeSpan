package com.lifespan.app

import com.lifespan.app.domain.usage.AppUsage
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
}
