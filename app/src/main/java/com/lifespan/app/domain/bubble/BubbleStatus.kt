package com.lifespan.app.domain.bubble

import com.lifespan.app.domain.alert.AlertEvaluator
import com.lifespan.app.domain.alert.AlertThresholds
import com.lifespan.app.domain.model.BatterySnapshot

/** Visual urgency of the floating charging bubble. */
enum class BubbleLevel { NORMAL, WARNING, DANGER }

/** Snapshot of what the floating charging bubble should display. */
data class BubbleStatus(
    val level: BubbleLevel,
    val percent: Int,
    val temperatureCelsius: Double,
    val powerWatts: Double,
    val plugLabel: String,
    val inHomeStretch: Boolean,
    val caption: String,
)

/**
 * Pure logic for the charging bubble. The "home stretch" is the final
 * [HOME_STRETCH_WINDOW] percent before the user's charge limit — where the
 * bubble turns amber to nudge the user to unplug soon — escalating to red once
 * an alert (overheat or charge-limit reached) actually fires.
 */
object BubbleStatusEvaluator {
    const val HOME_STRETCH_WINDOW = 10

    fun evaluate(snapshot: BatterySnapshot, thresholds: AlertThresholds): BubbleStatus {
        val alerts = AlertEvaluator.evaluate(snapshot, thresholds)
        val homeStretchStart = thresholds.chargeLimitPercent - HOME_STRETCH_WINDOW
        val approachingLimit = thresholds.chargeLimitEnabled &&
            snapshot.isCharging &&
            snapshot.level in homeStretchStart until thresholds.chargeLimitPercent

        val level = when {
            alerts.isNotEmpty() -> BubbleLevel.DANGER
            approachingLimit -> BubbleLevel.WARNING
            else -> BubbleLevel.NORMAL
        }

        val caption = when {
            alerts.isNotEmpty() -> "Unplug now"
            approachingLimit -> "Home stretch"
            else -> snapshot.plugType.label
        }

        return BubbleStatus(
            level = level,
            percent = snapshot.level,
            temperatureCelsius = snapshot.temperatureCelsius,
            powerWatts = snapshot.powerWatts,
            plugLabel = snapshot.plugType.label,
            inHomeStretch = approachingLimit || alerts.isNotEmpty(),
            caption = caption,
        )
    }
}
