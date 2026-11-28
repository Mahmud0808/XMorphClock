package com.drdisagree.xmorphclock.xposed.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceTile
import com.drdisagree.xmorphclock.domain.model.MediaAction
import com.drdisagree.xmorphclock.domain.model.PlaybackClock
import com.drdisagree.xmorphclock.engine.FaceImages
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

internal object MediaWatcher {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private var manager: MediaSessionManager? = null
    private var controller: MediaController? = null
    private var controllers: List<MediaController> = emptyList()
    private val watchers = HashMap<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()
    private val startedAt = HashMap<MediaSession.Token, Long>()
    private var appContext: Context? = null
    private val artWorker = Executors.newSingleThreadExecutor()
    private var artKey: String? = null

    @Volatile
    var artVersion = 0
        private set

    @Volatile
    var title: String? = null
        private set

    @Volatile
    var artist: String? = null
        private set

    @Volatile
    var playing = false
        private set

    @Volatile
    var album: String? = null
        private set

    @Volatile
    var durationMs = 0L
        private set

    @Volatile
    var clock: PlaybackClock? = null
        private set

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = update()
        override fun onPlaybackStateChanged(state: PlaybackState?) = update()
        override fun onSessionDestroyed() = pick(manager?.let { sessions(it) }.orEmpty())
    }

    private fun watcherOf(token: MediaSession.Token) = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            val nowPlaying = state?.state == PlaybackState.STATE_PLAYING
            if (nowPlaying && token != controller?.sessionToken) {
                startedAt[token] = SystemClock.elapsedRealtime()
                pick(controllers)
            } else if (!nowPlaying && token == controller?.sessionToken) {
                pick(controllers)
            }
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers -> pick(controllers.orEmpty()) }

    fun start(context: Context) {
        if (manager != null) return
        appContext = context.applicationContext
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
        watch(controllers)
        val current = controller?.let { active -> controllers.firstOrNull { it.sessionToken == active.sessionToken } }
        val playing = controllers.filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val best = current?.takeIf { it in playing }
            ?: playing.maxByOrNull { startedAt[it.sessionToken] ?: 0L }
            ?: current
            ?: controllers.firstOrNull { it.metadata != null }
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback, mainHandler)
        }
        update()
    }

    private fun watch(next: List<MediaController>) {
        controllers = next
        val tokens = next.mapTo(HashSet()) { it.sessionToken }
        watchers.keys.filterNot { it in tokens }.forEach { token ->
            watchers.remove(token)?.let { (old, watcher) -> runCatching { old.unregisterCallback(watcher) } }
            startedAt.remove(token)
        }
        next.forEach { item ->
            if (item.sessionToken in watchers) return@forEach
            val watcher = watcherOf(item.sessionToken)
            watchers[item.sessionToken] = item to watcher
            runCatching { item.registerCallback(watcher, mainHandler) }
        }
    }

    private fun update() {
        val current = controller
        val metadata = current?.metadata
        updateArt(metadata)
        val nextTitle = metadata?.let { it.getString(MediaMetadata.METADATA_KEY_TITLE) ?: it.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) }?.takeIf { it.isNotBlank() }
        val nextArtist = metadata?.let { it.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: it.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) }?.takeIf { it.isNotBlank() }
        val state = current?.playbackState
        val nextPlaying = state?.state == PlaybackState.STATE_PLAYING
        val nextAlbum = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }
        val nextDuration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
        val now = SystemClock.elapsedRealtime()
        val nextClock = state?.takeIf { it.position >= 0 }?.let {
            PlaybackClock(it.position, it.lastPositionUpdateTime.takeIf { time -> time in 1..now } ?: now, it.playbackSpeed.takeIf { speed -> speed > 0f } ?: 1f, nextPlaying)
        }
        val clockSame = if (nextClock == null) clock == null else nextClock.matches(clock, now, CLOCK_TOLERANCE_MS)
        if (nextTitle == title && nextArtist == artist && nextPlaying == playing && nextAlbum == album && nextDuration == durationMs && clockSame) return
        title = nextTitle
        artist = nextArtist
        playing = nextPlaying
        album = nextAlbum
        durationMs = nextDuration
        clock = nextClock
        mainHandler.post { listeners.forEach { it() } }
    }

    private fun updateArt(metadata: MediaMetadata?) {
        val key = metadata?.let { data ->
            ART_TEXT_KEYS.joinToString(KEY_SEPARATOR) { data.getString(it).orEmpty() } + KEY_SEPARATOR +
                ART_KEYS.firstNotNullOfOrNull { data.getBitmap(it) }?.generationId
        }
        if (key == artKey) return
        artKey = key
        val context = appContext
        artWorker.execute {
            val art = metadata?.let { runCatching { artOf(context, it) }.getOrNull() }
            mainHandler.post {
                if (artKey != key) return@post
                FaceImages.mediaArt = art
                artVersion++
                listeners.forEach { it() }
            }
        }
    }

    private fun artOf(context: Context?, metadata: MediaMetadata): Bitmap? {
        ART_KEYS.firstNotNullOfOrNull { metadata.getBitmap(it) }?.let { return scaled(it) }
        val uri = ART_URI_KEYS.firstNotNullOfOrNull { metadata.getString(it)?.takeIf(String::isNotBlank) } ?: return null
        val resolver = context?.contentResolver ?: return null
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri.toUri())) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = max(info.size.width, info.size.height)
            if (longest > ART_SIZE) {
                val ratio = ART_SIZE.toFloat() / longest
                decoder.setTargetSize(max(1, (info.size.width * ratio).roundToInt()), max(1, (info.size.height * ratio).roundToInt()))
            }
        }
    }

    private fun scaled(bitmap: Bitmap): Bitmap {
        val source = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        val longest = max(source.width, source.height)
        if (longest <= ART_SIZE) return source
        val ratio = ART_SIZE.toFloat() / longest
        return source.scale(max(1, (source.width * ratio).roundToInt()), max(1, (source.height * ratio).roundToInt()))
    }

    private const val ART_SIZE = 256
    private const val CLOCK_TOLERANCE_MS = 300L
    private const val KEY_SEPARATOR = "|"
    private val ART_KEYS = listOf(MediaMetadata.METADATA_KEY_ALBUM_ART, MediaMetadata.METADATA_KEY_ART, MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    private val ART_URI_KEYS = listOf(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, MediaMetadata.METADATA_KEY_ART_URI, MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
    private val ART_TEXT_KEYS = listOf(MediaMetadata.METADATA_KEY_TITLE, MediaMetadata.METADATA_KEY_ARTIST, MediaMetadata.METADATA_KEY_ALBUM)
}
