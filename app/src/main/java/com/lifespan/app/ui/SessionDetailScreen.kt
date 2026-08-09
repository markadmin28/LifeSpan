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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.db.TelemetryLogEntity
import com.lifespan.app.domain.chart.ChartMath
import com.lifespan.app.ui.theme.Amber
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
                title = {
                    Column {
                        Text("Charge session", fontWeight = FontWeight.Bold)
                        session?.let {
                            Text(
                                formatSessionTime(it.startTime),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))

            if (session == null) {
                Text(
                    "Session not found.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                SessionSummaryCard(session)

                if (logs.size < 2) {
                    Text(
                        "Not enough samples yet for charts. Keep monitoring while charging.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                } else {
                    ChartCard(
                        title = "Battery level",
                        subtitle = "Percent over the session",
                        values = logs.map { it.batteryLevel.toFloat() },
                        lineColor = MaterialTheme.colorScheme.primary,
                        valueFormatter = { "${it.toInt()}%" },
                    )
                    ChartCard(
                        title = "Temperature",
                        subtitle = "°C over the session",
                        values = logs.map { it.temperatureCelsius.toFloat() },
                        lineColor = Amber,
                        valueFormatter = { String.format(Locale.US, "%.1f°C", it) },
                    )
                    ChartCard(
                        title = "Current",
                        subtitle = "mA over the session",
                        values = logs.map { kotlin.math.abs(it.currentMa).toFloat() },
                        lineColor = Ok,
                        valueFormatter = { String.format(Locale.US, "%.0f mA", it) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SessionSummaryCard(session: ChargeSessionEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(session.plugType, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            val end = session.endLevel?.let { "$it%" } ?: "in progress"
            Text("Level: ${session.startLevel}% → $end")
            Text(
                "Peak ${String.format(Locale.US, "%.1f°C", session.peakTempCelsius)} · " +
                    "${String.format(Locale.US, "%.0f mA", session.peakCurrentMa)} · " +
                    (session.totalMahAdded?.let { String.format(Locale.US, "%.0f mAh added", it) } ?: "—"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    values: List<Float>,
    lineColor: Color,
    valueFormatter: (Float) -> String,
) {
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: 0f
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Text(
                    "${valueFormatter(min)} – ${valueFormatter(max)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                val pts = ChartMath.points(values, size.width, size.height, padding = 8f)
                if (pts.size < 2) return@Canvas
                val path = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 1 until pts.size) {
                        lineTo(pts[i].x, pts[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

private fun formatSessionTime(millis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(millis))
