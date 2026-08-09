package com.lifespan.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifespan.app.ui.theme.Ok

/** Live permission state shown by the onboarding wizard. */
data class OnboardingPermissions(
    val notificationsGranted: Boolean = false,
    /** POST_NOTIFICATIONS only exists on Android 13+. */
    val notificationsSupported: Boolean = true,
    val usageAccessGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val batteryOptimizationExempt: Boolean = false,
)

private data class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val actionLabel: String? = null,
    val granted: Boolean = false,
    val onAction: (() -> Unit)? = null,
)

/**
 * First-run wizard: introduces the app and walks through each optional
 * permission with a live granted indicator. Every step can be skipped;
 * permissions can always be granted later from the dashboard.
 */
@Composable
fun OnboardingScreen(
    permissions: OnboardingPermissions,
    onRequestNotifications: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onFinish: () -> Unit,
) {
    val steps = buildList {
        add(
            OnboardingStep(
                icon = Icons.Filled.BatteryChargingFull,
                title = "Welcome to LifeSpan",
                body = "Monitor charging speed, temperature and battery wear in real time — " +
                    "and get alerted before heat or overcharging shortens your battery's life.",
            ),
        )
        if (permissions.notificationsSupported) {
            add(
                OnboardingStep(
                    icon = Icons.Filled.Notifications,
                    title = "Stay informed",
                    body = "Notifications power the live monitoring status and the overheat " +
                        "and charge-limit alerts.",
                    actionLabel = "Allow notifications",
                    granted = permissions.notificationsGranted,
                    onAction = onRequestNotifications,
                ),
            )
        }
        add(
            OnboardingStep(
                icon = Icons.Filled.BatteryAlert,
                title = "Find battery hogs",
                body = "Usage access lets LifeSpan estimate which apps drain your battery the " +
                    "most, so you can force-stop the worst offenders.",
                actionLabel = "Grant usage access",
                granted = permissions.usageAccessGranted,
                onAction = onRequestUsageAccess,
            ),
        )
        add(
            OnboardingStep(
                icon = Icons.Filled.BubbleChart,
                title = "Floating charging bubble",
                body = "Display over other apps enables the draggable bubble with live " +
                    "charge-limit and overheat progress while you charge.",
                actionLabel = "Allow overlay",
                granted = permissions.overlayGranted,
                onAction = onRequestOverlay,
            ),
        )
        add(
            OnboardingStep(
                icon = Icons.Filled.Shield,
                title = "Reliable monitoring",
                body = "Exempting LifeSpan from battery optimization keeps monitoring and " +
                    "alerts running in the background.",
                actionLabel = "Allow in background",
                granted = permissions.batteryOptimizationExempt,
                onAction = onRequestBatteryExemption,
            ),
        )
        add(
            OnboardingStep(
                icon = Icons.Filled.TaskAlt,
                title = "You're all set",
                body = "Start monitoring from the dashboard whenever you're ready. Anything " +
                    "you skipped can be granted later from the app's cards and settings.",
            ),
        )
    }

    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[stepIndex.coerceIn(0, steps.lastIndex)]
    val isLast = stepIndex >= steps.lastIndex

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (!isLast) {
                    TextButton(onClick = onFinish) { Text("Skip") }
                }
            }

            AnimatedContent(
                targetState = step,
                label = "onboarding-step",
                modifier = Modifier.weight(1f),
            ) { current ->
                StepContent(current)
            }

            StepDots(count = steps.size, selected = stepIndex)

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (stepIndex > 0) {
                    OutlinedButton(onClick = { stepIndex-- }) { Text("Back") }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Button(
                    onClick = { if (isLast) onFinish() else stepIndex++ },
                ) {
                    Text(if (isLast) "Get started" else "Next")
                }
            }
        }
    }
}

@Composable
private fun StepContent(step: OnboardingStep) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            shape = CircleShape,
        ) {
            Icon(
                step.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(26.dp)
                    .size(54.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            step.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            step.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        step.actionLabel?.let { label ->
            Spacer(Modifier.height(24.dp))
            if (step.granted) {
                Surface(color = Ok.copy(alpha = 0.16f), shape = CircleShape) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Ok,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Granted", color = Ok, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(onClick = { step.onAction?.invoke() }) { Text(label) }
            }
        }
    }
}

@Composable
private fun StepDots(count: Int, selected: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (i == selected) 10.dp else 8.dp)
                    .background(
                        color = if (i == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}
