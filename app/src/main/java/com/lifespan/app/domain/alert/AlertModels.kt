package com.lifespan.app.domain.alert

import com.lifespan.app.domain.model.BatterySnapshot

/** User-configurable thresholds that drive the alerting logic. */
data class AlertThresholds(
    val overheatCelsius: Double = 42.0,
    val chargeLimitPercent: Int = 80,
    val overheatEnabled: Boolean = true,
    val chargeLimitEnabled: Boolean = true,
)

enum class AlertType {
    /** Battery temperature has reached or exceeded the overheat threshold. */
    OVERHEAT,

    /** Battery has reached the user-defined charge limit while charging. */
    CHARGE_LIMIT,

    /** Temperature is rising rapidly over a short window. */
    RAPID_TEMP_RISE,
}

/**
 * Pure decision logic for which alerts a given [BatterySnapshot] should raise.
 * No Android dependencies, so it is fully unit-testable.
 */
object AlertEvaluator {
    fun evaluate(snapshot: BatterySnapshot, thresholds: AlertThresholds): Set<AlertType> {
        val alerts = mutableSetOf<AlertType>()
        if (thresholds.overheatEnabled &&
            snapshot.temperatureCelsius >= thresholds.overheatCelsius
        ) {
            alerts += AlertType.OVERHEAT
        }
        if (thresholds.chargeLimitEnabled &&
            snapshot.isCharging &&
            snapshot.level >= thresholds.chargeLimitPercent
        ) {
            alerts += AlertType.CHARGE_LIMIT
        }
        return alerts
    }
}
