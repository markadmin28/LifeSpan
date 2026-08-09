package com.lifespan.app.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifespan.app.domain.chart.ChartPoint
import com.lifespan.app.domain.chart.ChartSeries
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AXIS_GUTTER = 48.dp

/**
 * A titled card plotting one normalised [ChartSeries] as a filled line, with
 * value labels down the left edge and the time span underneath.
 */
@Composable
fun TelemetryChartCard(
    title: String,
    series: ChartSeries?,
    color: Color,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    height: Dp = 128.dp,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (trailing != null) {
                    Text(
                        trailing,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (series == null || series.isEmpty) {
                Text(
                    "No readings logged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                return@Column
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.width(AXIS_GUTTER).height(height),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    AxisLabel(formatValue(series.maxY))
                    AxisLabel(formatValue((series.maxY + series.minY) / 2.0))
                    AxisLabel(formatValue(series.minY))
                }

                Spacer(Modifier.width(8.dp))

                SeriesCanvas(
                    series = series,
                    color = color,
                    gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    modifier = Modifier
                        .weight(1f)
                        .height(height),
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(AXIS_GUTTER + 8.dp))
                AxisLabel(formatClockTime(series.startTime), modifier = Modifier.weight(1f))
                AxisLabel(
                    formatClockTime(series.endTime),
                    modifier = Modifier.weight(1f),
                    align = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun AxisLabel(
    text: String,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        textAlign = align,
        maxLines = 1,
    )
}

@Composable
private fun SeriesCanvas(
    series: ChartSeries,
    color: Color,
    gridColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.5.dp.toPx()
        val plotTop = strokeWidth / 2f
        val plotHeight = (size.height - strokeWidth).coerceAtLeast(1f)
        val baseline = plotTop + plotHeight

        fun toOffset(point: ChartPoint) =
            Offset(point.x * size.width, plotTop + (1f - point.y) * plotHeight)

        val dashes = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
        listOf(0f, 0.5f, 1f).forEach { fraction ->
            val y = plotTop + fraction * plotHeight
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashes,
            )
        }

        val offsets = series.points.map(::toOffset)
        if (offsets.size == 1) {
            drawCircle(color = color, radius = 4.dp.toPx(), center = offsets.first())
            return@Canvas
        }

        val line = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }

        val fill = Path().apply {
            addPath(line)
            lineTo(offsets.last().x, baseline)
            lineTo(offsets.first().x, baseline)
            close()
        }

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0.02f)),
            ),
        )
        drawPath(
            path = line,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private fun formatClockTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
