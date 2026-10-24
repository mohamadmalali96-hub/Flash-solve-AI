package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.FlashSolveLocalization
import com.example.model.LocalizedStrings
import com.example.model.PillMode
import com.example.model.PracticeQuestion
import com.example.model.SolveHistoryItem
import com.example.model.SolveStatus
import com.example.ui.components.FloatingPillOverlay
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.StandbyAmber
import com.example.ui.theme.TextSecondaryDark

@Composable
fun FlashSolveDashboardScreen(
    isServiceRunning: Boolean,
    pillMode: PillMode,
    solveStatus: SolveStatus,
    latestAnswer: String,
    latestLatencyMs: Long?,
    liveTimerMs: Long,
    errorMessage: String?,
    language: AppLanguage,
    customApiKey: String,
    isBuildConfigKeyAvailable: Boolean,
    hasOverlayPermission: Boolean,
    hasMediaProjection: Boolean,
    currentQuestion: PracticeQuestion,
    solveHistory: List<SolveHistoryItem>,
    onMasterSwitchChange: (Boolean) -> Unit,
    onSetPillMode: (PillMode) -> Unit,
    onTriggerSolve: () -> Unit,
    onToggleLanguage: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestMediaProjection: () -> Unit,
    onNextPracticeQuestion: () -> Unit,
    onClearHistory: () -> Unit
) {
    val strings = remember(language) { FlashSolveLocalization.forLanguage(language) }
    var isCapturingHidePill by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = CyberNavy,
            topBar = {
                TopHeaderBar(
                    strings = strings,
                    isServiceRunning = isServiceRunning,
                    onToggleLanguage = onToggleLanguage
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("dashboard_lazy_column"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 110.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Master Start/Stop Hero Card + Pill Mode Control
                    item {
                        MasterControlHeroCard(
                            strings = strings,
                            isServiceRunning = isServiceRunning,
                            pillMode = pillMode,
                            latestAnswer = latestAnswer,
                            latestLatencyMs = latestLatencyMs,
                            errorMessage = errorMessage,
                            onMasterSwitchChange = onMasterSwitchChange,
                            onSetPillMode = onSetPillMode
                        )
                    }

                    // 2. Live Screen Test Arena (Practice Question Card for Immediate Screen Capture)
                    item {
                        LivePracticeArenaCard(
                            strings = strings,
                            language = language,
                            question = currentQuestion,
                            solveStatus = solveStatus,
                            latestAnswer = latestAnswer,
                            latestLatencyMs = latestLatencyMs,
                            onNextQuestion = onNextPracticeQuestion,
                            onSolveNow = onTriggerSolve
                        )
                    }

                    // 3. Secure Gemini API Key Configuration Card
                    item {
                        ApiKeySecurityCard(
                            strings = strings,
                            savedApiKey = customApiKey,
                            isBuildConfigKeyAvailable = isBuildConfigKeyAvailable,
                            onSaveApiKey = onSaveApiKey
                        )
                    }

                    // 4. Overlay (WindowManager) & Screen Capture Permissions Card
                    item {
                        OverlayAndCaptureEngineCard(
                            strings = strings,
                            hasOverlayPermission = hasOverlayPermission,
                            hasMediaProjection = hasMediaProjection,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onRequestMediaProjection = onRequestMediaProjection
                        )
                    }

                    // 5. Recent Solves History Header & Items
                    item {
                        HistoryHeaderRow(
                            strings = strings,
                            hasItems = solveHistory.isNotEmpty(),
                            onClearHistory = onClearHistory
                        )
                    }

                    if (solveHistory.isEmpty()) {
                        item {
                            EmptyHistoryCard(strings = strings)
                        }
                    } else {
                        items(solveHistory, key = { it.id }) { item ->
                            SolveHistoryRowCard(item = item)
                        }
                    }
                }

                // Floating Pill rendered inside the app window whenever service is active
                // and system-wide overlay permission is not yet active (or for immediate in-app interaction)
                if (isServiceRunning && !hasOverlayPermission && !isCapturingHidePill) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                            .align(Alignment.TopCenter),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingPillOverlay(
                            pillMode = pillMode,
                            solveStatus = solveStatus,
                            latestAnswer = latestAnswer,
                            latestLatencyMs = latestLatencyMs,
                            liveTimerMs = liveTimerMs,
                            strings = strings,
                            onTogglePillMode = {
                                onSetPillMode(
                                    if (pillMode == PillMode.ACTIVE) PillMode.STANDBY else PillMode.ACTIVE
                                )
                            },
                            onSolveClick = onTriggerSolve
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    strings: LocalizedStrings,
    isServiceRunning: Boolean,
    onToggleLanguage: () -> Unit
) {
    Surface(
        color = CyberNavy.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(ElectricCyan.copy(alpha = 0.25f), NeonEmerald.copy(alpha = 0.25f))
                            )
                        )
                        .border(1.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = strings.appTitle,
                        tint = ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.appTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isServiceRunning) NeonEmerald else TextSecondaryDark)
                        )
                    }
                    Text(
                        text = strings.appSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            }

            // Bilingual Language Switcher (Arabic / English)
            OutlinedButton(
                onClick = onToggleLanguage,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("language_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = strings.languageToggleLabel,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.languageToggleLabel,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MasterControlHeroCard(
    strings: LocalizedStrings,
    isServiceRunning: Boolean,
    pillMode: PillMode,
    latestAnswer: String,
    latestLatencyMs: Long?,
    errorMessage: String?,
    onMasterSwitchChange: (Boolean) -> Unit,
    onSetPillMode: (PillMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .border(
                width = 1.5.dp,
                color = if (isServiceRunning) ElectricCyan.copy(alpha = 0.6f)
                else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("master_control_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Generated Hero Banner Backdrop
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1792866667435),
                contentDescription = strings.appTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                CyberNavy.copy(alpha = 0.35f),
                                CyberCard.copy(alpha = 0.96f),
                                CyberCard
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(42.dp))

                // Master Start / Stop Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.masterSwitchTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isServiceRunning) {
                                strings.masterSwitchActiveSub
                            } else {
                                strings.masterSwitchStoppedSub
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isServiceRunning) NeonEmerald else TextSecondaryDark
                        )
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = onMasterSwitchChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberNavy,
                            checkedTrackColor = NeonEmerald,
                            uncheckedThumbColor = TextSecondaryDark,
                            uncheckedTrackColor = CyberNavy
                        ),
                        modifier = Modifier.testTag("master_start_stop_switch")
                    )
                }

                // Prominent Start / Stop Action Button
                Button(
                    onClick = { onMasterSwitchChange(!isServiceRunning) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceRunning) AlertCoral else ElectricCyan,
                        contentColor = if (isServiceRunning) Color.White else CyberNavy
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .testTag("master_start_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceRunning) strings.stopServiceBtn else strings.startServiceBtn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Pill Mode Selector (Active vs Standby)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.pillStateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark,
                        modifier = Modifier.weight(1f)
                    )

                    // Active State Pill Button
                    PillModeChip(
                        label = strings.pillActiveMode,
                        selected = pillMode == PillMode.ACTIVE,
                        activeColor = NeonEmerald,
                        onClick = { onSetPillMode(PillMode.ACTIVE) },
                        testTag = "pill_mode_active_button"
                    )

                    // Standby State Pill Button
                    PillModeChip(
                        label = strings.pillStandbyMode,
                        selected = pillMode == PillMode.STANDBY,
                        activeColor = StandbyAmber,
                        onClick = { onSetPillMode(PillMode.STANDBY) },
                        testTag = "pill_mode_standby_button"
                    )
                }

                // Speed Telemetry Strip (Immediate Output [B] + Latency ms)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberNavy.copy(alpha = 0.7f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.lastAnswerLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = latestAnswer,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = NeonEmerald,
                            modifier = Modifier.testTag("telemetry_latest_answer")
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.avgLatencyLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = latestLatencyMs?.let { "${it} ms" } ?: "— ms",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ElectricCyan,
                                modifier = Modifier.testTag("telemetry_latency_ms")
                            )
                        }
                        Text(
                            text = strings.targetSlaLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonEmerald.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        )
                    }
                }

                AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AlertCoral.copy(alpha = 0.15f))
                            .border(1.dp, AlertCoral.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AlertCoral
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillModeChip(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) activeColor.copy(alpha = 0.2f) else CyberNavy.copy(alpha = 0.6f)
            )
            .border(
                width = 1.dp,
                color = if (selected) activeColor else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (activeColor == StandbyAmber) {
                    Icons.Default.NightlightRound
                } else {
                    Icons.Default.Bolt
                },
                contentDescription = null,
                tint = if (selected) activeColor else TextSecondaryDark,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (selected) activeColor else TextSecondaryDark
            )
        }
    }
}

@Composable
private fun LivePracticeArenaCard(
    strings: LocalizedStrings,
    language: AppLanguage,
    question: PracticeQuestion,
    solveStatus: SolveStatus,
    latestAnswer: String,
    latestLatencyMs: Long?,
    onNextQuestion: () -> Unit,
    onSolveNow: () -> Unit
) {
    val isSolving = solveStatus == SolveStatus.CAPTURING || solveStatus == SolveStatus.SOLVING
    val category = if (language == AppLanguage.AR) question.categoryAr else question.categoryEn
    val prompt = if (language == AppLanguage.AR) question.questionAr else question.questionEn
    val options = if (language == AppLanguage.AR) question.optionsAr else question.optionsEn

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("practice_arena_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.practiceArenaTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.practiceArenaSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onNextQuestion,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("next_question_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = strings.nextQuestionBtn,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.nextQuestionBtn,
                        color = ElectricCyan,
                        fontSize = 12.sp
                    )
                }
            }

            // Screen Target Question Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberNavy)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonEmerald
                )
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                options.forEach { option ->
                    val isMatched = latestAnswer.length >= 3 && option.startsWith(latestAnswer)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isMatched) NeonEmerald.copy(alpha = 0.16f)
                                else CyberCardElevated.copy(alpha = 0.7f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isMatched) NeonEmerald else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (isMatched) FontWeight.Bold else FontWeight.Normal,
                            color = if (isMatched) NeonEmerald else Color(0xFFE2E8F0)
                        )
                        if (isMatched && latestLatencyMs != null) {
                            Text(
                                text = "✓ ${latestLatencyMs}ms",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = NeonEmerald
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onSolveNow,
                enabled = !isSolving,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = CyberNavy
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("arena_solve_button")
            ) {
                Text(
                    text = if (isSolving) strings.pillSolvingText else strings.triggerSolveNowBtn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ApiKeySecurityCard(
    strings: LocalizedStrings,
    savedApiKey: String,
    isBuildConfigKeyAvailable: Boolean,
    onSaveApiKey: (String) -> Unit
) {
    var keyDraft by rememberSaveable(savedApiKey) { mutableStateOf(savedApiKey) }
    var isKeyVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("api_key_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = ElectricCyan
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.apiKeyCardTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.apiKeyCardSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            }

            // Key Status Indicator
            val statusLabel = when {
                savedApiKey.isNotBlank() -> strings.apiKeySavedBadge
                isBuildConfigKeyAvailable -> strings.apiKeyUsingBuildConfig
                else -> strings.apiKeyMissingWarning
            }
            val isKeyReady = savedApiKey.isNotBlank() || isBuildConfigKeyAvailable

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isKeyReady) Icons.Default.CheckCircle else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isKeyReady) NeonEmerald else StandbyAmber,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isKeyReady) NeonEmerald else StandbyAmber,
                    fontSize = 12.sp
                )
            }

            OutlinedTextField(
                value = keyDraft,
                onValueChange = { keyDraft = it },
                placeholder = {
                    Text(
                        text = strings.apiKeyPlaceholder,
                        color = TextSecondaryDark
                    )
                },
                singleLine = true,
                visualTransformation = if (isKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onSaveApiKey(keyDraft)
                        focusManager.clearFocus()
                    }
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { isKeyVisible = !isKeyVisible },
                        modifier = Modifier.testTag("toggle_api_key_visibility")
                    ) {
                        Icon(
                            imageVector = if (isKeyVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = "Toggle key visibility",
                            tint = TextSecondaryDark
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = CyberNavy,
                    unfocusedContainerColor = CyberNavy
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        onSaveApiKey(keyDraft)
                        focusManager.clearFocus()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonEmerald,
                        contentColor = CyberNavy
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("save_api_key_button")
                ) {
                    Text(
                        text = strings.apiKeySaveBtn,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mandatory Security Notice per android-secret-management skill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberNavy.copy(alpha = 0.6f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = StandbyAmber,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings.securityNotice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun OverlayAndCaptureEngineCard(
    strings: LocalizedStrings,
    hasOverlayPermission: Boolean,
    hasMediaProjection: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestMediaProjection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.permissionsCardTitle,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // 1. WindowManager Overlay Permission Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = if (hasOverlayPermission) NeonEmerald else ElectricCyan
                    )
                    Column {
                        Text(
                            text = strings.overlayPermTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = strings.overlayPermGranted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasOverlayPermission) NeonEmerald else TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }
                }

                if (!hasOverlayPermission) {
                    OutlinedButton(
                        onClick = onRequestOverlayPermission,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("grant_overlay_permission_button")
                    ) {
                        Text(
                            text = strings.overlayPermGrantBtn,
                            color = ElectricCyan,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 2. Screen Capture (MediaProjection + Live Window) Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenshotMonitor,
                        contentDescription = null,
                        tint = if (hasMediaProjection) NeonEmerald else ElectricCyan
                    )
                    Column {
                        Text(
                            text = strings.capturePermTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (hasMediaProjection) {
                                strings.capturePermGranted
                            } else {
                                strings.capturePermFallback
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasMediaProjection) NeonEmerald else TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }
                }

                if (!hasMediaProjection) {
                    OutlinedButton(
                        onClick = onRequestMediaProjection,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("grant_media_projection_button")
                    ) {
                        Text(
                            text = strings.capturePermGrantBtn,
                            color = ElectricCyan,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeaderRow(
    strings: LocalizedStrings,
    hasItems: Boolean,
    onClearHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = strings.historyTitle,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        if (hasItems) {
            OutlinedButton(
                onClick = onClearHistory,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("clear_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = strings.clearHistoryBtn,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = strings.clearHistoryBtn,
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(strings: LocalizedStrings) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = strings.historyEmpty,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
private fun SolveHistoryRowCard(item: SolveHistoryItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (item.isSuccess) NeonEmerald.copy(alpha = 0.18f)
                            else AlertCoral.copy(alpha = 0.18f)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.answerText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (item.isSuccess) NeonEmerald else AlertCoral
                    )
                }

                Column {
                    Text(
                        text = item.sourceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.timestampFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondaryDark
                    )
                }
            }

            Text(
                text = "⚡ ${item.latencyMs} ms",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (item.latencyMs <= 1500L) ElectricCyan else StandbyAmber
            )
        }
    }
}
