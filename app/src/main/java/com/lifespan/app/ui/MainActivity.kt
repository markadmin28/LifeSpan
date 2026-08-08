package com.lifespan.app.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
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
                MainScreen(
                    state = state,
                    onStart = vm::startMonitoring,
                    onStop = vm::stopMonitoring,
                    onChargeLimitChange = vm::setChargeLimit,
                    onOverheatChange = vm::setOverheatCelsius,
                    onOverheatEnabledChange = vm::setOverheatEnabled,
                    onChargeLimitEnabledChange = vm::setChargeLimitEnabled,
                )
            }
        }
    }
}
