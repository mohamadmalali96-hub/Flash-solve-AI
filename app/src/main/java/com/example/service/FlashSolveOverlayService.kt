package com.example.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AppLanguage
import com.example.model.FlashSolveLocalization
import com.example.model.PillMode
import com.example.model.SolveStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

/**
 * Floating Overlay Service using Android's WindowManager to render a draggable
 * 'Pill' overlay with Active/Standby states and an immediate 'Solve' trigger button.
 */
class FlashSolveOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var windowManager: WindowManager? = null
    private var pillRootView: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // Pill UI subviews
    private var statusDotView: View? = null
    private var modeBadgeView: TextView? = null
    private var answerBadgeView: TextView? = null
    private var latencyTextView: TextView? = null
    private var solveTriggerButton: TextView? = null
    private var closePillButton: TextView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundWithNotification()
        attachOverlayIfPermitted()
        observeStateFlows()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                FlashSolveController.setMasterServiceEnabled(this, false)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SOLVE_NOW -> {
                triggerSolveWithTemporaryHide()
            }
            else -> {
                attachOverlayIfPermitted()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlayView()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForegroundWithNotification() {
        val channelId = NOTIFICATION_CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_text)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val solveIntent = Intent(this, FlashSolveOverlayService::class.java).apply {
            action = ACTION_SOLVE_NOW
        }
        val pendingSolve = PendingIntent.getService(
            this,
            101,
            solveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(0, getString(R.string.solve_button), pendingSolve)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fgType = if (FlashSolveController.hasMediaProjection.value) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                }
                startForeground(NOTIFICATION_ID, notification, fgType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // Fallback if foreground service restrictions apply in container environments
        }
    }

    private fun canDrawSystemOverlay(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachOverlayIfPermitted() {
        FlashSolveController.refreshPermissions(this)
        if (!canDrawSystemOverlay()) {
            removeOverlayView()
            return
        }
        if (pillRootView != null) return

        val wm = windowManager ?: return
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(16f)
            y = dpToPx(130f)
        }
        layoutParams = params

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12f), dpToPx(8f), dpToPx(10f), dpToPx(8f))
            elevation = dpToPx(12f).toFloat()
        }

        // 1. Glowing Status Dot (Tap to toggle Active/Standby)
        val dot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(10f), dpToPx(10f)).apply {
                marginEnd = dpToPx(8f)
            }
        }
        statusDotView = dot
        root.addView(dot)

        // 2. Mode / Status Pill Label (Tap to switch Active <-> Standby)
        val modeLabel = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.parseColor("#E2E8F0"))
            setPadding(dpToPx(6f), dpToPx(4f), dpToPx(8f), dpToPx(4f))
            setOnClickListener {
                FlashSolveController.togglePillMode()
            }
        }
        modeBadgeView = modeLabel
        root.addView(modeLabel)

        // 3. Immediate Answer Output Badge (e.g., [B])
        val answerBadge = TextView(this).apply {
            text = "[—]"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.parseColor("#00E676"))
            setPadding(dpToPx(10f), dpToPx(4f), dpToPx(10f), dpToPx(4f))
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(999f).toFloat()
                setColor(Color.parseColor("#1A00E676"))
                setStroke(dpToPx(1f), Color.parseColor("#5500E676"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(6f)
            }
        }
        answerBadgeView = answerBadge
        root.addView(answerBadge)

        // 4. Latency Readout (e.g., 580ms)
        val latencyLabel = TextView(this).apply {
            text = ""
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            setTextColor(Color.parseColor("#94A3B8"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(8f)
            }
        }
        latencyTextView = latencyLabel
        root.addView(latencyLabel)

        // 5. 'Solve' Trigger Button inside the Pill
        val solveBtn = TextView(this).apply {
            text = "⚡ Solve"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(Color.parseColor("#0A0F1D"))
            setPadding(dpToPx(14f), dpToPx(7f), dpToPx(14f), dpToPx(7f))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#00E5FF"), Color.parseColor("#00E676"))
            ).apply {
                cornerRadius = dpToPx(999f).toFloat()
            }
            setOnClickListener {
                triggerSolveWithTemporaryHide()
            }
        }
        solveTriggerButton = solveBtn
        root.addView(solveBtn)

        // 6. Compact Standby/Minimize toggle button
        val minBtn = TextView(this).apply {
            text = "◐"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(dpToPx(8f), dpToPx(4f), dpToPx(4f), dpToPx(4f))
            setOnClickListener {
                FlashSolveController.togglePillMode()
            }
        }
        closePillButton = minBtn
        root.addView(minBtn)

        // Enable smooth touch dragging across the screen + edge snap
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        root.setOnTouchListener { _, event ->
            val currentParams = layoutParams ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = currentParams.x
                    initialY = currentParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > dpToPx(4f) || abs(dy) > dpToPx(4f)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        currentParams.x = initialX + dx
                        currentParams.y = initialY + dy
                        try {
                            wm.updateViewLayout(root, currentParams)
                        } catch (_: Exception) {
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        if (FlashSolveController.pillMode.value == PillMode.STANDBY) {
                            FlashSolveController.setPillMode(PillMode.ACTIVE)
                        }
                    } else {
                        snapPillToNearestHorizontalBound(currentParams)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            wm.addView(root, params)
            pillRootView = root
        } catch (_: Exception) {
            pillRootView = null
        }
    }

    private fun snapPillToNearestHorizontalBound(params: WindowManager.LayoutParams) {
        val root = pillRootView ?: return
        val wm = windowManager ?: return
        val screenW = resources.displayMetrics.widthPixels
        val pillW = root.width.coerceAtLeast(dpToPx(180f))
        val targetX = if (params.x + pillW / 2 < screenW / 2) {
            dpToPx(12f)
        } else {
            (screenW - pillW - dpToPx(12f)).coerceAtLeast(dpToPx(12f))
        }
        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                params.x = anim.animatedValue as Int
                try {
                    if (pillRootView != null) {
                        wm.updateViewLayout(root, params)
                    }
                } catch (_: Exception) {
                }
            }
            start()
        }
    }

    private fun observeStateFlows() {
        serviceScope.launch {
            combine(
                FlashSolveController.pillMode,
                FlashSolveController.solveStatus,
                FlashSolveController.latestAnswer,
                FlashSolveController.latestLatencyMs,
                FlashSolveController.language
            ) { mode, status, answer, latency, lang ->
                OverlayUiSnapshot(mode, status, answer, latency, lang)
            }.collect { snapshot ->
                updatePillAppearance(snapshot)
            }
        }

        serviceScope.launch {
            FlashSolveController.liveTimerMs.collect { timerMs ->
                if (FlashSolveController.solveStatus.value == SolveStatus.SOLVING) {
                    val seconds = timerMs / 1000.0
                    latencyTextView?.text = String.format(Locale.US, "%.1fs", seconds)
                }
            }
        }
    }

    private fun updatePillAppearance(snapshot: OverlayUiSnapshot) {
        val root = pillRootView ?: return
        val strings = FlashSolveLocalization.forLanguage(snapshot.language)
        val isStandby = snapshot.mode == PillMode.STANDBY
        val isSolving = snapshot.status == SolveStatus.CAPTURING || snapshot.status == SolveStatus.SOLVING

        root.layoutDirection = if (snapshot.language == AppLanguage.AR) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }

        // Update outer pill background & alpha for Active vs Standby states
        val borderColor = when {
            isSolving -> Color.parseColor("#00E5FF")
            isStandby -> Color.parseColor("#66FFB300")
            else -> Color.parseColor("#9900E5FF")
        }
        val bgColor = if (isStandby) {
            Color.parseColor("#CC0A0F1D")
        } else {
            Color.parseColor("#F20D1627")
        }

        root.background = GradientDrawable().apply {
            cornerRadius = dpToPx(999f).toFloat()
            setColor(bgColor)
            setStroke(dpToPx(1.5f), borderColor)
        }
        root.alpha = if (isStandby) 0.78f else 1.0f

        // Update status dot color
        val dotColor = when {
            isSolving -> Color.parseColor("#00E5FF")
            isStandby -> Color.parseColor("#FFB300")
            snapshot.status == SolveStatus.ERROR -> Color.parseColor("#FF5252")
            else -> Color.parseColor("#00E676")
        }
        statusDotView?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(dotColor)
        }

        // Update mode text
        modeBadgeView?.text = if (isStandby) {
            strings.pillStandbyMode
        } else {
            strings.pillActiveMode
        }

        // Update answer badge & latency visibility based on Standby vs Active
        answerBadgeView?.visibility = if (isStandby) View.GONE else View.VISIBLE
        latencyTextView?.visibility = if (isStandby) View.GONE else View.VISIBLE

        answerBadgeView?.text = if (isSolving) "[···]" else snapshot.answer
        val isErr = snapshot.status == SolveStatus.ERROR || snapshot.answer == "[KEY?]" || snapshot.answer == "[ERR]"
        answerBadgeView?.setTextColor(
            if (isErr) Color.parseColor("#FF5252") else Color.parseColor("#00E676")
        )

        if (!isSolving) {
            latencyTextView?.text = snapshot.latencyMs?.let { "${it}ms" } ?: ""
        }

        // Update Solve button text
        solveTriggerButton?.text = if (isSolving) {
            strings.pillSolvingText
        } else {
            "⚡ ${strings.pillSolveBtn}"
        }
        solveTriggerButton?.isEnabled = !isSolving
    }

    private fun triggerSolveWithTemporaryHide() {
        FlashSolveController.triggerSolve(
            onBeforeCaptureHideOverlay = {
                pillRootView?.visibility = View.INVISIBLE
            },
            onAfterCaptureRestoreOverlay = {
                pillRootView?.visibility = View.VISIBLE
            }
        )
    }

    private fun removeOverlayView() {
        val view = pillRootView ?: return
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
        }
        pillRootView = null
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private data class OverlayUiSnapshot(
        val mode: PillMode,
        val status: SolveStatus,
        val answer: String,
        val latencyMs: Long?,
        val language: AppLanguage
    )

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "flashsolve_pill_channel"
        private const val NOTIFICATION_ID = 4091
        const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"
        const val ACTION_SOLVE_NOW = "com.example.action.SOLVE_NOW"

        fun startOrUpdateService(context: Context) {
            val intent = Intent(context, FlashSolveOverlayService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                try {
                    context.startService(intent)
                } catch (_: Exception) {
                }
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FlashSolveOverlayService::class.java)
            try {
                context.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
