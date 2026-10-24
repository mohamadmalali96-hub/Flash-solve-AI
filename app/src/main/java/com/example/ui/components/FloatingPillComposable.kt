package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LocalizedStrings
import com.example.model.PillMode
import com.example.model.SolveStatus
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.StandbyAmber
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Draggable Floating 'Pill' UI component supporting Active & Standby states
 * and a prominent 'Solve' trigger button.
 */
@Composable
fun FloatingPillOverlay(
    pillMode: PillMode,
    solveStatus: SolveStatus,
    latestAnswer: String,
    latestLatencyMs: Long?,
    liveTimerMs: Long,
    strings: LocalizedStrings,
    onTogglePillMode: () -> Unit,
    onSolveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val isStandby = pillMode == PillMode.STANDBY
    val isSolving = solveStatus == SolveStatus.CAPTURING || solveStatus == SolveStatus.SOLVING
    val isError = solveStatus == SolveStatus.ERROR || latestAnswer == "[KEY?]" || latestAnswer == "[ERR]"

    val infiniteTransition = rememberInfiniteTransition(label = "pillPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSolving -> ElectricCyan.copy(alpha = pulseAlpha)
            isStandby -> StandbyAmber.copy(alpha = 0.55f)
            isError -> AlertCoral.copy(alpha = 0.85f)
            else -> ElectricCyan.copy(alpha = 0.75f)
        },
        label = "borderColor"
    )

    val dotColor by animateColorAsState(
        targetValue = when {
            isSolving -> ElectricCyan
            isStandby -> StandbyAmber
            isError -> AlertCoral
            else -> NeonEmerald
        },
        label = "dotColor"
    )

    Surface(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .alpha(if (isStandby) 0.82f else 1.0f)
            .shadow(
                elevation = if (isStandby) 8.dp else 18.dp,
                shape = CircleShape,
                ambientColor = ElectricCyan,
                spotColor = ElectricCyan
            )
            .border(width = 1.5.dp, color = borderColor, shape = CircleShape)
            .testTag("floating_pill_container"),
        shape = CircleShape,
        color = Color(0xF20B1324)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Active / Standby state toggle chip inside the pill
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onTogglePillMode)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("floating_pill_state_toggle"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isSolving) dotColor.copy(alpha = pulseAlpha) else dotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isStandby) strings.pillStandbyMode else strings.pillActiveMode,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isStandby) StandbyAmber else Color(0xFFE2E8F0),
                    fontWeight = FontWeight.Bold
                )
            }

            // Immediate Answer Readout Badge & Latency (Shown in ACTIVE mode)
            AnimatedVisibility(visible = !isStandby) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isError) AlertCoral.copy(alpha = 0.16f)
                                else NeonEmerald.copy(alpha = 0.16f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isError) AlertCoral.copy(alpha = 0.5f)
                                else NeonEmerald.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("floating_pill_answer_badge")
                    ) {
                        Text(
                            text = if (isSolving) "[···]" else latestAnswer,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isError) AlertCoral else NeonEmerald
                        )
                    }

                    val timingText = when {
                        isSolving -> String.format(Locale.US, "%.1fs", liveTimerMs / 1000.0)
                        latestLatencyMs != null -> "${latestLatencyMs}ms"
                        else -> ""
                    }
                    if (timingText.isNotEmpty()) {
                        Text(
                            text = timingText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = if (latestLatencyMs != null && latestLatencyMs <= 1500L) {
                                ElectricCyan
                            } else {
                                Color(0xFF94A3B8)
                            },
                            modifier = Modifier.testTag("floating_pill_latency_text")
                        )
                    }
                }
            }

            // Prominent 'Solve' Trigger Button inside the Pill
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 76.dp, minHeight = 38.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (isSolving) {
                                listOf(ElectricCyan.copy(alpha = 0.45f), NeonEmerald.copy(alpha = 0.45f))
                            } else {
                                listOf(ElectricCyan, NeonEmerald)
                            }
                        )
                    )
                    .clickable(enabled = !isSolving, onClick = onSolveClick)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("floating_pill_solve_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = strings.pillSolveBtn,
                        tint = CyberNavy,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isSolving) strings.pillSolvingText else strings.pillSolveBtn,
                        color = CyberNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Mode switch icon button (Active <-> Standby)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable(onClick = onTogglePillMode)
                    .testTag("floating_pill_mode_icon_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isStandby) {
                        Icons.Default.NightlightRound
                    } else {
                        Icons.Default.RadioButtonChecked
                    },
                    contentDescription = strings.pillStateLabel,
                    tint = if (isStandby) StandbyAmber else ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
