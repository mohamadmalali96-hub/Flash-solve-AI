package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FlashSolveLocalization
import com.example.service.FlashSolveController
import com.example.service.FlashSolveOverlayService
import com.example.ui.screens.FlashSolveDashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FlashSolveController.initialize(this)
        FlashSolveController.attachActivity(this)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val isServiceRunning by FlashSolveController.isServiceRunning.collectAsStateWithLifecycle()
                val pillMode by FlashSolveController.pillMode.collectAsStateWithLifecycle()
                val solveStatus by FlashSolveController.solveStatus.collectAsStateWithLifecycle()
                val latestAnswer by FlashSolveController.latestAnswer.collectAsStateWithLifecycle()
                val latestLatencyMs by FlashSolveController.latestLatencyMs.collectAsStateWithLifecycle()
                val liveTimerMs by FlashSolveController.liveTimerMs.collectAsStateWithLifecycle()
                val errorMessage by FlashSolveController.errorMessage.collectAsStateWithLifecycle()
                val language by FlashSolveController.language.collectAsStateWithLifecycle()
                val customApiKey by FlashSolveController.customApiKey.collectAsStateWithLifecycle()
                val hasOverlayPermission by FlashSolveController.hasOverlayPermission.collectAsStateWithLifecycle()
                val hasMediaProjection by FlashSolveController.hasMediaProjection.collectAsStateWithLifecycle()
                val currentQuestionIdx by FlashSolveController.currentQuestionIndex.collectAsStateWithLifecycle()
                val solveHistory by FlashSolveController.solveHistory.collectAsStateWithLifecycle()

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                val overlaySettingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    FlashSolveController.refreshPermissions(this@MainActivity)
                    if (FlashSolveController.isServiceRunning.value) {
                        FlashSolveOverlayService.startOrUpdateService(this@MainActivity)
                    }
                }

                val mediaProjectionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        FlashSolveController.bindMediaProjection(
                            context = this@MainActivity,
                            resultCode = result.resultCode,
                            data = result.data!!
                        )
                        if (FlashSolveController.isServiceRunning.value) {
                            FlashSolveOverlayService.startOrUpdateService(this@MainActivity)
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val questions = FlashSolveLocalization.sampleQuestions
                val activeQuestion = questions[currentQuestionIdx.coerceIn(0, questions.lastIndex)]

                FlashSolveDashboardScreen(
                    isServiceRunning = isServiceRunning,
                    pillMode = pillMode,
                    solveStatus = solveStatus,
                    latestAnswer = latestAnswer,
                    latestLatencyMs = latestLatencyMs,
                    liveTimerMs = liveTimerMs,
                    errorMessage = errorMessage,
                    language = language,
                    customApiKey = customApiKey,
                    isBuildConfigKeyAvailable = FlashSolveController.isBuildConfigKeyConfigured(),
                    hasOverlayPermission = hasOverlayPermission,
                    hasMediaProjection = hasMediaProjection,
                    currentQuestion = activeQuestion,
                    solveHistory = solveHistory,
                    onMasterSwitchChange = { enabled ->
                        FlashSolveController.setMasterServiceEnabled(this@MainActivity, enabled)
                    },
                    onSetPillMode = { mode ->
                        FlashSolveController.setPillMode(mode)
                    },
                    onTriggerSolve = {
                        FlashSolveController.triggerSolve()
                    },
                    onToggleLanguage = {
                        FlashSolveController.toggleLanguage(this@MainActivity)
                    },
                    onSaveApiKey = { newKey ->
                        FlashSolveController.saveCustomApiKey(this@MainActivity, newKey)
                    },
                    onRequestOverlayPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            try {
                                overlaySettingsLauncher.launch(intent)
                            } catch (_: Exception) {
                            }
                        } else {
                            FlashSolveController.refreshPermissions(this@MainActivity)
                        }
                    },
                    onRequestMediaProjection = {
                        try {
                            val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            mediaProjectionLauncher.launch(mgr.createScreenCaptureIntent())
                        } catch (_: Exception) {
                        }
                    },
                    onNextPracticeQuestion = {
                        FlashSolveController.nextPracticeQuestion()
                    },
                    onClearHistory = {
                        FlashSolveController.clearHistory()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FlashSolveController.attachActivity(this)
        FlashSolveController.refreshPermissions(this)
    }

    override fun onDestroy() {
        FlashSolveController.detachActivity(this)
        super.onDestroy()
    }
}
