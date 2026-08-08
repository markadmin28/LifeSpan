package com.lifespan.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifespan.app.ui.theme.LifeSpanTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LifeSpanTheme {
                val vm: MainViewModel = viewModel()
                val state by vm.uiState.collectAsStateWithLifecycle()
                val usage by vm.usage.collectAsStateWithLifecycle()
                val context = LocalContext.current

                var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
                var showHighUsage by remember { mutableStateOf(false) }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            canDrawOverlays = Settings.canDrawOverlays(context)
                            vm.refreshUsage()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val overlayLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) {
                    canDrawOverlays = Settings.canDrawOverlays(context)
                }
                val usageAccessLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) {
                    vm.refreshUsage()
                }

                val onForceStop: (com.lifespan.app.domain.usage.AppUsage) -> Unit = { app ->
                    vm.killBackground(app.packageName)
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${app.packageName}"),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }

                if (showHighUsage) {
                    BackHandler { showHighUsage = false }
                    HighUsageScreen(
                        state = usage,
                        onBack = { showHighUsage = false },
                        onForceStop = onForceStop,
                        onRefresh = vm::refreshUsage,
                    )
                } else {
                    MainScreen(
                        state = state,
                        onStart = vm::startMonitoring,
                        onStop = vm::stopMonitoring,
                        onChargeLimitChange = vm::setChargeLimit,
                        onOverheatChange = vm::setOverheatCelsius,
                        onOverheatEnabledChange = vm::setOverheatEnabled,
                        onChargeLimitEnabledChange = vm::setChargeLimitEnabled,
                        canDrawOverlays = canDrawOverlays,
                        onBubbleEnabledChange = { enabled ->
                            if (enabled && !Settings.canDrawOverlays(context)) {
                                overlayLauncher.launch(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            }
                            vm.setBubbleEnabled(enabled)
                        },
                        usageAccess = usage.hasAccess,
                        topUsageLabel = usage.topApp?.label,
                        topUsagePercent = usage.topApp?.batteryPercent?.roundToInt(),
                        highUsageCount = usage.highCount,
                        onOpenHighUsage = {
                            if (usage.hasAccess) {
                                vm.refreshUsage()
                                showHighUsage = true
                            } else {
                                usageAccessLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        },
                    )
                }
            }
        }
    }
}
