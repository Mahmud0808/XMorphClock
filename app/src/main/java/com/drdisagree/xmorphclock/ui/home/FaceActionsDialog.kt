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
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FacePreset
import com.drdisagree.xmorphclock.ui.components.DialogButton
import com.drdisagree.xmorphclock.ui.components.GlassDialogFrame
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted

@Composable
fun FaceActionsDialog(
    preset: FacePreset?,
    onDuplicate: (FacePreset) -> Unit,
    onDelete: (FacePreset) -> Unit,
    onDismiss: () -> Unit
) {
    var last by remember { mutableStateOf(preset) }
    LaunchedEffect(preset) { if (preset != null) last = preset }
    val shown = preset ?: last ?: return
    val mine = FaceKind.of(shown.style)?.isMine(shown.style) == true
    GlassDialogFrame(preset != null, onDismiss) {
        Text(
            shown.face.name,
            style = MaterialTheme.typography.titleLarge,
            color = OnInk,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 8.dp)
        )
        Text(
            stringResource(if (mine) R.string.face_actions_mine else R.string.face_actions_preset),
            style = MaterialTheme.typography.bodyMedium,
            color = OnInkMuted,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
        )
        Row(
            Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (mine) {
                DialogButton(
                    stringResource(R.string.action_delete),
                    primary = false,
                    onClick = {
                        onDelete(shown)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            DialogButton(
                stringResource(R.string.action_duplicate),
                primary = true,
                onClick = {
                    onDuplicate(shown)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
