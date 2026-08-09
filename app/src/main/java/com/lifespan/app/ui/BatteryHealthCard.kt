package com.lifespan.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifespan.app.domain.health.BatteryHealthCalculator
import com.lifespan.app.domain.health.CapacityVerdict
import com.lifespan.app.domain.health.HealthStatus
import com.lifespan.app.ui.theme.Amber
import com.lifespan.app.ui.theme.Danger
import com.lifespan.app.ui.theme.Ok
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Dashboard card summarising battery wear: OS-reported health, estimated vs
 * design capacity, equivalent full charge cycles, and thermal stress across
 * recorded sessions.
 */
@Composable
fun BatteryHealthCard(health: HealthUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Battery health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                HealthPill(health.status)
            }

            Spacer(Modifier.height(12.dp))

            CapacitySection(health)

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HealthMetric(
                    value = String.format(Locale.US, "%.1f", health.cycles),
                    label = "Full cycles",
                )
                HealthMetric(
                    value = "${health.stats.sessionCount}",
                    label = "Sessions",
                )
                HealthMetric(
                    value = "${health.stats.hotSessionCount}",
                    label = "Hot sessions",
                    tint = if (health.stats.hotSessionCount > 0) Amber else null,
                )
            }

            health.stats.avgPeakTempCelsius?.let { avg ->
                Spacer(Modifier.height(10.dp))
                val max = health.stats.maxPeakTempCelsius ?: avg
                Text(
                    String.format(
                        Locale.US,
                        "Peak temperature: avg %.1f°C · max %.1f°C (hot ≥ %.0f°C)",
                        avg,
                        max,
                        BatteryHealthCalculator.HOT_SESSION_CELSIUS,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun CapacitySection(health: HealthUiState) {
    val estimated = health.estimatedCapacityMah
    val design = health.designCapacityMah
    val percent = health.capacityPercent

    if (estimated == null) {
        Text(
            "Capacity estimate appears once your device reports its charge counter.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            buildString {
                append("≈${estimated.roundToInt()} mAh")
                design?.let { append(" of ${it.roundToInt()} mAh design") }
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        percent?.let {
            Text(
                "$it%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = verdictColor(BatteryHealthCalculator.capacityVerdict(it)),
            )
        }
    }

    percent?.let {
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (it / 100f).coerceIn(0f, 1f) },
            color = verdictColor(BatteryHealthCalculator.capacityVerdict(it)),
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
        )
    }
}

@Composable
private fun HealthMetric(value: String, label: String, tint: Color? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tint ?: MaterialTheme.colorScheme.onSurface,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun HealthPill(status: HealthStatus) {
    val color = when (status) {
        HealthStatus.GOOD -> Ok
        HealthStatus.OVERHEAT, HealthStatus.DEAD,
        HealthStatus.OVER_VOLTAGE, HealthStatus.FAILURE,
        -> Danger
        HealthStatus.COLD -> MaterialTheme.colorScheme.tertiary
        HealthStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    Surface(color = color.copy(alpha = 0.18f), shape = CircleShape) {
        Text(
            status.label.uppercase(Locale.US),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

private fun verdictColor(verdict: CapacityVerdict): Color = when (verdict) {
    CapacityVerdict.HEALTHY -> Ok
    CapacityVerdict.WORN -> Amber
    CapacityVerdict.DEGRADED -> Danger
    CapacityVerdict.UNKNOWN -> Amber
}
