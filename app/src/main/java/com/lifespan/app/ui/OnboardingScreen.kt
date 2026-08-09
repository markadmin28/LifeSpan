package com.lifespan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifespan.app.ui.theme.Ok

/** First-run wizard that guides the user through the permissions LifeSpan uses. */
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
    autoStartEnabled: Boolean = false,
    periodicSamplingEnabled: Boolean = false,
    onAutoStartChange: (Boolean) -> Unit = {},
    onPeriodicSamplingChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Welcome to LifeSpan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Grant a few permissions so LifeSpan can monitor battery health reliably. " +
                "Everything except notifications is optional — you can change these later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(20.dp))

        PermissionRow(
            title = "Notifications",
            description = "Show the live status and battery alerts.",
            granted = notificationsGranted,
            onGrant = onRequestNotifications,
        )
        PermissionRow(
            title = "Usage access",
            description = "Rank apps by battery-draining activity.",
            granted = usageAccessGranted,
            onGrant = onRequestUsageAccess,
        )
        PermissionRow(
            title = "Display over other apps",
            description = "Show the floating charging bubble.",
            granted = canDrawOverlays,
            onGrant = onRequestOverlay,
        )
        PermissionRow(
            title = "Ignore battery optimizations",
            description = "Keep monitoring reliable in the background.",
            granted = ignoringBatteryOptimizations,
            onGrant = onRequestBatteryOptimizations,
        )

        Spacer(Modifier.height(18.dp))
        Text(
            "Reliable background monitoring",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Choose how LifeSpan keeps your history current. You can review this setup later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )
        Spacer(Modifier.height(8.dp))
        ReliabilityToggleRow(
            title = "Auto-start on boot",
            description = "Resume foreground monitoring after your phone restarts.",
            checked = autoStartEnabled,
            onCheckedChange = onAutoStartChange,
        )
        ReliabilityToggleRow(
            title = "Periodic background sampling",
            description = "Record a sample about every 15 minutes with WorkManager.",
            checked = periodicSamplingEnabled,
            onCheckedChange = onPeriodicSamplingChange,
        )

        Spacer(Modifier.height(24.dp))
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
private fun ReliabilityToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.width(12.dp))
            if (granted) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Granted", tint = Ok)
            } else {
                OutlinedButton(onClick = onGrant) { Text("Grant") }
            }
        }
    }
}
