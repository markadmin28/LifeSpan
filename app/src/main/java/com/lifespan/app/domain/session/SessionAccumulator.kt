package com.lifespan.app.domain.session

import com.lifespan.app.domain.battery.BatteryCalculator
import com.lifespan.app.domain.model.BatterySnapshot
import kotlin.math.abs
import kotlin.math.max

/**
 * Accumulates running statistics for a single charge session as telemetry
 * samples arrive. Pure and deterministic so the charge-profile math can be
 * unit-tested without a database or the Android framework.
 *
 * `totalMahAdded` integrates current over time between successive samples
 * (a simple left-Riemann sum): `Σ |I(mA)| * Δt(h)`.
 */
class SessionAccumulator(
    val startTime: Long,
    val startLevel: Int,
    val plugType: String,
) {
    var endTime: Long? = null
        private set
    var endLevel: Int? = null
        private set
    var peakTempCelsius: Double = Double.NEGATIVE_INFINITY
        private set
    var peakCurrentMa: Double = 0.0
        private set
    var totalMahAdded: Double = 0.0
        private set

    private var lastTimestamp: Long = startTime
    private var lastCurrentMa: Double = 0.0
    private var sampleCount: Int = 0

    /** Fold a new telemetry [snapshot] into the running totals. */
    fun add(snapshot: BatterySnapshot) {
        val currentMa = snapshot.currentMa
        if (sampleCount > 0) {
            val interval = snapshot.timestamp - lastTimestamp
            // Trapezoidal average of the two current samples for a smoother estimate.
            val avgCurrentMa = (abs(lastCurrentMa) + abs(currentMa)) / 2.0
            totalMahAdded += BatteryCalculator.mahOverInterval(avgCurrentMa, interval)
        }
        peakTempCelsius =
            if (sampleCount == 0) snapshot.temperatureCelsius
            else max(peakTempCelsius, snapshot.temperatureCelsius)
        peakCurrentMa = max(peakCurrentMa, abs(currentMa))

        lastTimestamp = snapshot.timestamp
        lastCurrentMa = currentMa
        endTime = snapshot.timestamp
        endLevel = snapshot.level
        sampleCount++
    }

    fun peakTempOrZero(): Double =
        if (peakTempCelsius == Double.NEGATIVE_INFINITY) 0.0 else peakTempCelsius
}
