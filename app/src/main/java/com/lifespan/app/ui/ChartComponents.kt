package com.lifespan.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lifespan.app.domain.chart.ChartMath

@Composable
internal fun LineChart(
    values: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val gridColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
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

@Composable
internal fun BarChart(
    values: List<Float>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val gridColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val fractions = ChartMath.barFractions(values)
    Canvas(modifier = modifier) {
        if (fractions.isEmpty()) return@Canvas
        val baseline = size.height - 2.dp.toPx()
        drawLine(
            color = gridColor,
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = 1.dp.toPx(),
        )
        val slotWidth = size.width / fractions.size
        val barWidth = slotWidth * 0.58f
        fractions.forEachIndexed { index, fraction ->
            val height = (baseline * fraction).coerceAtLeast(0f)
            if (height == 0f) return@forEachIndexed
            drawRoundRect(
                color = barColor,
                topLeft = Offset(
                    x = index * slotWidth + (slotWidth - barWidth) / 2f,
                    y = baseline - height,
                ),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
        }
    }
}
