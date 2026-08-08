package com.lifespan.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
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
                val context = LocalContext.current

                var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            canDrawOverlays = Settings.canDrawOverlays(context)
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
                )
            }
        }
    }
}
