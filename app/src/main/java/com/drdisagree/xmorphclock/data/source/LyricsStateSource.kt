package com.drdisagree.xmorphclock.data.source

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.drdisagree.xmorphclock.domain.model.LyricsOrigin
import com.drdisagree.xmorphclock.domain.model.NowPlayingLyrics
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class LyricsStateSource(private val context: Context) {

    val state: Flow<NowPlayingLyrics?> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.action == LyricsContract.ACTION_STATE) trySend(parse(intent))
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(LyricsContract.ACTION_STATE),
            LyricsContract.SENDER_PERMISSION,
            null,
            ContextCompat.RECEIVER_EXPORTED
        )
        request()
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }.conflate()

    fun request() {
        runCatching {
            context.sendBroadcast(Intent(LyricsContract.ACTION_REQUEST).setPackage(ReportContract.SYSTEMUI_PACKAGE))
        }
    }

    private fun parse(intent: Intent): NowPlayingLyrics? {
        val key = intent.getStringExtra(LyricsContract.EXTRA_KEY) ?: return null
        val title = intent.getStringExtra(LyricsContract.EXTRA_TITLE) ?: return null
        val origin = intent.getStringExtra(LyricsContract.EXTRA_ORIGIN)
            ?.let { name -> LyricsOrigin.entries.firstOrNull { it.name == name } }
            ?: LyricsOrigin.NONE
        return NowPlayingLyrics(
            key = key,
            title = title,
            artist = intent.getStringExtra(LyricsContract.EXTRA_ARTIST),
            album = intent.getStringExtra(LyricsContract.EXTRA_ALBUM),
            durationMs = intent.getLongExtra(LyricsContract.EXTRA_DURATION, 0L),
            origin = origin
        )
    }
}
