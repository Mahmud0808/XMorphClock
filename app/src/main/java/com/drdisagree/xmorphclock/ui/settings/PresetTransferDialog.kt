package com.drdisagree.xmorphclock.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.ui.components.GlassDialog

@Composable
fun PresetTransferDialog(transfer: PresetTransfer?, onDismiss: () -> Unit) {
    var last by remember { mutableStateOf(transfer) }
    LaunchedEffect(transfer) { if (transfer != null) last = transfer }
    val shown = transfer ?: last ?: return
    val title = stringResource(
        when (shown.status) {
            PresetTransferStatus.EXPORTED -> R.string.transfer_exported_title
            PresetTransferStatus.IMPORTED -> R.string.transfer_imported_title
            else -> R.string.transfer_failed_title
        }
    )
    val message = when (shown.status) {
        PresetTransferStatus.EXPORTED -> stringResource(R.string.transfer_exported_message)
        PresetTransferStatus.IMPORTED -> pluralStringResource(R.plurals.transfer_imported_message, shown.count, shown.count)
        PresetTransferStatus.SAVE_FAILED -> stringResource(R.string.transfer_save_failed)
        PresetTransferStatus.READ_FAILED -> stringResource(R.string.transfer_read_failed)
        PresetTransferStatus.NOT_PRESET_FILE -> stringResource(R.string.transfer_not_preset_file)
        PresetTransferStatus.UNSUPPORTED_VERSION -> stringResource(R.string.transfer_unsupported)
        PresetTransferStatus.DAMAGED -> stringResource(R.string.transfer_damaged)
    }
    GlassDialog(
        visible = transfer != null,
        title = title,
        message = message,
        confirmLabel = stringResource(R.string.action_got_it),
        onDismiss = onDismiss
    )
}
