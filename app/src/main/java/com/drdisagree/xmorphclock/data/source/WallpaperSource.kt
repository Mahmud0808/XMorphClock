package com.drdisagree.xmorphclock.data.source

import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext

class WallpaperSource(private val context: Context) {

    @Volatile
    var visibleView: String? = null
        private set

    private val cache = File(context.cacheDir, CACHE_FILE)

    @Volatile
    var lastLive = false
        private set
    private val lockTransform = File(context.filesDir, LOCK_TRANSFORM_FILE)

    private val frameFile = File(context.filesDir, FRAME_FILE)

    private val incomingFrame = File(context.filesDir, WallpaperContract.FRAME_FILE)

    suspend fun readFrame(): Bitmap? = withContext(Dispatchers.IO) {
        if (!incomingFrame.isFile) return@withContext null
        val bitmap = BitmapFactory.decodeFile(incomingFrame.path) ?: return@withContext null
        runCatching { incomingFrame.copyTo(frameFile, overwrite = true) }
        bitmap
    }

    suspend fun lastFrame(): Bitmap? = withContext(Dispatchers.IO) {
        if (frameFile.isFile) BitmapFactory.decodeFile(frameFile.path) else null
    }

    var frameScale: Float
        get() = context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE).getFloat(FRAME_SCALE, 1f)
        set(value) = context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE).edit { putFloat(FRAME_SCALE, value) }

    fun requestFrame() {
        runCatching {
            context.sendBroadcast(Intent(WallpaperContract.ACTION_REQUEST_FRAME).setPackage(ReportContract.SYSTEMUI_PACKAGE))
        }
    }

    fun clearFrame() {
        frameFile.delete()
    }

    suspend fun cached(): Bitmap? = withContext(Dispatchers.IO) {
        if (cache.isFile) BitmapFactory.decodeFile(cache.path) else null
    }

    @OptIn(FlowPreview::class)
    fun changes(): Flow<Unit> = callbackFlow {
        val manager = WallpaperManager.getInstance(context)
        val listener = WallpaperManager.OnColorsChangedListener { _, _ -> trySend(Unit) }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        awaitClose { manager.removeOnColorsChangedListener(listener) }
    }.debounce(CHANGE_DEBOUNCE_MS)

    suspend fun isLive(): Boolean {
        if (liveInfo(WallpaperManager.getInstance(context)) != null) return true
        return liveComponent(RootShell.read(INFO_COMMAND) { it.readBytes().decodeToString() }.orEmpty()) != null
    }

    private fun liveComponent(info: String): String? {
        val tag = Regex("<kwp\\b[^>]*>").find(info)?.value ?: Regex("<wp\\b[^>]*>").find(info)?.value ?: return null
        val component = Regex("\\bcomponent=\"([^\"]*)\"").find(tag)?.groupValues?.get(1).orEmpty()
        return component.takeIf { it.isNotEmpty() && !it.endsWith(IMAGE_WALLPAPER) }
    }

    private suspend fun liveImage(): Bitmap? {
        val info = RootShell.read(INFO_COMMAND) { it.readBytes().decodeToString() }.orEmpty()
        val component = liveComponent(info) ?: return null
        val pkg = component.substringBefore('/')
        val names = IMAGE_NAME.findAll(info).map { it.value }.distinct()
        for (name in names) {
            for (dir in LIVE_DIRS) {
                val bytes = RootShell.read("cat '${dir.format(pkg)}/$name' 2>/dev/null") { it.readBytes() }?.takeIf { it.isNotEmpty() }
                bytes?.let { decodeCropped(it, null, null, 1f, null) }?.let { return it }
            }
        }
        return null
    }

    private fun liveInfo(manager: WallpaperManager): WallpaperInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return manager.wallpaperInfo
        return manager.getWallpaperInfo(WallpaperManager.FLAG_LOCK) ?: manager.wallpaperInfo
    }

    suspend fun lockscreenBitmap(): Bitmap? {
        val info = RootShell.read(INFO_COMMAND) { it.readBytes().decodeToString() }.orEmpty()
        val path = if (Regex("<kwp\\b").containsMatchIn(info)) LOCK_CROPPED else SYSTEM_CROPPED
        val bytes = RootShell.read("cat $path 2>/dev/null") { it.readBytes() }?.takeIf { it.isNotEmpty() } ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    suspend fun refresh(): Bitmap? {
        val live = isLive()
        if (live) visibleView = null
        lastLive = live
        val bitmap = (if (live) liveImage() else rootWallpaper()) ?: systemFallback() ?: return null
        withContext(Dispatchers.IO) {
            cache.outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, CACHE_QUALITY, it) }
        }
        return bitmap
    }

    suspend fun measureView(): String? = withContext(Dispatchers.IO) {
        val measured = measuredTransform() ?: return@withContext null
        val info = RootShell.read(INFO_COMMAND) { it.readBytes().decodeToString() }.orEmpty()
        val tag = Regex("<kwp\\b[^>]*>").find(info)?.value ?: Regex("<wp\\b[^>]*>").find(info)?.value ?: return@withContext null
        saveLockTransform(idOf(tag), measured)
        val base = rectOf(tag, TOTAL_CROP) ?: rectOf(tag, CROP) ?: return@withContext null
        val sampleSize = sampleOf(tag) ?: 1f
        withScreen { screenWidth, screenHeight ->
            viewOf(visibleRegion(base, sampleSize, measured, screenWidth, screenHeight, base), base)
        }
    }

    private fun viewOf(region: Rect, base: Rect): String = listOf(
        (region.left - base.left).toFloat() / base.width(),
        (region.top - base.top).toFloat() / base.height(),
        region.width().toFloat() / base.width(),
        region.height().toFloat() / base.height()
    ).joinToString(WallpaperContract.SEPARATOR)

    private suspend fun rootWallpaper(): Bitmap? {
        val info = RootShell.read(INFO_COMMAND) { it.readBytes().decodeToString() }.orEmpty()
        val lockTag = Regex("<kwp\\b[^>]*>").find(info)?.value
        val tag = lockTag ?: Regex("<wp\\b[^>]*>").find(info)?.value
        val measured = tag?.let { savedLockTransform(idOf(it)) }
        val original = if (lockTag != null) LOCK_ORIGINAL else SYSTEM_ORIGINAL
        val crop = tag?.let { rectOf(it, CROP) }
        val total = tag?.let { rectOf(it, TOTAL_CROP) }
        val sampleSize = tag?.let(::sampleOf) ?: 1f
        val bytes = RootShell.read("cat $original 2>/dev/null") { it.readBytes() }?.takeIf { it.isNotEmpty() }
        if (bytes != null) {
            decodeCropped(bytes, crop, total ?: crop, sampleSize, measured)?.let { return it }
        }
        val fallback = if (lockTag != null) LOCK_CROPPED else SYSTEM_CROPPED
        val cropped = RootShell.read("cat $fallback 2>/dev/null") { it.readBytes() }?.takeIf { it.isNotEmpty() }
        return cropped?.let { decodeCropped(it, null, null, 1f, measured) }
    }

    private fun rectOf(tag: String, prefix: String): Rect? {
        fun attr(name: String): Int? = Regex("\\b$prefix$name=\"(-?\\d+)\"").find(tag)?.groupValues?.get(1)?.toIntOrNull()
        val left = attr("Left") ?: return null
        val top = attr("Top") ?: return null
        val right = attr("Right") ?: return null
        val bottom = attr("Bottom") ?: return null
        return Rect(left, top, right, bottom).takeIf { it.width() > 0 && it.height() > 0 }
    }

    private fun sampleOf(tag: String): Float? =
        Regex("\\bsampleSize=\"([0-9.]+)\"").find(tag)?.groupValues?.get(1)?.toFloatOrNull()?.takeIf { it > 0f }

    private fun decodeCropped(
        bytes: ByteArray,
        crop: Rect?,
        buffer: Rect?,
        sampleSize: Float,
        measured: WallpaperTransform?
    ): Bitmap? = withScreen { screenWidth, screenHeight ->
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withScreen null
        val full = Rect(0, 0, bounds.outWidth, bounds.outHeight)
        val base = buffer?.takeIf { it.right <= full.right && it.bottom <= full.bottom } ?: full
        val measuredRegion = measured?.let { visibleRegion(base, sampleSize, it, screenWidth, screenHeight, full) }
            ?.takeIf { plausible(it, base, screenWidth, screenHeight) }
        val target = if (measuredRegion != null) {
            visibleView = viewOf(measuredRegion, base)
            measuredRegion
        } else {
            visibleView = null
            val region = crop?.takeIf { it.right <= full.right && it.bottom <= full.bottom } ?: full
            zoomCrop(screenCrop(region, screenWidth.toFloat() / screenHeight), wallpaperScale())
        }
        var sample = 1
        while (target.width() / (sample * 2) >= screenWidth && target.height() / (sample * 2) >= screenHeight) sample *= 2
        val decoder = BitmapRegionDecoder.newInstance(bytes, 0, bytes.size)
        try {
            decoder.decodeRegion(target, BitmapFactory.Options().apply { inSampleSize = sample })
        } finally {
            decoder.recycle()
        }
    }

    private fun visibleRegion(
        base: Rect,
        sampleSize: Float,
        transform: WallpaperTransform,
        screenWidth: Int,
        screenHeight: Int,
        full: Rect
    ): Rect {
        if (transform.scaleX <= 0f || transform.scaleY <= 0f) return base
        val left = base.left + (-transform.translateX / transform.scaleX) * sampleSize
        val top = base.top + (-transform.translateY / transform.scaleY) * sampleSize
        val width = screenWidth / transform.scaleX * sampleSize
        val height = screenHeight / transform.scaleY * sampleSize
        val result = Rect(left.roundToInt(), top.roundToInt(), (left + width).roundToInt(), (top + height).roundToInt())
        return if (result.intersect(full) && result.width() > 0 && result.height() > 0) result else base
    }

    private fun zoomCrop(region: Rect, scale: Float): Rect {
        if (scale <= 1f) return region
        val width = (region.width() / scale).roundToInt()
        val height = (region.height() / scale).roundToInt()
        val left = region.left + (region.width() - width) / 2
        val top = region.top + (region.height() - height) / 2
        return Rect(left, top, left + width, top + height)
    }

    private fun screenCrop(region: Rect, aspect: Float): Rect {
        val regionAspect = region.width().toFloat() / region.height()
        return if (regionAspect > aspect) {
            val width = (region.height() * aspect).roundToInt()
            val left = region.left + (region.width() - width) / 2
            Rect(left, region.top, left + width, region.bottom)
        } else {
            val height = (region.width() / aspect).roundToInt()
            val top = region.top + (region.height() - height) / 2
            Rect(region.left, top, region.right, top + height)
        }
    }

    private suspend fun measuredTransform(): WallpaperTransform? {
        val dump = RootShell.read(TRANSFORM_COMMAND) { it.readBytes().decodeToString() }.orEmpty()
        val blocks = dump.split(BLOCK_SEPARATOR).filter { it.contains(WALLPAPER_LAYER) && TRANSFORM.containsMatchIn(it) }
        val layer = blocks.firstOrNull { !it.contains(INVISIBLE) } ?: blocks.firstOrNull() ?: return null
        val match = TRANSFORM.find(layer) ?: return null
        val (sx, sy, tx, ty) = match.destructured
        return WallpaperTransform(sx.toFloat(), sy.toFloat(), tx.toFloat(), ty.toFloat())
    }

    private fun saveLockTransform(id: String, transform: WallpaperTransform) {
        runCatching {
            lockTransform.writeText(
                (listOf(id) + listOf(transform.scaleX, transform.scaleY, transform.translateX, transform.translateY).map { it.toString() })
                    .joinToString(WallpaperContract.SEPARATOR)
            )
        }
    }

    private fun savedLockTransform(id: String): WallpaperTransform? = runCatching {
        if (!lockTransform.isFile) return null
        val parts = lockTransform.readText().split(WallpaperContract.SEPARATOR)
        if (parts.size != TRANSFORM_PARTS + 1 || parts.first() != id) return null
        val values = parts.drop(1).mapNotNull { it.toFloatOrNull() }
        if (values.size != TRANSFORM_PARTS) null else WallpaperTransform(values[0], values[1], values[2], values[3])
    }.getOrNull()

    private fun idOf(tag: String): String = Regex("\\bid=\"(-?\\d+)\"").find(tag)?.groupValues?.get(1).orEmpty()

    private fun plausible(region: Rect, base: Rect, screenWidth: Int, screenHeight: Int): Boolean {
        val expected = minOf(base.width().toFloat(), base.height() * screenWidth.toFloat() / screenHeight)
        if (expected <= 0f) return false
        val ratio = region.width() / expected
        return ratio in MIN_VIEW_RATIO..MAX_VIEW_RATIO
    }

    private fun wallpaperScale(): Float {
        val resources = Resources.getSystem()
        val id = resources.getIdentifier(MAX_SCALE, DIMEN, ANDROID)
        if (id == 0) return 1f
        return try {
            resources.getFloat(id).takeIf { it in 1f..MAX_SANE_SCALE } ?: 1f
        } catch (_: Resources.NotFoundException) {
            1f
        }
    }

    private inline fun <T> withScreen(block: (Int, Int) -> T): T {
        val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        return block(max(1, bounds.width()), max(1, bounds.height()))
    }

    private suspend fun systemFallback(): Bitmap? = withContext(Dispatchers.IO) {
        val manager = WallpaperManager.getInstance(context)
        val live = liveInfo(manager)
        val drawable: Drawable? = try {
            live?.loadThumbnail(context.packageManager) ?: manager.getBuiltInDrawable(WallpaperManager.FLAG_LOCK)
        } catch (_: Exception) {
            null
        }
        drawable?.takeIf { it.intrinsicWidth > 0 && it.intrinsicHeight > 0 }?.toBitmap()
    }
}

private const val CACHE_FILE = "lock_wallpaper_v3.webp"
private const val CROP = "crop"
private const val MIN_VIEW_RATIO = 0.7f
private const val MAX_VIEW_RATIO = 1.15f
private const val IMAGE_WALLPAPER = ".ImageWallpaper"
private val IMAGE_NAME = Regex("[A-Za-z0-9._-]+\\.(?:jpe?g|png|webp)")
private val LIVE_DIRS = listOf("/data/user_de/0/%s/files", "/data/data/%s/files")
private const val TOTAL_CROP = "totalCrop"
private const val CHANGE_DEBOUNCE_MS = 400L
private const val MAX_SCALE = "config_wallpaperMaxScale"
private const val DIMEN = "dimen"
private const val ANDROID = "android"
private const val MAX_SANE_SCALE = 2f
private const val TRANSFORM_COMMAND = "dumpsys SurfaceFlinger 2>/dev/null | grep -A3 'Layer \\[.*WallpaperService-'"
private const val BLOCK_SEPARATOR = "Layer ["
private const val WALLPAPER_LAYER = "WallpaperService-"
private const val INVISIBLE = "invisible reason"
private const val LOCK_TRANSFORM_FILE = "lock_wallpaper_transform"
private const val TRANSFORM_PARTS = 4
private val TRANSFORM = Regex("scale x=(-?[0-9.]+) y=(-?[0-9.]+)\\s+tx=(-?[0-9.]+) ty=(-?[0-9.]+)")
private const val CACHE_QUALITY = 92
private const val USER_DIR = "/data/system/users/0"
private const val INFO_COMMAND = "abx2xml $USER_DIR/wallpaper_info.xml - 2>/dev/null || cat $USER_DIR/wallpaper_info.xml"
private const val LOCK_ORIGINAL = "$USER_DIR/wallpaper_lock_orig"
private const val SYSTEM_ORIGINAL = "$USER_DIR/wallpaper_orig"
private const val LOCK_CROPPED = "$USER_DIR/wallpaper_lock"
private const val SYSTEM_CROPPED = "$USER_DIR/wallpaper"
private const val FRAME_FILE = "lockscreen_frame.png"
private const val FRAME_SCALE = "frame_scale"
