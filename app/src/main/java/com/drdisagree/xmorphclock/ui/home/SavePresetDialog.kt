package com.drdisagree.xmorphclock.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.ui.components.DialogButton
import com.drdisagree.xmorphclock.ui.components.GlassDialogFrame
import com.drdisagree.xmorphclock.ui.components.InputField
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted

@Composable
fun SavePresetDialog(initialName: String?, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var last by remember { mutableStateOf(initialName) }
    LaunchedEffect(initialName) { if (initialName != null) last = initialName }
    val shown = initialName ?: last ?: return
    var name by remember(shown) { mutableStateOf(shown) }
    GlassDialogFrame(initialName != null, onDismiss) {
        Text(
            stringResource(R.string.save_preset_title),
            style = MaterialTheme.typography.titleLarge,
            color = OnInk,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 8.dp)
        )
        Text(
            stringResource(R.string.save_preset_message),
            style = MaterialTheme.typography.bodyMedium,
            color = OnInkMuted,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
        )
        InputField(
            value = shown,
            hint = stringResource(R.string.save_preset_hint),
            onValueChange = { name = it },
            key = shown,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Row(
            Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DialogButton(stringResource(R.string.action_cancel), primary = false, onClick = onDismiss, modifier = Modifier.weight(1f))
            DialogButton(
                stringResource(R.string.action_save),
                primary = true,
                onClick = {
                    onSave(name.trim().ifEmpty { shown })
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
