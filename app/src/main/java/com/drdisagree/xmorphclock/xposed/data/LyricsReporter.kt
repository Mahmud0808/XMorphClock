package com.drdisagree.xmorphclock.xposed.data

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.drdisagree.xmorphclock.BuildConfig
import com.drdisagree.xmorphclock.data.source.LyricsContract
import com.drdisagree.xmorphclock.domain.model.LyricsOrigin

@SuppressLint("StaticFieldLeak")
internal object LyricsReporter {

    private var context: Context? = null
    private var key: String? = null
    private var title: String? = null
    private var artist: String? = null
    private var album: String? = null
    private var durationMs = 0L
    private var origin = LyricsOrigin.NONE

    private val requestReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context, intent: Intent) {
            send()
        }
    }

    fun start(context: Context) {
        if (this.context != null) return
        val app = context.applicationContext
        this.context = app
        runCatching {
            ContextCompat.registerReceiver(app, requestReceiver, IntentFilter(LyricsContract.ACTION_REQUEST), ContextCompat.RECEIVER_EXPORTED)
        }
    }

    fun update(key: String?, title: String?, artist: String?, album: String?, durationMs: Long, origin: LyricsOrigin) {
        if (key == this.key && title == this.title && artist == this.artist && album == this.album && durationMs == this.durationMs && origin == this.origin) return
        this.key = key
        this.title = title
        this.artist = artist
        this.album = album
        this.durationMs = durationMs
        this.origin = origin
        send()
    }

    private fun send() {
        val app = context ?: return
        val intent = Intent(LyricsContract.ACTION_STATE).setPackage(BuildConfig.APPLICATION_ID)
        if (key != null && title != null) {
            intent.putExtra(LyricsContract.EXTRA_KEY, key)
                .putExtra(LyricsContract.EXTRA_TITLE, title)
                .putExtra(LyricsContract.EXTRA_ARTIST, artist)
                .putExtra(LyricsContract.EXTRA_ALBUM, album)
                .putExtra(LyricsContract.EXTRA_DURATION, durationMs)
                .putExtra(LyricsContract.EXTRA_ORIGIN, origin.name)
        }
        runCatching { app.sendBroadcast(intent) }
    }
}
