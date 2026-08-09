package com.lifespan.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.db.TelemetryLogEntity
import com.lifespan.app.domain.battery.TimeEstimator
import com.lifespan.app.domain.chart.ChartMath
import com.lifespan.app.domain.chart.ChartPoint
import com.lifespan.app.ui.theme.Amber
import com.lifespan.app.ui.theme.Ok
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Maximum samples drawn per chart; longer sessions are downsampled. */
private const val MAX_CHART_POINTS = 240

data class SessionDetailUiState(
    val session: ChargeSessionEntity? = null,
    val telemetry: List<TelemetryLogEntity> = emptyList(),
)

/**
 * Drill-down for one charge session: summary stats plus level, temperature
 * and current line charts with linear-trend callouts, all rendered from the
 * session's persisted telemetry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    state: SessionDetailUiState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charge session", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(2.dp)) }

            item { SessionSummaryCard(state.session) }

            val telemetry = ChartMath.downsample(state.telemetry, MAX_CHART_POINTS)
            if (telemetry.size < 2) {
                item {
                    Text(
                        "Not enough telemetry recorded to chart this session yet. " +
                            "Charts appear once a few samples have been logged.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            } else {
                val timestamps = telemetry.map { it.timestamp }

                item {
                    TelemetryChartCard(
                        title = "Battery level",
                        unit = "%",
                        timestamps = timestamps,
                        values = telemetry.map { it.batteryLevel.toDouble() },
                        lineColor = MaterialTheme.colorScheme.primary,
                        trendLabel = { slope -> chargeRateLabel(slope) },
                    )
                }

                item {
                    TelemetryChartCard(
                        title = "Temperature",
                        unit = "°C",
                        timestamps = timestamps,
                        values = telemetry.map { it.temperatureCelsius },
                        lineColor = Amber,
                        trendLabel = { slope -> tempTrendLabel(slope) },
                    )
                }

                item {
                    TelemetryChartCard(
                        title = "Current",
                        unit = "mA",
                        timestamps = timestamps,
                        values = telemetry.map { abs(it.currentMa.toDouble()) },
                        lineColor = MaterialTheme.colorScheme.tertiary,
                        trendLabel = { null },
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SessionSummaryCard(session: ChargeSessionEntity?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (session == null) {
                Text("Session not found", style = MaterialTheme.typography.titleMedium)
                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${session.plugType} charge",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (session.endTime == null) "In progress" else "Completed",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (session.endTime == null) Ok
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                sessionTimeRange(session),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryMetric(
                    value = "${session.startLevel}% → ${session.endLevel?.let { "$it%" } ?: "…"}",
                    label = "Level",
                )
                SummaryMetric(
                    value = session.totalMahAdded
                        ?.let { String.format(Locale.US, "%.0f mAh", it) } ?: "—",
                    label = "Added",
                )
                SummaryMetric(
                    value = String.format(Locale.US, "%.1f°C", session.peakTempCelsius),
                    label = "Peak temp",
                )
                SummaryMetric(
                    value = String.format(Locale.US, "%.0f mA", session.peakCurrentMa),
                    label = "Peak current",
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun TelemetryChartCard(
    title: String,
    unit: String,
    timestamps: List<Long>,
    values: List<Double>,
    lineColor: Color,
    trendLabel: (Double?) -> String?,
) {
    val (minBound, maxBound) = ChartMath.bounds(values)
    val points = ChartMath.normalize(timestamps, values, minBound, maxBound)
    val slope = ChartMath.slopePerMinute(timestamps, values)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    formatValue(values.last(), unit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = lineColor,
                )
            }

            trendLabel(slope)?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }

            Spacer(Modifier.height(12.dp))

            LineChart(
                points = points,
                lineColor = lineColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "min ${formatValue(values.min(), unit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
                Text(
                    "max ${formatValue(values.max(), unit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun LineChart(
    points: List<ChartPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Horizontal gridlines at 0%, 50%, 100% of the value range.
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        listOf(0f, 0.5f, 1f).forEach { fraction ->
            val y = h * (1f - fraction)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dash,
            )
        }

        if (points.isEmpty()) return@Canvas

        val line = Path()
        val fill = Path()
        points.forEachIndexed { i, p ->
            val x = p.x * w
            val y = (1f - p.y) * h
            if (i == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, h)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(points.last().x * w, h)
        fill.close()

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f)),
            ),
        )
        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private fun chargeRateLabel(slopePerMinute: Double?): String? {
    slopePerMinute ?: return null
    val perHour = slopePerMinute * 60.0
    return String.format(Locale.US, "Charge rate: %+.1f%%/hr", perHour)
}

private fun tempTrendLabel(slopePerMinute: Double?): String? {
    slopePerMinute ?: return null
    val perTenMinutes = slopePerMinute * 10.0
    return String.format(Locale.US, "Trend: %+.1f°C / 10 min", perTenMinutes)
}

private fun formatValue(value: Double, unit: String): String = when (unit) {
    "%" -> String.format(Locale.US, "%.0f%%", value)
    "°C" -> String.format(Locale.US, "%.1f°C", value)
    else -> String.format(Locale.US, "%.0f %s", value, unit)
}

private fun sessionTimeRange(session: ChargeSessionEntity): String {
    val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val start = formatter.format(Date(session.startTime))
    val end = session.endTime?.let { formatter.format(Date(it)) } ?: "now"
    val durationMinutes = ((session.endTime ?: System.currentTimeMillis()) - session.startTime) / 60_000L
    return "$start → $end · ${TimeEstimator.formatMinutes(durationMinutes.coerceAtLeast(0))}"
}
