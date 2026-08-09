package com.lifespan.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifespan.app.domain.usage.AppUsage
import com.lifespan.app.domain.usage.ConsumptionLevel
import com.lifespan.app.domain.usage.UsageRanker
import com.lifespan.app.ui.theme.Amber
import com.lifespan.app.ui.theme.Danger
import com.lifespan.app.ui.theme.Ok
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighUsageScreen(
    state: UsageUiState,
    onBack: () -> Unit,
    onForceStop: (AppUsage) -> Unit,
    onRefresh: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("High battery usage", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Estimated battery share by app, based on foreground (screen) time over the " +
                        "last 24 hours — the closest signal an app can read without privileged access. " +
                        "\"Force stop\" makes a best-effort background kill and opens the app's system " +
                        "page, where you can fully stop it so it no longer drains battery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            if (state.loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.apps.isEmpty()) {
                item {
                    Text(
                        "No app usage recorded in the last 24 hours.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            } else {
                items(state.apps, key = { it.packageName }) { app ->
                    AppUsageRow(app = app, onForceStop = { onForceStop(app) })
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AppUsageRow(app: AppUsage, onForceStop: () -> Unit) {
    val icon = rememberAppIcon(app.packageName)
    val level = UsageRanker.consumption(app)
    val accent = consumptionColor(level)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (icon != null) {
                        Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(44.dp))
                    } else {
                        Icon(
                            Icons.Filled.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(app.label, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(label = consumptionLabel(level), color = accent)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            activityStatus(app),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    "${app.batteryPercent.roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (app.batteryPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                color = accent,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    usageLabel(app.foregroundMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                OutlinedButton(onClick = onForceStop) {
                    Text("Force stop")
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.18f), shape = CircleShape) {
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private fun consumptionColor(level: ConsumptionLevel): Color = when (level) {
    ConsumptionLevel.HEAVY -> Danger
    ConsumptionLevel.MODERATE -> Amber
    ConsumptionLevel.LIGHT -> Ok
}

private fun consumptionLabel(level: ConsumptionLevel): String = when (level) {
    ConsumptionLevel.HEAVY -> "HEAVY"
    ConsumptionLevel.MODERATE -> "MODERATE"
    ConsumptionLevel.LIGHT -> "LIGHT"
}

private fun activityStatus(app: AppUsage): String {
    if (app.lastUsedMillis <= 0L) return "background"
    val ageMinutes = ((System.currentTimeMillis() - app.lastUsedMillis) / 60_000L).coerceAtLeast(0)
    return when {
        ageMinutes < 5 -> "active now"
        ageMinutes < 60 -> "active ${ageMinutes}m ago"
        else -> "active ${ageMinutes / 60}h ago"
    }
}

private fun usageLabel(minutes: Long): String = when {
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m screen time (24h)"
    else -> "${minutes}m screen time (24h)"
}
