package com.example.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.model.FlashSolveLocalization
import com.example.model.PillMode
import com.example.model.PracticeQuestion
import com.example.model.SolveHistoryItem
import com.example.model.SolveStatus
import com.example.network.GeminiFlashClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Central state and screen-capture controller shared between MainActivity,
 * ViewModel, and FlashSolveOverlayService (WindowManager).
 * Ensures all image processing and OkHttp network calls run off the main thread.
 */
object FlashSolveController {

    private const val PREFS_NAME = "flashsolve_secure_prefs"
    private const val KEY_API_KEY = "custom_gemini_api_key"
    private const val KEY_LANGUAGE = "app_language"

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _pillMode = MutableStateFlow(PillMode.ACTIVE)
    val pillMode: StateFlow<PillMode> = _pillMode.asStateFlow()

    private val _solveStatus = MutableStateFlow(SolveStatus.IDLE)
    val solveStatus: StateFlow<SolveStatus> = _solveStatus.asStateFlow()

    private val _latestAnswer = MutableStateFlow("[—]")
    val latestAnswer: StateFlow<String> = _latestAnswer.asStateFlow()

    private val _latestLatencyMs = MutableStateFlow<Long?>(null)
    val latestLatencyMs: StateFlow<Long?> = _latestLatencyMs.asStateFlow()

    private val _liveTimerMs = MutableStateFlow(0L)
    val liveTimerMs: StateFlow<Long> = _liveTimerMs.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.EN)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _hasMediaProjection = MutableStateFlow(false)
    val hasMediaProjection: StateFlow<Boolean> = _hasMediaProjection.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _solveHistory = MutableStateFlow<List<SolveHistoryItem>>(emptyList())
    val solveHistory: StateFlow<List<SolveHistoryItem>> = _solveHistory.asStateFlow()

    private var activeActivityRef: WeakReference<Activity>? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    private var activeSolveJob: Job? = null
    private var timerJob: Job? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedKey = prefs.getString(KEY_API_KEY, "") ?: ""
            val savedLang = prefs.getString(KEY_LANGUAGE, AppLanguage.EN.code) ?: AppLanguage.EN.code
            _customApiKey.value = savedKey
            _language.value = if (savedLang == AppLanguage.AR.code) AppLanguage.AR else AppLanguage.EN
            isInitialized = true
        }
        refreshPermissions(context)
    }

    fun attachActivity(activity: Activity) {
        activeActivityRef = WeakReference(activity)
        refreshPermissions(activity)
    }

    fun detachActivity(activity: Activity) {
        if (activeActivityRef?.get() === activity) {
            activeActivityRef = null
        }
    }

    fun refreshPermissions(context: Context) {
        val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        _hasOverlayPermission.value = canDraw
    }

    fun setLanguage(context: Context, newLanguage: AppLanguage) {
        _language.value = newLanguage
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, newLanguage.code)
            .apply()
    }

    fun toggleLanguage(context: Context) {
        val next = if (_language.value == AppLanguage.EN) AppLanguage.AR else AppLanguage.EN
        setLanguage(context, next)
    }

    fun saveCustomApiKey(context: Context, key: String) {
        val cleaned = key.trim()
        _customApiKey.value = cleaned
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, cleaned)
            .apply()
        if (_errorMessage.value != null && cleaned.isNotEmpty()) {
            _errorMessage.value = null
            if (_latestAnswer.value == "[KEY?]") {
                _latestAnswer.value = "[—]"
            }
        }
    }

    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value.trim()
        if (custom.isNotEmpty()) return custom
        val buildConfigKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildConfigKey.isNotEmpty() && buildConfigKey != "MY_GEMINI_API_KEY" && buildConfigKey != "null") {
            return buildConfigKey
        }
        return ""
    }

    fun isBuildConfigKeyConfigured(): Boolean {
        val buildConfigKey = BuildConfig.GEMINI_API_KEY.trim()
        return buildConfigKey.isNotEmpty() && buildConfigKey != "MY_GEMINI_API_KEY" && buildConfigKey != "null"
    }

    fun setPillMode(mode: PillMode) {
        _pillMode.value = mode
    }

    fun togglePillMode() {
        _pillMode.value = if (_pillMode.value == PillMode.ACTIVE) PillMode.STANDBY else PillMode.ACTIVE
    }

    fun nextPracticeQuestion() {
        val total = FlashSolveLocalization.sampleQuestions.size
        _currentQuestionIndex.update { (it + 1) % total }
    }

    fun currentPracticeQuestion(): PracticeQuestion {
        val list = FlashSolveLocalization.sampleQuestions
        return list[_currentQuestionIndex.value.coerceIn(0, list.lastIndex)]
    }

    fun clearHistory() {
        _solveHistory.value = emptyList()
    }

    /**
     * Master Start/Stop switch handler.
     */
    fun setMasterServiceEnabled(context: Context, enabled: Boolean) {
        refreshPermissions(context)
        if (enabled) {
            _isServiceRunning.value = true
            _pillMode.value = PillMode.ACTIVE
            _errorMessage.value = null
            FlashSolveOverlayService.startOrUpdateService(context)
        } else {
            activeSolveJob?.cancel()
            timerJob?.cancel()
            _solveStatus.value = SolveStatus.IDLE
            _isServiceRunning.value = false
            FlashSolveOverlayService.stopService(context)
        }
    }

    fun bindMediaProjection(context: Context, resultCode: Int, data: Intent) {
        try {
            releaseMediaProjection()
            val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = mgr.getMediaProjection(resultCode, data) ?: return
            mediaProjection = projection

            val thread = HandlerThread("FlashSolveScreenCapture").apply { start() }
            val handler = Handler(thread.looper)
            captureThread = thread
            captureHandler = handler

            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    _hasMediaProjection.value = false
                    releaseVirtualDisplay()
                }
            }, handler)

            val metrics = context.resources.displayMetrics
            val captureW = (metrics.widthPixels / 2).coerceAtLeast(360)
            val captureH = (metrics.heightPixels / 2).coerceAtLeast(640)
            val density = metrics.densityDpi

            val reader = ImageReader.newInstance(captureW, captureH, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            virtualDisplay = projection.createVirtualDisplay(
                "FlashSolveVirtualDisplay",
                captureW,
                captureH,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )
            _hasMediaProjection.value = true
        } catch (e: Exception) {
            _hasMediaProjection.value = false
        }
    }

    fun releaseMediaProjection() {
        releaseVirtualDisplay()
        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        mediaProjection = null
        _hasMediaProjection.value = false
    }

    private fun releaseVirtualDisplay() {
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        imageReader = null
        try {
            captureThread?.quitSafely()
        } catch (_: Exception) {
        }
        captureThread = null
        captureHandler = null
    }

    /**
     * Triggers an immediate screen capture + asynchronous OkHttp POST request to Gemini Flash REST API.
     * Can be invoked from the WindowManager Pill button, the In-App Pill, or the Test Arena button.
     */
    fun triggerSolve(
        onBeforeCaptureHideOverlay: (() -> Unit)? = null,
        onAfterCaptureRestoreOverlay: (() -> Unit)? = null
    ) {
        if (_solveStatus.value == SolveStatus.CAPTURING || _solveStatus.value == SolveStatus.SOLVING) {
            return
        }

        // Automatically wake Pill from Standby to Active when Solve is triggered
        if (_pillMode.value == PillMode.STANDBY) {
            _pillMode.value = PillMode.ACTIVE
        }

        activeSolveJob?.cancel()
        activeSolveJob = controllerScope.launch {
            _errorMessage.value = null
            _solveStatus.value = SolveStatus.CAPTURING
            _liveTimerMs.value = 0L

            // Briefly hide the floating pill during screen capture so it doesn't occlude text
            onBeforeCaptureHideOverlay?.invoke()
            delay(35L)

            val capturePair = captureScreenBitmapInternal()
            onAfterCaptureRestoreOverlay?.invoke()

            val capturedBitmap = capturePair.first
            val captureSource = capturePair.second

            _solveStatus.value = SolveStatus.SOLVING
            startLiveTimer()

            val effectiveKey = getEffectiveApiKey()
            val apiResult = GeminiFlashClient.solveScreenAsync(
                screenBitmap = capturedBitmap,
                apiKey = effectiveKey
            )

            stopLiveTimer()
            _latestAnswer.value = apiResult.answerBracketed
            _latestLatencyMs.value = apiResult.latencyMs

            if (apiResult.isSuccess) {
                _solveStatus.value = SolveStatus.RESULT
                _errorMessage.value = null
                appendHistoryItem(
                    answer = apiResult.answerBracketed,
                    latencyMs = apiResult.latencyMs,
                    source = captureSource,
                    isSuccess = true
                )
            } else {
                _solveStatus.value = SolveStatus.ERROR
                _errorMessage.value = apiResult.rawError
                appendHistoryItem(
                    answer = apiResult.answerBracketed,
                    latencyMs = apiResult.latencyMs,
                    source = captureSource,
                    isSuccess = false
                )
            }
        }
    }

    private fun startLiveTimer() {
        timerJob?.cancel()
        timerJob = controllerScope.launch {
            val start = android.os.SystemClock.elapsedRealtime()
            while (true) {
                _liveTimerMs.value = android.os.SystemClock.elapsedRealtime() - start
                delay(45L)
            }
        }
    }

    private fun stopLiveTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun appendHistoryItem(
        answer: String,
        latencyMs: Long,
        source: String,
        isSuccess: Boolean
    ) {
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val item = SolveHistoryItem(
            id = UUID.randomUUID().toString(),
            timestampFormatted = timeFmt,
            answerText = answer,
            latencyMs = latencyMs,
            sourceLabel = source,
            isSuccess = isSuccess
        )
        _solveHistory.update { existing -> (listOf(item) + existing).take(20) }
    }

    /**
     * Captures the screen content using:
     * 1. Active MediaProjection ImageReader (when capturing over external apps or system-wide),
     * 2. Activity Root Window View drawing (when inside FlashSolve AI), or
     * 3. High-contrast rendered bitmap of the current Practice Question card as an infallible fallback.
     */
    private suspend fun captureScreenBitmapInternal(): Pair<Bitmap, String> {
        // 1. Try MediaProjection frame first on Dispatchers.Default
        val projectionBitmap = withContext(Dispatchers.Default) {
            acquireMediaProjectionBitmap()
        }
        if (projectionBitmap != null) {
            return projectionBitmap to "MediaProjection"
        }

        // 2. Try capturing the active Activity window decorView on Main, then scaling on Default
        val activity = activeActivityRef?.get()
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            val windowBitmap = withContext(Dispatchers.Main.immediate) {
                captureActivityDecorBitmap(activity)
            }
            if (windowBitmap != null) {
                return windowBitmap to "Live Screen"
            }
        }

        // 3. Fallback: render the active practice question onto a clean bitmap off the main thread
        val fallbackBitmap = withContext(Dispatchers.Default) {
            renderPracticeQuestionBitmap(currentPracticeQuestion(), _language.value)
        }
        return fallbackBitmap to "Practice Screen"
    }

    private fun acquireMediaProjectionBitmap(): Bitmap? {
        val reader = imageReader ?: return null
        var image: Image? = null
        return try {
            image = reader.acquireLatestImage() ?: return null
            val width = image.width
            val height = image.height
            val plane = image.planes.firstOrNull() ?: return null
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width

            val paddedWidth = width + rowPadding / pixelStride
            val rawBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
            buffer.rewind()
            rawBitmap.copyPixelsFromBuffer(buffer)

            if (paddedWidth != width) {
                val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, width, height)
                rawBitmap.recycle()
                cropped
            } else {
                rawBitmap
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                image?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun captureActivityDecorBitmap(activity: Activity): Bitmap? {
        return try {
            val rootView = activity.window?.decorView?.rootView ?: return null
            val w = rootView.width
            val h = rootView.height
            if (w <= 0 || h <= 0) return null
            val scale = 0.5f
            val targetW = (w * scale).toInt().coerceAtLeast(320)
            val targetH = (h * scale).toInt().coerceAtLeast(480)
            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            rootView.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun renderPracticeQuestionBitmap(
        question: PracticeQuestion,
        lang: AppLanguage
    ): Bitmap {
        val width = 640
        val height = 420
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#0A0F1D"))

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#182235")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(20f, 20f, width - 20f, height - 20f), 24f, 24f, cardPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val promptText = if (lang == AppLanguage.AR) question.questionAr else question.questionEn
        val options = if (lang == AppLanguage.AR) question.optionsAr else question.optionsEn

        // Draw wrapped question lines
        val words = promptText.split(" ")
        var line = ""
        var y = 75f
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (textPaint.measureText(testLine) > width - 80f) {
                canvas.drawText(line, 40f, y, textPaint)
                line = word
                y += 32f
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, 40f, y, textPaint)
            y += 46f
        }

        val optionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        for (opt in options) {
            canvas.drawText(opt, 48f, y, optionPaint)
            y += 42f
        }

        return bitmap
    }
}
