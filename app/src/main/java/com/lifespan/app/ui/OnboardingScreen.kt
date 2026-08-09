package com.lifespan.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    notificationGranted: Boolean,
    usageAccessGranted: Boolean,
    overlayGranted: Boolean,
    batteryOptimizationGranted: Boolean,
    onRequestNotification: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onComplete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item { Spacer(Modifier.height(28.dp)) }
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ) {
                Icon(
                    Icons.Filled.BatterySaver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(18.dp).size(38.dp),
                )
            }
        }
        item {
            Text(
                "Protect your battery",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "LifeSpan works locally on your device. Choose the access you want now; " +
                    "you can change every option later.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
        item {
            PermissionCard(
                icon = Icons.Filled.Notifications,
                title = "Safety alerts",
                description = "Notify you when charge or temperature limits are reached.",
                granted = notificationGranted,
                actionLabel = "Allow",
                onAction = onRequestNotification,
            )
        }
        item {
            PermissionCard(
                icon = Icons.Filled.QueryStats,
                title = "App usage insights",
                description = "Estimate which apps account for the most foreground battery use.",
                granted = usageAccessGranted,
                actionLabel = "Open settings",
                onAction = onRequestUsageAccess,
            )
        }
        item {
            PermissionCard(
                icon = Icons.Filled.ViewInAr,
                title = "Charging bubble",
                description = "Show compact charge and heat progress over other apps.",
                granted = overlayGranted,
                actionLabel = "Allow",
                onAction = onRequestOverlay,
            )
        }
        item {
            PermissionCard(
                icon = Icons.Filled.BatterySaver,
                title = "Reliable background monitoring",
                description = "Exclude LifeSpan from battery optimization for uninterrupted alerts.",
                granted = batteryOptimizationGranted,
                actionLabel = "Allow",
                onAction = onRequestBatteryOptimization,
            )
        }
        item {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Start using LifeSpan")
            }
            Text(
                "Optional access can be skipped.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (granted) Icons.Filled.Check else icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
            ) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    if (granted) "Ready" else description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
            OutlinedButton(onClick = onAction, enabled = !granted) {
                Text(if (granted) "Done" else actionLabel)
            }
        }
    }
}
