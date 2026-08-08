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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lifespan.app.domain.alert.AlertType
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.ui.theme.Amber
import com.lifespan.app.ui.theme.Danger
import com.lifespan.app.ui.theme.LifeSpanTheme
import com.lifespan.app.ui.theme.Ok
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onChargeLimitChange: (Int) -> Unit,
    onOverheatChange: (Double) -> Unit,
    onOverheatEnabledChange: (Boolean) -> Unit,
    onChargeLimitEnabledChange: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("LifeSpan", fontWeight = FontWeight.Bold)
                        Text(
                            "Battery · thermal · charge health",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
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

            if (state.activeAlerts.isNotEmpty()) {
                item { AlertBanner(state.activeAlerts) }
            }

            item { BatteryStatusCard(state.snapshot) }

            item {
                MonitoringControl(
                    monitoring = state.monitoring,
                    onStart = onStart,
                    onStop = onStop,
                )
            }

            item {
                ThresholdsCard(
                    chargeLimit = state.thresholds.chargeLimitPercent,
                    chargeLimitEnabled = state.thresholds.chargeLimitEnabled,
                    overheat = state.thresholds.overheatCelsius,
                    overheatEnabled = state.thresholds.overheatEnabled,
                    onChargeLimitChange = onChargeLimitChange,
                    onOverheatChange = onOverheatChange,
                    onOverheatEnabledChange = onOverheatEnabledChange,
                    onChargeLimitEnabledChange = onChargeLimitEnabledChange,
                )
            }

            item {
                Text(
                    "Charge sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            if (state.sessions.isEmpty()) {
                item {
                    Text(
                        "No charge sessions recorded yet. Start monitoring and plug in to log one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            } else {
                items(state.sessions, key = { it.id }) { session ->
                    ChargeSessionCard(session)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AlertBanner(alerts: Set<AlertType>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Danger)
            Spacer(Modifier.width(12.dp))
            Text(
                text = alerts.joinToString("  ·  ") {
                    when (it) {
                        AlertType.OVERHEAT -> "Overheat protection triggered"
                        AlertType.CHARGE_LIMIT -> "Charge limit reached"
                    }
                },
                color = Danger,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun BatteryStatusCard(snapshot: BatterySnapshot?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            if (snapshot == null) {
                Text("No reading yet", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Start monitoring to see live battery telemetry.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                return@Column
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${snapshot.level}%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (snapshot.isCharging) "Charging · ${snapshot.plugType.label}" else "On battery",
                        color = if (snapshot.isCharging) Ok else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    snapshot.technology?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Metric(
                    icon = { Icon(Icons.Filled.Thermostat, null, tint = tempTint(snapshot.temperatureCelsius)) },
                    label = "Temp",
                    value = String.format(Locale.US, "%.1f°C", snapshot.temperatureCelsius),
                )
                Metric(
                    icon = { Icon(Icons.Filled.Bolt, null, tint = Amber) },
                    label = "Power",
                    value = String.format(Locale.US, "%.2f W", snapshot.powerWatts),
                )
                Metric(
                    icon = { Icon(Icons.Filled.BatteryChargingFull, null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Current",
                    value = String.format(Locale.US, "%.0f mA", snapshot.currentMa),
                )
                Metric(
                    icon = { Icon(Icons.Filled.Bolt, null, tint = MaterialTheme.colorScheme.secondary) },
                    label = "Voltage",
                    value = String.format(Locale.US, "%.2f V", snapshot.voltageMv / 1000.0),
                )
            }
        }
    }
}

@Composable
private fun Metric(icon: @Composable () -> Unit, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun MonitoringControl(monitoring: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    if (monitoring) "Monitoring active" else "Monitoring stopped",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (monitoring) "Foreground service is running." else "Start the sticky foreground service.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            if (monitoring) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                ) {
                    Icon(Icons.Filled.Stop, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Stop")
                }
            } else {
                Button(onClick = onStart) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Start")
                }
            }
        }
    }
}

@Composable
private fun ThresholdsCard(
    chargeLimit: Int,
    chargeLimitEnabled: Boolean,
    overheat: Double,
    overheatEnabled: Boolean,
    onChargeLimitChange: (Int) -> Unit,
    onOverheatChange: (Double) -> Unit,
    onOverheatEnabledChange: (Boolean) -> Unit,
    onChargeLimitEnabledChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Charge limit: $chargeLimit%", fontWeight = FontWeight.SemiBold)
                Switch(checked = chargeLimitEnabled, onCheckedChange = onChargeLimitEnabledChange)
            }
            Slider(
                value = chargeLimit.toFloat(),
                onValueChange = { onChargeLimitChange(it.roundToInt()) },
                valueRange = 50f..100f,
                steps = 49,
                enabled = chargeLimitEnabled,
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Overheat alert: ${String.format(Locale.US, "%.0f°C", overheat)}",
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(checked = overheatEnabled, onCheckedChange = onOverheatEnabledChange)
            }
            Slider(
                value = overheat.toFloat(),
                onValueChange = { onOverheatChange(it.toDouble()) },
                valueRange = 35f..50f,
                steps = 14,
                enabled = overheatEnabled,
            )
        }
    }
}

@Composable
private fun ChargeSessionCard(session: ChargeSessionEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(session.plugType, fontWeight = FontWeight.Bold)
                Text(
                    formatTime(session.startTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
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

private fun tempTint(celsius: Double): Color = when {
    celsius >= 42.0 -> Danger
    celsius >= 38.0 -> Amber
    else -> Ok
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(millis))

@Preview
@Composable
private fun MainScreenPreview() {
    LifeSpanTheme(dynamicColor = false) {
        MainScreen(
            state = MainUiState(
                snapshot = BatterySnapshot(
                    timestamp = System.currentTimeMillis(),
                    level = 78,
                    voltageMv = 4210,
                    currentUa = 1_850_000,
                    temperatureCelsius = 39.4,
                    isCharging = true,
                    plugType = PlugType.AC,
                    technology = "Li-ion",
                ),
                monitoring = true,
                sessions = listOf(
                    ChargeSessionEntity(
                        id = 1,
                        startTime = System.currentTimeMillis() - 3_600_000,
                        endTime = System.currentTimeMillis(),
                        startLevel = 42,
                        endLevel = 80,
                        plugType = "AC",
                        peakTempCelsius = 40.1,
                        peakCurrentMa = 2100.0,
                        totalMahAdded = 1500.0,
                    ),
                ),
            ),
            onStart = {},
            onStop = {},
            onChargeLimitChange = {},
            onOverheatChange = {},
            onOverheatEnabledChange = {},
            onChargeLimitEnabledChange = {},
        )
    }
}
