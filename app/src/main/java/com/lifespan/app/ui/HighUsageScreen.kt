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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifespan.app.domain.usage.AppUsage
import com.lifespan.app.ui.theme.Amber
import com.lifespan.app.ui.theme.Danger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighUsageScreen(
    state: UsageUiState,
    onBack: () -> Unit,
    onForceStop: (AppUsage) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Ranked by foreground (screen) time over the last 24 hours — the closest " +
                        "signal an app can read as a battery-drain proxy. Android doesn't let apps " +
                        "force-stop others directly, so \"Force stop\" opens the app's system page " +
                        "where you can stop it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
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
                    AppUsageRow(
                        app = app,
                        isHigh = state.isHigh(app),
                        onForceStop = { onForceStop(app) },
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AppUsageRow(app: AppUsage, isHigh: Boolean, onForceStop: () -> Unit) {
    val icon = rememberAppIcon(app.packageName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.label,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                if (isHigh) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = Danger.copy(alpha = 0.18f), shape = CircleShape) {
                        Text(
                            "HIGH",
                            color = Danger,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                usageLabel(app.foregroundMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = if (isHigh) Amber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }

        Spacer(Modifier.width(8.dp))

        OutlinedButton(onClick = onForceStop) {
            Text("Force stop")
        }
    }
}

private fun usageLabel(minutes: Long): String = when {
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m screen time (24h)"
    else -> "${minutes}m screen time (24h)"
}
