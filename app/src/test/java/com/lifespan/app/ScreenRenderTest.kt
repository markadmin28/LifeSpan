package com.lifespan.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.db.TelemetryLogEntity
import com.lifespan.app.domain.health.BatteryHealthEstimate
import com.lifespan.app.domain.health.BatteryHealthEstimator
import com.lifespan.app.domain.health.CapacityObservation
import com.lifespan.app.domain.health.ReportedHealth
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType
import com.lifespan.app.domain.session.SessionStatsCalculator
import com.lifespan.app.ui.MainScreen
import com.lifespan.app.ui.MainUiState
import com.lifespan.app.ui.SessionDetailScreen
import com.lifespan.app.ui.SessionDetailUiState
import com.lifespan.app.ui.theme.LifeSpanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the screens on the JVM to check that the battery-lifespan card and the
 * charge-session detail view actually compose and show the figures they derive.
 *
 * Passing `-Dlifespan.captureScreenshots=true` also writes each rendered screen
 * to `app/build/screenshots`, which is how the review screenshots are produced.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    // The screens are handed their state directly, so skip LifeSpanApp: its
    // onCreate schedules WorkManager, which has no place in a render test.
    application = Application::class,
)
class ScreenRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `lifespan card reports state of health against the original capacity`() {
        renderMainScreen(health = HEALTHY_ESTIMATE)

        compose.onNodeWithText("Battery lifespan").assertExists()
        compose.onNodeWithText("80%").assertExists()
        compose.onNodeWithText("of original capacity").assertExists()
        compose.onNodeWithText("GOOD").assertExists()
        compose.onNodeWithText("3,600 mAh measured · 4,500 mAh when new").assertExists()
        compose.onNodeWithText("≈900 mAh lost to wear").assertExists()
        compose.onNodeWithText("Building confidence · 3 sessions").assertExists()
        compose.onNodeWithText("Reported: Good").assertExists()

        capture("main_screen_battery_lifespan")
    }

    @Test
    fun `lifespan card asks for a longer charge before it can estimate`() {
        renderMainScreen(
            health = BatteryHealthEstimator.estimate(
                observations = listOf(CapacityObservation(levelDeltaPercent = 6, mahAdded = 210.0)),
                designCapacityMah = 4500.0,
                reportedHealth = ReportedHealth.GOOD,
            ),
        )

        compose.onNodeWithText("Collecting data").assertExists()
        compose.onNodeWithText("Charge through at least 15%", substring = true).assertExists()
        compose.onNodeWithText("No data yet").assertExists()

        capture("main_screen_collecting_data")
    }

    @Test
    fun `lifespan card reports capacity alone when the design rating is unavailable`() {
        renderMainScreen(
            health = BatteryHealthEstimator.estimate(
                observations = listOf(CapacityObservation(levelDeltaPercent = 45, mahAdded = 1620.0)),
                designCapacityMah = null,
                reportedHealth = ReportedHealth.GOOD,
            ),
        )

        compose.onNodeWithText("3,600 mAh").assertExists()
        compose.onNodeWithText("does not expose its factory", substring = true).assertExists()
    }

    @Test
    fun `charge sessions are presented as tappable`() {
        renderMainScreen(health = HEALTHY_ESTIMATE)

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Charge sessions"))
        compose.onNodeWithText("Tap a session for its charge curve").assertExists()
        compose.onNodeWithText("Level: 38% → 82%").assertExists()
    }

    @Test
    fun `session detail plots the recorded curves and derived stats`() {
        renderSessionDetail()

        compose.onNodeWithText("Charge session").assertExists()
        compose.onNodeWithText("38%").assertExists()
        compose.onNodeWithText("AC").assertExists()
        // The end level heads the summary and also labels the level chart.
        compose.onAllNodesWithText("82%").onFirst().assertExists()

        compose.onNodeWithText("Duration").assertExists()
        compose.onNodeWithText("1h 30m").assertExists()
        compose.onNodeWithText("Charge rate").assertExists()
        compose.onNodeWithText("+29.3 %/h").assertExists()
        compose.onNodeWithText("Charge added").assertExists()
        compose.onNodeWithText("1800 mAh").assertExists()
        compose.onNodeWithText("Average current").assertExists()
        compose.onNodeWithText("1200 mA").assertExists()
        compose.onNodeWithText("Implied full capacity").assertExists()
        compose.onNodeWithText("4091 mAh").assertExists()

        compose.onNodeWithText("Battery level").assertExists()

        capture("session_detail_charge_curves")
    }

    @Test
    fun `session detail scrolls through every telemetry chart`() {
        renderSessionDetail()

        val list = compose.onNode(hasScrollAction())
        list.performScrollToNode(hasText("Temperature"))
        compose.onNodeWithText("peak 39.8°C").assertExists()

        list.performScrollToNode(hasText("Charge current"))
        compose.onNodeWithText("peak 2150 mA").assertExists()

        list.performScrollToNode(hasText("24 readings logged", substring = true))
        compose.onNodeWithText("24 readings logged", substring = true).assertExists()

        capture("session_detail_temperature_and_current")
    }

    @Test
    fun `session detail explains an empty session instead of drawing nothing`() {
        renderSessionDetail(telemetry = emptyList())

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Charge current"))
        // One per chart: level, temperature and current.
        compose.onAllNodesWithText("No readings logged.").assertCountEquals(3)
    }

    private fun renderMainScreen(health: BatteryHealthEstimate) = renderDark {
        MainScreen(
            state = MainUiState(snapshot = SNAPSHOT, monitoring = true, sessions = listOf(SESSION)),
            onStart = {},
            onStop = {},
            onChargeLimitChange = {},
            onOverheatChange = {},
            onOverheatEnabledChange = {},
            onChargeLimitEnabledChange = {},
            health = health,
        )
    }

    private fun renderSessionDetail(telemetry: List<TelemetryLogEntity> = TELEMETRY) = renderDark {
        SessionDetailScreen(
            state = SessionDetailUiState(
                session = SESSION,
                telemetry = telemetry,
                stats = SessionStatsCalculator.compute(
                    startTime = SESSION.startTime,
                    endTime = SESSION.endTime,
                    startLevel = SESSION.startLevel,
                    endLevel = SESSION.endLevel,
                    totalMahAdded = SESSION.totalMahAdded,
                    nowMillis = SESSION_END,
                ),
            ),
            onBack = {},
        )
    }

    private fun renderDark(content: @Composable () -> Unit) {
        compose.setContent { LifeSpanTheme(darkTheme = true) { content() } }
        compose.waitForIdle()
    }

    /**
     * Draws the hosting window straight into a bitmap. Compose's own
     * `captureToImage` waits on a display frame callback that never arrives off
     * a device, whereas the view draw path works under native graphics.
     */
    private fun capture(name: String) {
        if (System.getProperty("lifespan.captureScreenshots") != "true") return
        compose.waitForIdle()

        val view = compose.activity.window.decorView
        if (view.width == 0 || view.height == 0) {
            val metrics = compose.activity.resources.displayMetrics
            view.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        }

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val directory = File("build/screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private companion object {
        const val SESSION_START = 1_700_000_000_000L
        const val SESSION_DURATION = 5_400_000L
        const val SESSION_END = SESSION_START + SESSION_DURATION
        const val SAMPLES = 24

        val HEALTHY_ESTIMATE = BatteryHealthEstimator.estimate(
            observations = listOf(
                CapacityObservation(levelDeltaPercent = 45, mahAdded = 1620.0),
                CapacityObservation(levelDeltaPercent = 40, mahAdded = 1440.0),
                CapacityObservation(levelDeltaPercent = 50, mahAdded = 1800.0),
            ),
            designCapacityMah = 4500.0,
            reportedHealth = ReportedHealth.GOOD,
        )

        val SNAPSHOT = BatterySnapshot(
            timestamp = SESSION_END,
            level = 82,
            voltageMv = 4210,
            currentUa = 1_850_000,
            temperatureCelsius = 39.4,
            isCharging = true,
            plugType = PlugType.AC,
            technology = "Li-ion",
            chargeCounterMicroAh = 3_000_000L,
        )

        val SESSION = ChargeSessionEntity(
            id = 1,
            startTime = SESSION_START,
            endTime = SESSION_END,
            startLevel = 38,
            endLevel = 82,
            plugType = "AC",
            peakTempCelsius = 39.8,
            peakCurrentMa = 2150.0,
            totalMahAdded = 1800.0,
        )

        /** A plausible charge curve: level climbs, temperature rises, current tapers. */
        val TELEMETRY = List(SAMPLES) { index ->
            val last = SAMPLES - 1
            TelemetryLogEntity(
                id = index.toLong(),
                sessionId = SESSION.id,
                timestamp = SESSION_START + index * SESSION_DURATION / last,
                batteryLevel = 38 + index * 44 / last,
                voltageMv = 4050 + index * 160 / last,
                currentMa = 2150 - index * 1250 / last,
                temperatureCelsius = 31.0 + index * 8.8 / last,
                isCharging = true,
            )
        }
    }
}
