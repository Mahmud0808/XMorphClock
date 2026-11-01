package com.drdisagree.xmorphclock.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.OrderPosition

@Composable
internal fun OrderBlock(position: OrderPosition, onMove: (Int) -> Unit) {
    val last = position.count - 1
    EditorBlock(stringResource(R.string.label_order), stringResource(R.string.order_position, position.index + 1, position.count)) {
        TileRow {
            item { TextTile(stringResource(R.string.order_back), position.index == 0, { onMove(-last) }) }
            item { TextTile(stringResource(R.string.order_backward), false, { onMove(-1) }) }
            item { TextTile(stringResource(R.string.order_forward), false, { onMove(1) }) }
            item { TextTile(stringResource(R.string.order_front), position.index == last, { onMove(last) }) }
        }
    }
}
