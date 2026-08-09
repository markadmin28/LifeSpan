package com.lifespan.app.domain.alert

/**
 * Detects a rapid temperature rise over a sliding time window. Stateful but
 * dependency-free, so it can be unit-tested directly.
 *
 * A rise is flagged when the newest temperature exceeds the lowest temperature
 * seen within [windowMillis] by at least [riseThresholdC].
 */
class TemperatureTrendMonitor(
    private val windowMillis: Long = 5 * 60_000L,
    val riseThresholdC: Double = 2.0,
) {
    private data class Sample(val timestamp: Long, val temp: Double)

    private val samples = ArrayDeque<Sample>()

    /** Add a reading; returns true if a rapid rise is detected at this sample. */
    fun add(timestamp: Long, temperatureCelsius: Double): Boolean {
        samples.addLast(Sample(timestamp, temperatureCelsius))
        while (samples.isNotEmpty() && timestamp - samples.first().timestamp > windowMillis) {
            samples.removeFirst()
        }
        val minTemp = samples.minOf { it.temp }
        return temperatureCelsius - minTemp >= riseThresholdC
    }

    fun reset() = samples.clear()
}
