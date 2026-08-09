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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifespan.app.ui.theme.Ok

@Composable
fun OnboardingScreen(
    notificationsGranted: Boolean,
    usageAccessGranted: Boolean,
    canDrawOverlays: Boolean,
    ignoringBatteryOptimizations: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestBatteryOptimizations: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Text(
            "Welcome to LifeSpan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Grant a few permissions so monitoring, alerts, and insights work reliably.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(24.dp))

        PermissionRow(
            icon = Icons.Filled.Notifications,
            title = "Notifications",
            subtitle = "Live charge status and thermal alerts",
            granted = notificationsGranted,
            onGrant = onRequestNotifications,
        )
        Spacer(Modifier.height(12.dp))
        PermissionRow(
            icon = Icons.Filled.QueryStats,
            title = "Usage access",
            subtitle = "See which apps use the most battery",
            granted = usageAccessGranted,
            onGrant = onRequestUsageAccess,
        )
        Spacer(Modifier.height(12.dp))
        PermissionRow(
            icon = Icons.Filled.Layers,
            title = "Display over other apps",
            subtitle = "Floating charging bubble while plugged in",
            granted = canDrawOverlays,
            onGrant = onRequestOverlay,
        )
        Spacer(Modifier.height(12.dp))
        PermissionRow(
            icon = Icons.Filled.BatterySaver,
            title = "Battery optimization",
            subtitle = "Keep the monitor running in the background",
            granted = ignoringBatteryOptimizations,
            onGrant = onRequestBatteryOptimizations,
        )

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Get started")
        }
        TextButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Skip for now")
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (granted) Ok else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
            Spacer(Modifier.width(8.dp))
            if (granted) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Granted", tint = Ok)
            } else {
                OutlinedButton(onClick = onGrant) { Text("Allow") }
            }
        }
    }
}
