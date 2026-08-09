package com.lifespan.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifespan.app.data.prefs.ThemeMode
import com.lifespan.app.ui.theme.LifeSpanTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* refreshed on resume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: MainViewModel = viewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            val usage by vm.usage.collectAsStateWithLifecycle()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val health by vm.health.collectAsStateWithLifecycle()
            val selectedSessionId by vm.selectedSessionId.collectAsStateWithLifecycle()
            val sessionLogs by vm.sessionLogs.collectAsStateWithLifecycle()
            val context = LocalContext.current

            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LifeSpanTheme(darkTheme = darkTheme, accent = settings.accent) {
                var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
                var ignoringBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
                var notificationsGranted by remember { mutableStateOf(hasNotificationsPermission(context)) }
                var showHighUsage by remember { mutableStateOf(false) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            canDrawOverlays = Settings.canDrawOverlays(context)
                            ignoringBattery = isIgnoringBatteryOptimizations(context)
                            notificationsGranted = hasNotificationsPermission(context)
                            vm.refreshUsage()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val overlayLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { canDrawOverlays = Settings.canDrawOverlays(context) }
                val usageAccessLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { vm.refreshUsage() }
                val batteryOptLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { ignoringBattery = isIgnoringBatteryOptimizations(context) }

                val requestOverlay: () -> Unit = {
                    overlayLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                }
                val requestUsageAccess: () -> Unit = {
                    usageAccessLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                val requestBatteryOpt: () -> Unit = {
                    runCatching {
                        batteryOptLauncher.launch(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                }
                val requestNotificationsPermission: () -> Unit = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
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

                when {
                    !settings.onboardingComplete -> {
                        OnboardingScreen(
                            notificationsGranted = notificationsGranted,
                            usageAccessGranted = usage.hasAccess,
                            canDrawOverlays = canDrawOverlays,
                            ignoringBatteryOptimizations = ignoringBattery,
                            onRequestNotifications = requestNotificationsPermission,
                            onRequestUsageAccess = requestUsageAccess,
                            onRequestOverlay = requestOverlay,
                            onRequestBatteryOptimizations = requestBatteryOpt,
                            onFinish = { vm.setOnboardingComplete(true) },
                        )
                    }

                    selectedSessionId != null -> {
                        BackHandler { vm.closeSession() }
                        val session = state.sessions.firstOrNull { it.id == selectedSessionId }
                        SessionDetailScreen(
                            session = session,
                            logs = sessionLogs,
                            onBack = vm::closeSession,
                        )
                    }

                    showHighUsage -> {
                        BackHandler { showHighUsage = false }
                        HighUsageScreen(
                            state = usage,
                            onBack = { showHighUsage = false },
                            onForceStop = onForceStop,
                            onRefresh = vm::refreshUsage,
                        )
                    }

                    else -> {
                        MainScreen(
                            state = state,
                            settings = settings,
                            health = health,
                            onStart = vm::startMonitoring,
                            onStop = vm::stopMonitoring,
                            onChargeLimitChange = vm::setChargeLimit,
                            onOverheatChange = vm::setOverheatCelsius,
                            onOverheatEnabledChange = vm::setOverheatEnabled,
                            onChargeLimitEnabledChange = vm::setChargeLimitEnabled,
                            canDrawOverlays = canDrawOverlays,
                            onBubbleEnabledChange = { enabled ->
                                if (enabled && !Settings.canDrawOverlays(context)) {
                                    requestOverlay()
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
                                    requestUsageAccess()
                                }
                            },
                            onAutoStartChange = vm::setAutoStartEnabled,
                            onPeriodicSamplingChange = vm::setPeriodicSamplingEnabled,
                            onPersistentAlarmChange = vm::setPersistentChargeAlarm,
                            onRapidRiseChange = vm::setRapidRiseEnabled,
                            onThemeModeChange = vm::setThemeMode,
                            onAccentChange = vm::setAccent,
                            ignoringBatteryOptimizations = ignoringBattery,
                            onRequestIgnoreBatteryOptimizations = requestBatteryOpt,
                            onOpenSession = vm::openSession,
                        )
                    }
                }
            }
        }
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun hasNotificationsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
