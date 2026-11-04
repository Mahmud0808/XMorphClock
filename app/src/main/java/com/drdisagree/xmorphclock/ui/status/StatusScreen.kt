package com.drdisagree.xmorphclock.ui.status

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.ClockAttachState
import com.drdisagree.xmorphclock.domain.model.HookSignal
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.PageScaffold
import com.drdisagree.xmorphclock.ui.components.StatusGlyph
import com.drdisagree.xmorphclock.ui.components.StatusTone
import com.drdisagree.xmorphclock.ui.components.glass
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidButton
import com.drdisagree.xmorphclock.ui.components.rememberSpin
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.Hairline
import com.drdisagree.xmorphclock.ui.theme.Negative
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkFaint
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import com.drdisagree.xmorphclock.ui.theme.Warning
import com.drdisagree.xmorphclock.ui.theme.onTint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val CardShape = RoundedCornerShape(28.dp)
private val TileShape = RoundedCornerShape(20.dp)
private val CopyTint = Color(0xFFFF8D28)
private val RING_SIZE = 112.dp
private val RING_STROKE = 6.dp
private const val RING_GAP = 14f

@Composable
fun StatusScreen(viewModel: StatusViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val steps = statusSteps(state)
    val overall = when {
        steps.all { it.state == StepState.OK } -> StepState.OK
        steps.any { it.state == StepState.FAILED } -> StepState.FAILED
        else -> StepState.WAITING
    }
    val logTitle = stringResource(R.string.report_section_logcat)
    val currentState by rememberUpdatedState(state)
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(REPORT_MIME)) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.onSaveReport(uri.toString(), StatusReportFormatter.format(context, currentState), logTitle)
    }

    PageScaffold(stringResource(R.string.tab_status), onBack) {
        item(key = "hero") { Hero(steps, overall) }
        item(key = "chain") { Chain(steps) }
        item(key = "errors") {
            val errors = state.report?.errors.orEmpty()
            AnimatedVisibility(
                visible = errors.isNotEmpty(),
                enter = fadeIn(tween(260)) + expandVertically(tween(360, easing = Emphasized)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(300, easing = Emphasized))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .glass(LocalBackdrop.current, CardShape)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.section_errors), style = MaterialTheme.typography.titleSmall, color = Negative)
                    errors.forEach { error ->
                        Text(error, style = MaterialTheme.typography.bodySmall, color = OnInkMuted)
                    }
                }
            }
        }
        item(key = "details") {
            val report = state.report
            AnimatedVisibility(
                visible = report != null,
                enter = fadeIn(tween(260)) + expandVertically(tween(360, easing = Emphasized)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(300, easing = Emphasized))
            ) {
                if (report != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DetailTile(stringResource(R.string.status_systemui_version), report.systemUiVersion, Modifier.weight(1f))
                            DetailTile(stringResource(R.string.status_build), report.buildId, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DetailTile(
                                stringResource(R.string.status_api),
                                state.framework?.apiVersion?.toString() ?: stringResource(R.string.not_detected),
                                Modifier.weight(1f)
                            )
                            DetailTile(
                                stringResource(R.string.status_updated),
                                DateUtils.getRelativeTimeSpanString(report.timestamp).toString(),
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        item(key = "depth") {
            DepthModelRow(state.depthModel)
        }
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    R.drawable.ic_refresh,
                    stringResource(
                        when (state.refresh) {
                            RefreshState.IDLE -> R.string.action_refresh
                            RefreshState.CHECKING -> R.string.action_checking
                            RefreshState.UPDATED -> R.string.action_updated
                            RefreshState.NO_RESPONSE -> R.string.action_no_response
                        }
                    ),
                    if (state.refresh == RefreshState.NO_RESPONSE) Warning else Accent,
                    viewModel::refresh,
                    Modifier.weight(1f),
                    spinning = state.refresh == RefreshState.CHECKING
                )
                ActionButton(
                    R.drawable.ic_save,
                    stringResource(
                        when (state.save) {
                            SaveState.IDLE -> R.string.action_save_report
                            SaveState.SAVING -> R.string.action_saving
                            SaveState.SAVED -> R.string.action_saved
                            SaveState.FAILED -> R.string.action_save_failed
                        }
                    ),
                    if (state.save == SaveState.FAILED) Warning else CopyTint,
                    { saveLauncher.launch(reportFileName()) },
                    Modifier.weight(1f),
                    spinning = state.save == SaveState.SAVING
                )
                ActionButton(
                    R.drawable.ic_restart,
                    stringResource(if (state.restarting) R.string.restarting else R.string.action_restart),
                    Negative,
                    viewModel::onRestartSystemUi,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun statusSteps(state: StatusUiState): List<StatusStep> {
    val framework = state.framework
    val report = state.report
    val steps = mutableListOf<StatusStep>()
    var blocked = false
    fun add(title: String, value: String, ok: Boolean, soft: Boolean = false) {
        val stepState = when {
            blocked -> StepState.PENDING
            ok -> StepState.OK
            soft -> StepState.WAITING
            else -> StepState.FAILED
        }
        steps += StatusStep(title, value, stepState)
        if (!ok) blocked = true
    }
    add(
        stringResource(R.string.status_framework),
        framework?.let { stringResource(R.string.framework_label, it.name, it.version.substringBefore(' ')) }
            ?: stringResource(R.string.not_detected),
        framework != null
    )
    add(
        stringResource(R.string.status_scope),
        stringResource(if (state.isScoped) R.string.status_scope_ok else R.string.status_scope_missing),
        state.isScoped
    )
    add(
        stringResource(R.string.status_step_systemui),
        report?.systemUiVersion?.takeIf { it.isNotEmpty() } ?: stringResource(R.string.status_no_report_summary),
        report != null
    )
    add(
        stringResource(R.string.status_adapter),
        report?.adapter ?: stringResource(R.string.unsupported),
        report?.adapter != null
    )
    val signals = report?.signals.orEmpty().ifEmpty { listOf(SIGNAL_SIZE, SIGNAL_PROGRESS).map { HookSignal(it, null) } }
    signals.forEach { signal ->
        add(signalLabel(signal.name), signal.source ?: stringResource(R.string.missing), signal.source != null)
    }
    val attach = report?.attachState(state.installedAt) ?: ClockAttachState.WAITING
    add(
        stringResource(R.string.status_clock_face),
        when (attach) {
            ClockAttachState.ATTACHED -> report?.clockFace.orEmpty()
            ClockAttachState.OFF -> stringResource(R.string.status_clock_off)
            ClockAttachState.STALE -> stringResource(R.string.status_restart_to_load)
            ClockAttachState.FAILED -> stringResource(R.string.status_attach_failed)
            ClockAttachState.WAITING, ClockAttachState.UNSUPPORTED -> stringResource(R.string.status_lock_to_attach)
        },
        attach == ClockAttachState.ATTACHED,
        soft = attach != ClockAttachState.FAILED
    )
    return steps
}

@Composable
private fun Hero(steps: List<StatusStep>, overall: StepState) {
    val passed = steps.count { it.state == StepState.OK }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) { sweep.animateTo(1f, tween(1_100, easing = Emphasized)) }
    val colors = steps.map { step -> animateColorAsState(stateColor(step.state), tween(500), label = "segment").value }
    val track = Hairline
    Row(
        Modifier
            .fillMaxWidth()
            .glass(LocalBackdrop.current, CardShape)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            Modifier
                .size(RING_SIZE)
                .drawBehind {
                    val stroke = RING_STROKE.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val count = colors.size.coerceAtLeast(1)
                    val slot = 360f / count
                    val gap = RING_GAP
                    colors.forEachIndexed { index, color ->
                        val start = -90f + index * slot + gap / 2f
                        val length = (slot - gap) * sweep.value
                        drawArc(track, start, slot - gap, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(color, start, length, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = passed,
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInVertically(tween(320)) { it / 2 }) togetherWith
                            (fadeOut(tween(140)) + slideOutVertically(tween(320)) { -it / 2 })
                },
                label = "passed"
            ) { value ->
                Text(
                    stringResource(R.string.status_ring_count, value, steps.size),
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnInk
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AnimatedContent(
                targetState = overall,
                transitionSpec = { fadeIn(tween(300, delayMillis = 80)) togetherWith fadeOut(tween(160)) using SizeTransform(clip = false) },
                label = "heroTitle"
            ) { target ->
                Text(
                    stringResource(
                        when (target) {
                            StepState.OK -> R.string.status_hero_ok
                            StepState.FAILED -> R.string.status_hero_failed
                            else -> R.string.status_hero_waiting
                        }
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = OnInk
                )
            }
            Text(
                pluralStringResource(R.plurals.status_ring_summary, steps.size, passed, steps.size),
                style = MaterialTheme.typography.bodySmall,
                color = OnInkMuted
            )
        }
    }
}

@Composable
private fun Chain(steps: List<StatusStep>) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(LocalBackdrop.current, CardShape)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        steps.forEachIndexed { index, step ->
            val previous = steps.getOrNull(index - 1)
            val next = steps.getOrNull(index + 1)
            ChainRow(
                step = step,
                index = index,
                topLinked = previous?.state == StepState.OK && step.state == StepState.OK,
                bottomLinked = next != null && step.state == StepState.OK && next.state == StepState.OK,
                first = index == 0,
                last = next == null
            )
        }
    }
}

@Composable
private fun ChainRow(step: StatusStep, index: Int, topLinked: Boolean, bottomLinked: Boolean, first: Boolean, last: Boolean) {
    val hairline = Hairline
    val linked = OnInkMuted
    val top by animateColorAsState(if (topLinked) linked else hairline, tween(420), label = "top")
    val bottom by animateColorAsState(if (bottomLinked) linked else hairline, tween(420), label = "bottom")
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        appear.animateTo(1f, tween(420, easing = Emphasized))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 16.dp.toPx()
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier
                .width(18.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2f
                    val y = size.height / 2f
                    val gap = NODE_GAP.toPx()
                    val stroke = 1.5.dp.toPx()
                    if (!first) drawLine(top, Offset(x, 0f), Offset(x, y - gap), stroke, StrokeCap.Round)
                    if (!last) drawLine(bottom, Offset(x, y + gap), Offset(x, size.height), stroke, StrokeCap.Round)
                },
            contentAlignment = Alignment.Center
        ) {
            StatusGlyph(toneOf(step.state), size = 16.dp)
        }
        Column(Modifier.padding(vertical = 10.dp)) {
            Text(step.title, style = MaterialTheme.typography.titleMedium, color = OnInk)
            AnimatedContent(
                targetState = step.value,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(140)) },
                label = "stepValue"
            ) { value ->
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (step.state == StepState.PENDING) OnInkFaint else OnInkMuted
                )
            }
        }
    }
}

@Composable
private fun DetailTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .glass(LocalBackdrop.current, TileShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnInkMuted, maxLines = 1)
        Text(value.ifEmpty { "-" }, style = MaterialTheme.typography.titleSmall, color = OnInk, maxLines = 2)
    }
}

@Composable
private fun ActionButton(
    icon: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    spinning: Boolean = false
) {
    val animatedTint by animateColorAsState(tint, tween(300), label = "actionTint")
    val angle by rememberSpin(spinning, SPIN_MS)
    LiquidButton(onClick, LocalBackdrop.current, modifier, tint = animatedTint, height = 76.dp, horizontalPadding = 16.dp) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppIcon(
                icon,
                tint = onTint(tint),
                size = 20.dp,
                modifier = Modifier.graphicsLayer { rotationZ = angle }
            )
            AnimatedContent(
                targetState = label,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "actionLabel"
            ) { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.labelMedium,
                    color = onTint(tint),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

private fun toneOf(state: StepState): StatusTone = when (state) {
    StepState.OK -> StatusTone.OK
    StepState.FAILED -> StatusTone.ERROR
    StepState.WAITING -> StatusTone.BUSY
    StepState.PENDING -> StatusTone.IDLE
}

@Composable
private fun stateColor(state: StepState): Color = when (state) {
    StepState.OK -> Accent
    StepState.FAILED -> Negative
    StepState.WAITING -> Warning
    StepState.PENDING -> Color.Gray.copy(alpha = 0.5f)
}

@Composable
private fun signalLabel(name: String): String = when (name) {
    SIGNAL_SIZE -> stringResource(R.string.status_signal_size)
    SIGNAL_PROGRESS -> stringResource(R.string.status_signal_progress)
    else -> name
}

private fun reportFileName(): String =
    REPORT_PREFIX + SimpleDateFormat(REPORT_STAMP, Locale.US).format(Date()) + REPORT_EXTENSION

private const val SPIN_MS = 800
private val NODE_GAP = 12.dp
private const val REPORT_MIME = "text/plain"
private const val REPORT_PREFIX = "xmorph-report-"
private const val REPORT_STAMP = "yyyyMMdd-HHmmss"
private const val REPORT_EXTENSION = ".txt"
private const val SIGNAL_SIZE = "CLOCK_SIZE"
private const val SIGNAL_PROGRESS = "KEYGUARD_PROGRESS"
