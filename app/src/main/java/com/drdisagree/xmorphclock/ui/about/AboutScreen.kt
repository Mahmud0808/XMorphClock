package com.drdisagree.xmorphclock.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.BuildConfig
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.PageScaffold
import com.drdisagree.xmorphclock.ui.components.glass
import com.drdisagree.xmorphclock.ui.components.openUrl
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkFaint
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenLicenses: () -> Unit) {
    PageScaffold(stringResource(R.string.action_about), onBack) {
        item(key = "identity") { IdentityCard() }
        item(key = "links") { LinksCard(onOpenLicenses) }
    }
}

@Composable
private fun IdentityCard() {
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .glass(LocalBackdrop.current, TileShape)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(60.dp).clip(IconShape), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(90.dp))
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(90.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = OnInk)
            Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall, color = OnInkMuted)
        }
    }
}

@Composable
private fun LinksCard(onOpenLicenses: () -> Unit) {
    val context = LocalContext.current
    val developerUrl = stringResource(R.string.developer_url)
    val sourceUrl = stringResource(R.string.source_url)
    val supportUrl = stringResource(R.string.support_url)
    Column(
        Modifier
            .fillMaxWidth()
            .glass(LocalBackdrop.current, TileShape)
            .clip(TileShape)
    ) {
        LinkRow(stringResource(R.string.about_developer), stringResource(R.string.about_developer_name)) { openUrl(context, developerUrl) }
        LinkRow(stringResource(R.string.about_source)) { openUrl(context, sourceUrl) }
        LinkRow(stringResource(R.string.about_support)) { openUrl(context, supportUrl) }
        LinkRow(stringResource(R.string.licenses), onClick = onOpenLicenses)
    }
}

@Composable
private fun LinkRow(title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = OnInk, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = OnInkMuted)
            Spacer(Modifier.width(8.dp))
        }
        AppIcon(R.drawable.ic_chevron, tint = OnInkFaint, size = 16.dp)
    }
}

private val TileShape = RoundedCornerShape(28.dp)
private val IconShape = RoundedCornerShape(16.dp)
