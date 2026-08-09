package com.lifespan.app.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.db.TelemetryLogEntity
import com.lifespan.app.domain.chart.ChartScaler
import com.lifespan.app.domain.session.SessionStats
import com.lifespan.app.domain.session.SessionStatsCalculator
import com.lifespan.app.ui.chart.TelemetryChartCard
import com.lifespan.app.ui.theme.Amber
import com.lifespan.app.ui.theme.Danger
import com.lifespan.app.ui.theme.LifeSpanTheme
import com.lifespan.app.ui.theme.Ok
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(state: SessionDetailUiState, onBack: () -> Unit) {
    val session = state.session

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Charge session", fontWeight = FontWeight.Bold)
                        if (session != null) {
                            Text(
                                formatSessionDate(session.startTime),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                },
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
        if (session == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            ) {
                Text(
                    "This charge session is no longer available.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            return@Scaffold
        }

        val telemetry = state.telemetry
        val levelSeries = remember(telemetry) {
            ChartScaler.series(
                telemetry.map { it.timestamp to it.batteryLevel.toDouble() },
                minYOverride = 0.0,
                maxYOverride = 100.0,
            )
        }
        val temperatureSeries = remember(telemetry) {
            ChartScaler.series(
                telemetry.map { it.timestamp to it.temperatureCelsius },
                padY = 1.0,
            )
        }
        val currentSeries = remember(telemetry) {
            ChartScaler.series(
                telemetry.map { it.timestamp to it.currentMa.toDouble() },
                padY = 50.0,
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(2.dp)) }

            item { SessionSummaryCard(session = session, stats = state.stats) }

            item {
                TelemetryChartCard(
                    title = "Battery level",
                    series = levelSeries,
                    color = MaterialTheme.colorScheme.primary,
                    formatValue = { String.format(Locale.US, "%.0f%%", it) },
                    trailing = telemetry.lastOrNull()?.let { "${it.batteryLevel}%" },
                )
            }

            item {
                TelemetryChartCard(
                    title = "Temperature",
                    series = temperatureSeries,
                    color = temperatureTint(session.peakTempCelsius),
                    formatValue = { String.format(Locale.US, "%.1f°", it) },
                    trailing = String.format(Locale.US, "peak %.1f°C", session.peakTempCelsius),
                )
            }

            item {
                TelemetryChartCard(
                    title = "Charge current",
                    series = currentSeries,
                    color = Ok,
                    formatValue = { String.format(Locale.US, "%.0f", it) },
                    trailing = String.format(Locale.US, "peak %.0f mA", session.peakCurrentMa),
                )
            }

            item {
                Text(
                    if (telemetry.isEmpty()) {
                        "No per-sample readings were logged for this session. Keep monitoring " +
                            "running while charging to capture the full curve."
                    } else {
                        "${telemetry.size} readings logged during this session. Full-charge " +
                            "capacity is inferred from the charge delivered across the level " +
                            "change, so it is most accurate over a long charge."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SessionSummaryCard(session: ChargeSessionEntity, stats: SessionStats?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${session.startLevel}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "  →  ",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Text(
                    session.endLevel?.let { "$it%" } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Pill(label = session.plugType, color = MaterialTheme.colorScheme.primary)
                    if (stats?.inProgress == true) {
                        Spacer(Modifier.height(4.dp))
                        Pill(label = "IN PROGRESS", color = Ok)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            StatRow(
                left = "Duration" to (stats?.let { SessionStatsCalculator.formatDuration(it.durationMillis) } ?: "—"),
                right = "Charge rate" to (
                    stats?.ratePercentPerHour
                        ?.let { String.format(Locale.US, "%+.1f %%/h", it) } ?: "—"
                    ),
            )
            Spacer(Modifier.height(12.dp))
            StatRow(
                left = "Charge added" to (
                    session.totalMahAdded
                        ?.let { String.format(Locale.US, "%.0f mAh", it) } ?: "—"
                    ),
                right = "Average current" to (
                    stats?.averageCurrentMa
                        ?.let { String.format(Locale.US, "%.0f mA", it) } ?: "—"
                    ),
            )
            Spacer(Modifier.height(12.dp))
            StatRow(
                left = "Peak temperature" to String.format(Locale.US, "%.1f°C", session.peakTempCelsius),
                right = "Peak current" to String.format(Locale.US, "%.0f mA", session.peakCurrentMa),
            )

            stats?.impliedFullCapacityMah?.let { capacity ->
                Spacer(Modifier.height(12.dp))
                StatRow(
                    left = "Implied full capacity" to String.format(Locale.US, "%.0f mAh", capacity),
                    right = null,
                )
            }
        }
    }
}

@Composable
private fun StatRow(left: Pair<String, String>, right: Pair<String, String>?) {
    Row(modifier = Modifier.fillMaxWidth()) {
        StatCell(label = left.first, value = left.second, modifier = Modifier.weight(1f))
        if (right != null) {
            StatCell(label = right.first, value = right.second, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Pill(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.18f), shape = CircleShape) {
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

private fun temperatureTint(celsius: Double): Color = when {
    celsius >= 42.0 -> Danger
    celsius >= 38.0 -> Amber
    else -> Ok
}

private fun formatSessionDate(millis: Long): String =
    SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault()).format(Date(millis))

@Preview
@Composable
private fun SessionDetailPreview() {
    val start = System.currentTimeMillis() - 5_400_000
    val session = ChargeSessionEntity(
        id = 1,
        startTime = start,
        endTime = start + 5_400_000,
        startLevel = 38,
        endLevel = 82,
        plugType = "AC",
        peakTempCelsius = 39.8,
        peakCurrentMa = 2150.0,
        totalMahAdded = 1800.0,
    )
    val telemetry = List(24) { index ->
        TelemetryLogEntity(
            id = index.toLong(),
            sessionId = 1,
            timestamp = start + index * 225_000L,
            batteryLevel = 38 + index * 2,
            voltageMv = 4050 + index * 6,
            currentMa = 2100 - index * 40,
            temperatureCelsius = 31.0 + index * 0.36,
            isCharging = true,
        )
    }

    LifeSpanTheme {
        SessionDetailScreen(
            state = SessionDetailUiState(
                session = session,
                telemetry = telemetry,
                stats = SessionStatsCalculator.compute(
                    startTime = session.startTime,
                    endTime = session.endTime,
                    startLevel = session.startLevel,
                    endLevel = session.endLevel,
                    totalMahAdded = session.totalMahAdded,
                    nowMillis = System.currentTimeMillis(),
                ),
            ),
            onBack = {},
        )
    }
}
