package com.lifespan.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifespan.app.domain.history.ChartSeries
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A labeled telemetry line chart: title + latest value on top, a smooth line
 * with a soft gradient fill and dashed min/max gridlines, and the time range
 * underneath.
 */
@Composable
fun TelemetryChart(
    title: String,
    series: ChartSeries,
    lineColor: Color,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 96.dp,
) {
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
            )
            series.latest?.let {
                Text(
                    valueFormatter(it.value),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = lineColor,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        ChartCanvas(
            series = series,
            lineColor = lineColor,
            gridColor = labelColor.copy(alpha = 0.25f),
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        )
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatChartTime(series.startTime), style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text(
                "${valueFormatter(series.minValue)} – ${valueFormatter(series.maxValue)}",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
            )
            Text(formatChartTime(series.endTime), style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}

@Composable
private fun ChartCanvas(
    series: ChartSeries,
    lineColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f || series.points.size < 2) return@Canvas

        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        listOf(0f, 0.5f, 1f).forEach { fy ->
            val y = h * (1f - fy)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f, pathEffect = dash)
        }

        val linePath = Path()
        val fillPath = Path()
        series.points.forEachIndexed { i, p ->
            val x = series.normalizedX(p.timestamp) * w
            val y = (1f - series.normalizedY(p.value)) * h
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        val lastX = series.normalizedX(series.points.last().timestamp) * w
        fillPath.lineTo(lastX, h)
        fillPath.close()

        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)),
                startY = 0f,
                endY = h,
            ),
        )
        drawPath(
            linePath,
            color = lineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
        )

        // Emphasize the most recent sample.
        val last = series.points.last()
        drawCircle(
            color = lineColor,
            radius = 6f,
            center = Offset(
                series.normalizedX(last.timestamp) * w,
                (1f - series.normalizedY(last.value)) * h,
            ),
        )
    }
}

private fun formatChartTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
