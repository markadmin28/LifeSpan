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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.db.TelemetryLogEntity
import com.lifespan.app.domain.chart.ChartMath
import com.lifespan.app.ui.theme.Amber
import com.lifespan.app.ui.theme.Danger
import com.lifespan.app.ui.theme.Ok
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    session: ChargeSessionEntity?,
    logs: List<TelemetryLogEntity>,
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

            if (session != null) {
                item { SummaryCard(session, logs.size) }
            }

            if (logs.size < 2) {
                item {
                    Text(
                        "Not enough telemetry captured for this session to chart yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            } else {
                item {
                    ChartCard(
                        "Battery level (%)",
                        logs.map { it.batteryLevel.toFloat() },
                        MaterialTheme.colorScheme.primary,
                    )
                }
                item {
                    ChartCard(
                        "Temperature (°C)",
                        logs.map { it.temperatureCelsius.toFloat() },
                        Danger,
                    )
                }
                item {
                    ChartCard(
                        "Current (mA)",
                        logs.map { it.currentMa.toFloat() },
                        Amber,
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SummaryCard(session: ChargeSessionEntity, sampleCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(session.plugType, fontWeight = FontWeight.Bold)
                Text(
                    formatRange(session.startTime, session.endTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.height(8.dp))
            val end = session.endLevel?.let { "$it%" } ?: "in progress"
            Text("Level: ${session.startLevel}% → $end", color = Ok, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Peak ${String.format(Locale.US, "%.1f°C", session.peakTempCelsius)} · " +
                    "${String.format(Locale.US, "%.0f mA", session.peakCurrentMa)} · " +
                    (session.totalMahAdded?.let { String.format(Locale.US, "%.0f mAh added", it) } ?: "—") +
                    " · $sampleCount samples",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun ChartCard(title: String, values: List<Float>, lineColor: Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    "${values.minOrNull()?.toInt() ?: 0} – ${values.maxOrNull()?.toInt() ?: 0}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.height(10.dp))
            LineChart(
                values = values,
                lineColor = lineColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
        }
    }
}

@Composable
private fun LineChart(values: List<Float>, lineColor: Color, modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val pad = 6.dp.toPx()
        drawLine(
            color = gridColor,
            start = Offset(pad, size.height - pad),
            end = Offset(size.width - pad, size.height - pad),
            strokeWidth = 1.dp.toPx(),
        )
        val points = ChartMath.points(values, size.width, size.height, pad)
        val path = Path()
        points.forEachIndexed { index, (x, y) ->
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 3.dp.toPx()))
    }
}

private fun formatRange(start: Long, end: Long?): String {
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val startStr = fmt.format(Date(start))
    return if (end != null) {
        "$startStr – ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(end))}"
    } else {
        startStr
    }
}
