package com.lifespan.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.db.TelemetryLogEntity
import com.lifespan.app.data.prefs.Accent
import com.lifespan.app.data.prefs.AppSettings
import com.lifespan.app.domain.health.ChargeStats
import com.lifespan.app.domain.health.HealthStatus
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType
import com.lifespan.app.domain.usage.AppUsage
import com.lifespan.app.domain.usage.UsageRanker
import com.lifespan.app.ui.HealthUiState
import com.lifespan.app.ui.HighUsageScreen
import com.lifespan.app.ui.MainScreen
import com.lifespan.app.ui.MainUiState
import com.lifespan.app.ui.OnboardingPermissions
import com.lifespan.app.ui.OnboardingScreen
import com.lifespan.app.ui.SessionDetailScreen
import com.lifespan.app.ui.SessionDetailUiState
import com.lifespan.app.ui.UsageUiState
import com.lifespan.app.ui.theme.LifeSpanTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import java.util.TimeZone

/**
 * Golden-image screenshot tests rendered on the JVM with Robolectric native
 * graphics and compared by Roborazzi.
 *
 * Record / update goldens:  ./gradlew recordRoborazziDebug
 * Verify against goldens:   ./gradlew verifyRoborazziDebug (run in CI)
 *
 * All fixture data is fixed (timestamps, locale, timezone) so renders are
 * reproducible across machines.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// A plain Application avoids LifeSpanApp's WorkManager wiring, which is not
// initialized in the Robolectric environment (screens get fixture state).
@Config(
    sdk = [35],
    qualifiers = RobolectricDeviceQualifiers.Pixel7,
    application = android.app.Application::class,
)
class LifeSpanScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    // 2026-01-15T10:00:00Z.
    private val baseTime = 1_768_471_200_000L

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    @Before
    fun pinEnvironment() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeRule.setContent(content)
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/$name.png",
            roborazziOptions = roborazziOptions,
        )
    }

    @Test
    fun onboardingWelcome() = capture("onboarding_welcome") {
        LifeSpanTheme(darkTheme = false, accent = Accent.COOL) {
            OnboardingScreen(
                permissions = OnboardingPermissions(
                    notificationsGranted = false,
                    notificationsSupported = true,
                    usageAccessGranted = false,
                    overlayGranted = false,
                    batteryOptimizationExempt = false,
                ),
                onRequestNotifications = {},
                onRequestUsageAccess = {},
                onRequestOverlay = {},
                onRequestBatteryExemption = {},
                onFinish = {},
            )
        }
    }

    @Test
    fun mainScreenLightCool() = capture("main_screen_light_cool") {
        LifeSpanTheme(darkTheme = false, accent = Accent.COOL) {
            DashboardFixture()
        }
    }

    @Test
    fun mainScreenDarkWarm() = capture("main_screen_dark_warm") {
        LifeSpanTheme(darkTheme = true, accent = Accent.WARM) {
            DashboardFixture(settings = AppSettings(accent = Accent.WARM))
        }
    }

    @Test
    fun sessionDetailCharts() = capture("session_detail_charts") {
        LifeSpanTheme(darkTheme = false, accent = Accent.COOL) {
            SessionDetailScreen(
                state = SessionDetailUiState(
                    session = fixtureSession(),
                    telemetry = fixtureTelemetry(),
                ),
                onBack = {},
            )
        }
    }

    @Test
    fun highUsageApps() = capture("high_usage_apps") {
        LifeSpanTheme(darkTheme = false, accent = Accent.COOL) {
            HighUsageScreen(
                state = UsageUiState(
                    hasAccess = true,
                    loading = false,
                    apps = fixtureUsageApps(),
                ),
                onBack = {},
                onForceStop = {},
                onRefresh = {},
            )
        }
    }

    @Composable
    private fun DashboardFixture(settings: AppSettings = AppSettings()) {
        MainScreen(
            state = MainUiState(
                snapshot = fixtureSnapshot(),
                monitoring = true,
                sessions = listOf(
                    fixtureSession(),
                    fixtureSession(
                        id = 2,
                        startOffsetMs = -26_000_000L,
                        plugType = "USB",
                        startLevel = 20,
                        endLevel = 55,
                    ),
                ),
                bubbleEnabled = true,
            ),
            health = HealthUiState(
                status = HealthStatus.GOOD,
                estimatedCapacityMah = 4_412.0,
                designCapacityMah = 5_000.0,
                capacityPercent = 88,
                stats = ChargeStats(
                    sessionCount = 12,
                    percentAdded = 340,
                    mahAdded = 15_600.0,
                    avgPeakTempCelsius = 36.4,
                    maxPeakTempCelsius = 41.5,
                    hotSessionCount = 2,
                ),
            ),
            settings = settings,
            onStart = {},
            onStop = {},
            onChargeLimitChange = {},
            onOverheatChange = {},
            onOverheatEnabledChange = {},
            onChargeLimitEnabledChange = {},
            usageAccess = true,
            topUsageLabel = "StreamTube",
            topUsagePercent = 32,
            highUsageCount = 2,
        )
    }

    private fun fixtureSnapshot() = BatterySnapshot(
        timestamp = baseTime,
        level = 78,
        voltageMv = 4_210,
        currentUa = 1_850_000,
        temperatureCelsius = 33.4,
        isCharging = true,
        plugType = PlugType.AC,
        health = 2,
        technology = "Li-ion",
        chargeCounterMicroAh = 3_400_000L,
    )

    private fun fixtureSession(
        id: Long = 1,
        startOffsetMs: Long = -5_400_000L,
        plugType: String = "AC",
        startLevel: Int = 35,
        endLevel: Int = 80,
    ) = ChargeSessionEntity(
        id = id,
        startTime = baseTime + startOffsetMs,
        endTime = baseTime + startOffsetMs + 5_400_000L,
        startLevel = startLevel,
        endLevel = endLevel,
        plugType = plugType,
        peakTempCelsius = 41.5,
        peakCurrentMa = 2_400.0,
        totalMahAdded = 1_820.0,
    )

    /** 90 minutes of charging telemetry: level ramps, temp rises, current tapers. */
    private fun fixtureTelemetry(): List<TelemetryLogEntity> {
        val start = baseTime - 5_400_000L
        return (0..30).map { i ->
            val fraction = i / 30.0
            TelemetryLogEntity(
                id = i + 1L,
                sessionId = 1L,
                timestamp = start + i * 180_000L,
                batteryLevel = (35 + fraction * 45).toInt(),
                voltageMv = (3_900 + fraction * 400).toInt(),
                currentMa = (2_400 - fraction * 1_600).toInt(),
                temperatureCelsius = 30.0 + 11.5 * (1 - (1 - fraction) * (1 - fraction)),
                isCharging = true,
            )
        }
    }

    private fun fixtureUsageApps(): List<AppUsage> {
        val now = System.currentTimeMillis()
        return UsageRanker.rank(
            listOf(
                AppUsage(
                    packageName = "com.example.streamtube",
                    label = "StreamTube",
                    foregroundMillis = 96 * 60_000L,
                    lastUsedMillis = now - 2 * 60_000L,
                ),
                AppUsage(
                    packageName = "com.example.socialgram",
                    label = "SocialGram",
                    foregroundMillis = 74 * 60_000L,
                    lastUsedMillis = now - 40 * 60_000L,
                ),
                AppUsage(
                    packageName = "com.example.mapsnav",
                    label = "MapsNav",
                    foregroundMillis = 41 * 60_000L,
                    lastUsedMillis = now - 3 * 3_600_000L,
                ),
                AppUsage(
                    packageName = "com.example.chatly",
                    label = "Chatly",
                    foregroundMillis = 18 * 60_000L,
                    lastUsedMillis = now - 5 * 3_600_000L,
                ),
            ),
        )
    }
}
