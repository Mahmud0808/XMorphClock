package com.drdisagree.xmorphclock.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.drdisagree.xmorphclock.R
import kotlin.math.roundToInt

@Composable
fun PositionSection(state: EditorUiState, id: String, viewModel: EditorViewModel) {
    val placement = viewModel.placementOf(state, id) ?: return
    val target = state.bounds?.takeIf { it.id == id } ?: return
    val measured = viewModel.placementIn(target.face, state.scope, id) ?: return
    val box = target.bounds
    val x = placement.x
    val y = placement.y
    val rangeX = spanOf(measured.x - box.left, measured.x + 1f - box.right)
    val rangeY = spanOf(measured.y - box.top, measured.y + target.maxBottom - box.bottom)
    EditorBlock(stringResource(R.string.label_position_x), percentOf(x, rangeX)) {
        RangeSlider(
            x.coerceIn(rangeX),
            rangeX,
            { viewModel.setPositionX(id, it) },
            start = { StepButton(R.drawable.ic_minus, stringResource(R.string.action_move_left)) { viewModel.setPositionX(id, (x - STEP).coerceIn(rangeX)) } },
            end = { StepButton(R.drawable.ic_plus, stringResource(R.string.action_move_right)) { viewModel.setPositionX(id, (x + STEP).coerceIn(rangeX)) } }
        )
    }
    EditorBlock(stringResource(R.string.label_position_y), percentOf(y, rangeY)) {
        RangeSlider(
            y.coerceIn(rangeY),
            rangeY,
            { viewModel.setPositionY(id, it) },
            start = { StepButton(R.drawable.ic_minus, stringResource(R.string.action_move_up)) { viewModel.setPositionY(id, (y - STEP).coerceIn(rangeY)) } },
            end = { StepButton(R.drawable.ic_plus, stringResource(R.string.action_move_down)) { viewModel.setPositionY(id, (y + STEP).coerceIn(rangeY)) } }
        )
    }
}

private fun spanOf(a: Float, b: Float): ClosedFloatingPointRange<Float> {
    val low = snap(minOf(a, b))
    val high = snap(maxOf(a, b))
    return if (high - low < MIN_SPAN) low..(low + MIN_SPAN) else low..high
}

private fun snap(value: Float): Float = (value * RANGE_QUANTUM).roundToInt() / RANGE_QUANTUM

private fun percentOf(value: Float, range: ClosedFloatingPointRange<Float>): String {
    val span = range.endInclusive - range.start
    return "${((value - range.start) / span * PERCENT).roundToInt().coerceIn(0, PERCENT.toInt())}%"
}

private const val STEP = 0.002f
private const val PERCENT = 100f
private const val MIN_SPAN = 0.001f
private const val RANGE_QUANTUM = 1_000f
