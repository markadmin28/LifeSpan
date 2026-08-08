package com.lifespan.app.service

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.lifespan.app.R
import com.lifespan.app.domain.bubble.BubbleLevel
import com.lifespan.app.domain.bubble.BubbleStatus
import com.lifespan.app.ui.MainActivity
import java.util.Locale
import kotlin.math.abs

/**
 * Manages a draggable floating "chat-head" style overlay that shows live
 * charging status over other apps. Requires the "display over other apps"
 * (`SYSTEM_ALERT_WINDOW`) permission; all methods no-op safely when it is not
 * granted. Must be called from the main thread.
 */
class ChargingBubbleController(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var view: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded: Boolean = false

    private fun canDraw(): Boolean = Settings.canDrawOverlays(context)

    /** Show/update the bubble for [status], adding it to the window if needed. */
    fun render(status: BubbleStatus) {
        if (!canDraw()) {
            remove()
            return
        }
        val v = view ?: createView().also { view = it }
        bind(v, status)
    }

    /** Remove the bubble from the window if it is currently shown. */
    fun remove() {
        val v = view ?: return
        runCatching { windowManager.removeView(v) }
        view = null
        params = null
    }

    private fun createView(): View {
        val v = LayoutInflater.from(context).inflate(R.layout.bubble_overlay, null)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 160
        }
        params = lp
        attachDragHandler(v, lp)
        windowManager.addView(v, lp)
        return v
    }

    private fun bind(v: View, status: BubbleStatus) {
        val color = when (status.level) {
            BubbleLevel.NORMAL -> Color.parseColor("#5CC5EA")
            BubbleLevel.WARNING -> Color.parseColor("#F5B301")
            BubbleLevel.DANGER -> Color.parseColor("#E4572E")
        }
        val root = v.findViewById<View>(R.id.bubble_root)
        root.backgroundTintList = ColorStateList.valueOf(color)
        v.findViewById<ImageView>(R.id.bubble_icon)
            .imageTintList = ColorStateList.valueOf(Color.WHITE)
        v.findViewById<TextView>(R.id.bubble_title).text = String.format(
            Locale.US,
            "%d%%  ·  %.1f°C",
            status.percent,
            status.temperatureCelsius,
        )
        v.findViewById<TextView>(R.id.bubble_subtitle).text = String.format(
            Locale.US,
            "%.1f W  ·  %s",
            status.powerWatts,
            status.caption,
        )

        // Charge-limit progress bar
        v.findViewById<TextView>(R.id.bubble_charge_label).text = String.format(
            Locale.US,
            "Charge  %d%% / %d%%",
            status.percent,
            status.chargeLimitPercent,
        )
        v.findViewById<ProgressBar>(R.id.bubble_charge_bar)
            .setProgress(status.chargeProgress, true)

        // Overheat progress bar
        v.findViewById<TextView>(R.id.bubble_overheat_label).text = String.format(
            Locale.US,
            "Temp  %.1f°C / %.0f°C",
            status.temperatureCelsius,
            status.overheatCelsius,
        )
        v.findViewById<ProgressBar>(R.id.bubble_overheat_bar)
            .setProgress(status.overheatProgress, true)

        v.findViewById<View>(R.id.bubble_expand).visibility =
            if (expanded) View.VISIBLE else View.GONE
    }

    private fun attachDragHandler(v: View, lp: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var dragging = false
        var downTime = 0L
        val longPressMs = 500L

        v.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    dragging = false
                    downTime = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 12 || abs(dy) > 12) dragging = true
                    lp.x = initialX + dx
                    lp.y = initialY + dy
                    runCatching { windowManager.updateViewLayout(v, lp) }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        val heldMs = System.currentTimeMillis() - downTime
                        if (heldMs >= longPressMs) openApp() else toggleExpand(v)
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun toggleExpand(v: View) {
        expanded = !expanded
        v.findViewById<View>(R.id.bubble_expand).visibility =
            if (expanded) View.VISIBLE else View.GONE
        params?.let { runCatching { windowManager.updateViewLayout(v, it) } }
    }

    private fun openApp() {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
