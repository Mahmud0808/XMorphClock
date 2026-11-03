package com.drdisagree.xmorphclock.xposed.data

import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceTile
import com.drdisagree.xmorphclock.domain.model.MediaAction
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import java.util.concurrent.CopyOnWriteArrayList

internal object MediaWatcher {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private var manager: MediaSessionManager? = null
    private var controller: MediaController? = null

    @Volatile
    var title: String? = null
        private set

    @Volatile
    var artist: String? = null
        private set

    @Volatile
    var playing = false
        private set

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = update()
        override fun onPlaybackStateChanged(state: PlaybackState?) = update()
        override fun onSessionDestroyed() = pick(manager?.let { sessions(it) }.orEmpty())
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers -> pick(controllers.orEmpty()) }

    fun start(context: Context) {
        if (manager != null) return
        val sessions = context.applicationContext.getSystemService(MediaSessionManager::class.java) ?: return
        manager = sessions
        runCatching {
            sessions.addOnActiveSessionsChangedListener(sessionsListener, null, mainHandler)
            pick(sessions(sessions))
        }.onFailure { HookEnv.log("Media sessions unavailable", it) }
    }

    fun tileState(tile: FaceTile): Int = when {
        controller == null -> FaceData.TILE_OFF
        tile != FaceTile.MEDIA -> FaceData.TILE_ON
        playing -> FaceData.TILE_ON
        else -> FaceData.TILE_ALT
    }

    fun perform(context: Context, action: MediaAction) {
        val controls = controller?.transportControls
        if (controls == null) {
            dispatch(context, keyOf(action))
            return
        }
        when (action) {
            MediaAction.TOGGLE -> if (controller?.playbackState?.state == PlaybackState.STATE_PLAYING) controls.pause() else controls.play()
            MediaAction.PREVIOUS -> controls.skipToPrevious()
            MediaAction.NEXT -> controls.skipToNext()
        }
    }

    private fun keyOf(action: MediaAction): Int = when (action) {
        MediaAction.TOGGLE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        MediaAction.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        MediaAction.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
    }

    private fun dispatch(context: Context, code: Int) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        val now = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    private fun sessions(manager: MediaSessionManager): List<MediaController> =
        runCatching { manager.getActiveSessions(null) }.getOrNull().orEmpty()

    private fun pick(controllers: List<MediaController>) {
        val best = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull { it.metadata != null }
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback, mainHandler)
        }
        update()
    }

    private fun update() {
        val current = controller
        val metadata = current?.metadata
        val nextTitle = metadata?.let { it.getString(MediaMetadata.METADATA_KEY_TITLE) ?: it.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) }?.takeIf { it.isNotBlank() }
        val nextArtist = metadata?.let { it.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: it.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) }?.takeIf { it.isNotBlank() }
        val nextPlaying = current?.playbackState?.state == PlaybackState.STATE_PLAYING
        if (nextTitle == title && nextArtist == artist && nextPlaying == playing) return
        title = nextTitle
        artist = nextArtist
        playing = nextPlaying
        mainHandler.post { listeners.forEach { it() } }
    }
}
