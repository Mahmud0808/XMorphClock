package com.drdisagree.xmorphclock.xposed.clock

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.core.graphics.createBitmap
import com.drdisagree.xmorphclock.BuildConfig
import com.drdisagree.xmorphclock.data.source.WallpaperContract
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.callMethod
import com.drdisagree.xmorphclock.xposed.hook.fieldValue
import com.drdisagree.xmorphclock.xposed.hook.findMethod
import com.drdisagree.xmorphclock.xposed.hook.hookAfter
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.zip.CRC32
import kotlin.math.max
import kotlin.math.roundToInt

internal object WallpaperCapture {

    private const val ENGINE = "com.android.systemui.wallpapers.ImageWallpaper\$CanvasEngine"
    private val LEGACY_ENGINES = setOf("com.android.systemui.ImageWallpaper\$GLEngine", "com.android.systemui.wallpapers.ImageWallpaper\$GLEngine")
    private const val DRAW_FRAME = "drawFrameOnCanvas"
    private const val OFFSETS_CHANGED = "doOffsetsChanged"
    private const val DIM_AMOUNT = "mWallpaperDimAmount"
    private const val DRAW = "drawFrame"
    private const val DRAW_INTERNAL = "drawFrameInternal"
    private const val FLAGS = "getWallpaperFlags"
    private const val DISPLAY_CONTEXT = "getDisplayContext"
    private const val SURFACE_HOLDER = "getSurfaceHolder"
    private const val RELAYOUT = "mRelayoutResult"
    private const val WIN_FRAMES = "mWinFrames"
    private const val IS_PREVIEW = "isPreview"
    private const val FRAMES = "frames"
    private const val FRAME = "frame"
    private const val X_OFFSET = "mPendingXOffset"
    private const val Y_OFFSET = "mPendingYOffset"
    private const val BITMAP_CROPS = "getBitmapCrops"
    private const val CENTER = 0.5f
    private const val ROUND = 0.5f

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val scaleListeners = CopyOnWriteArrayList<() -> Unit>()
    private const val BASE_ENGINE = "android.service.wallpaper.WallpaperService\$Engine"
    private const val SET_ZOOM = "setZoom"
    private const val GET_ZOOM = "getZoom"
    private const val ZOOMS_OUT = "shouldZoomOutWallpaper"
    private const val MAX_SCALE = "config_wallpaperMaxScale"
    private const val DIMEN = "dimen"
    private const val ANDROID = "android"
    private const val MAX_SANE_SCALE = 2f

    @Volatile
    var scale = 1f
        private set

    @Volatile
    var shiftX = 0f
        private set

    @Volatile
    var shiftY = 0f
        private set

    private var baked: RectF? = null
    private var bakedWidth = 0
    private var bakedHeight = 0
    private var bakedCrop: Rect? = null
    private var bakedDisplay: Rect? = null
    private var installed = false
    private var canvasEngine = false

    @Volatile
    private var backdrop = false

    @Volatile
    var frame: Bitmap? = null
        private set

    @Volatile
    private var key: String? = null

    @Volatile
    private var lastScale = 1f

    private var engineRef: WeakReference<Any>? = null
    private var receiving = false

    private val requestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WallpaperContract.ACTION_REQUEST_FRAME) executor.execute { runCatching { resend(context) } }
        }
    }

    fun execute(task: () -> Unit) {
        executor.execute { runCatching(task).onFailure { HookEnv.log("Wallpaper capture failed", it) } }
    }

    fun listen(context: Context) {
        if (!canvasEngine) StaticWallpaperFrames.start(context, ::processStatic)
        if (receiving) return
        receiving = true
        context.applicationContext.registerReceiver(requestReceiver, IntentFilter(WallpaperContract.ACTION_REQUEST_FRAME), Context.RECEIVER_EXPORTED)
        executor.execute { runCatching { resend(context.applicationContext) } }
    }

    private fun resend(context: Context) {
        val current = frame
        val currentKey = key
        if (current != null && currentKey != null) {
            publish(context, current, currentKey, lastScale)
            return
        }
        if (!canvasEngine) {
            StaticWallpaperFrames.capture(context, ::processStatic)
            return
        }
        val engine = engineRef?.get() ?: return
        val redraw = engine.javaClass.findMethod(DRAW) { it.parameterCount == 0 } ?: engine.javaClass.findMethod(DRAW_INTERNAL) { it.parameterCount == 0 } ?: return
        mainHandler.post { runCatching { redraw.invoke(engine) } }
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun addScaleListener(listener: () -> Unit) {
        scaleListeners += listener
    }

    fun install() {
        if (installed) return
        installed = true
        HookEnv.findClass(BASE_ENGINE)?.findMethod(OFFSETS_CHANGED) { it.parameterCount == 1 }?.hookAfter { chain, _ ->
            val engine = chain.thisObject ?: return@hookAfter
            if (!isTracked(engine)) return@hookAfter
            adoptLegacy(engine)
            val display = bakedDisplay ?: return@hookAfter
            updateShift(placementOf(engine, display, bakedWidth, bakedHeight, bakedCrop))
        }
        HookEnv.findClass(BASE_ENGINE)?.findMethod(SET_ZOOM) { it.parameterCount == 1 }?.hookAfter { chain, _ ->
            val engine = chain.thisObject ?: return@hookAfter
            if (!isTracked(engine)) return@hookAfter
            adoptLegacy(engine)
            updateScale(engine)
        }
        val engineClass = HookEnv.findClass(ENGINE) ?: return HookEnv.log("Canvas wallpaper engine not found, using static frames")
        canvasEngine = true
        val draw = engineClass.findMethod(DRAW_FRAME) { it.parameterCount == 1 && it.parameterTypes[0] == Bitmap::class.java }
            ?: return HookEnv.log("Wallpaper draw not found")
        draw.hookAfter { chain, _ ->
            val engine = chain.thisObject ?: return@hookAfter
            if (!isLock(engine)) return@hookAfter
            engineRef = WeakReference(engine)
            updateScale(engine)
            val source = (chain.args.getOrNull(0) as? Bitmap)?.takeUnless { it.isRecycled } ?: return@hookAfter
            val copy = runCatching { source.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull() ?: return@hookAfter
            executor.execute { runCatching { process(engine, copy) }.onFailure { HookEnv.log("Wallpaper capture failed", it) } }
        }
    }

    private fun adoptLegacy(engine: Any) {
        if (canvasEngine || engineRef?.get() === engine) return
        engineRef = WeakReference(engine)
        val context = engine.callMethod(DISPLAY_CONTEXT) as? Context ?: return
        if (!backdrop) StaticWallpaperFrames.capture(context.applicationContext, ::processStatic)
    }

    private fun updateDim(engine: Any) {
        val dim = (engine.fieldValue(DIM_AMOUNT) as? Float) ?: return
        if (dim == ScrimTracker.wallpaperDim) return
        ScrimTracker.wallpaperDim = dim
        mainHandler.post { scaleListeners.forEach { it() } }
    }

    private fun updateScale(engine: Any) {
        if (backdrop) return
        updateDim(engine)
        val zoom = ((engine.callMethod(GET_ZOOM) as? Float) ?: 0f).coerceIn(0f, 1f)
        val zoomsOut = (engine.callMethod(ZOOMS_OUT) as? Boolean) ?: true
        val next = if (zoomsOut) 1f + (maxScale() - 1f) * (1f - zoom) else 1f
        if (next == scale) return
        scale = next
        mainHandler.post { scaleListeners.forEach { it() } }
    }

    private fun restScale(engine: Any): Float =
        if ((engine.callMethod(ZOOMS_OUT) as? Boolean) != false) maxScale() else 1f

    private fun maxScale(): Float {
        val resources = Resources.getSystem()
        val id = resources.getIdentifier(MAX_SCALE, DIMEN, ANDROID)
        if (id == 0) return 1f
        return runCatching { resources.getFloat(id) }.getOrNull()?.takeIf { it in 1f..MAX_SANE_SCALE } ?: 1f
    }

    private fun isLock(engine: Any): Boolean {
        val flags = engine.callMethod(FLAGS) as? Int ?: return true
        return (flags and WallpaperManager.FLAG_LOCK) != 0
    }

    private fun isTracked(engine: Any): Boolean = when (engine.javaClass.name) {
        ENGINE -> isLock(engine)
        in LEGACY_ENGINES -> !canvasEngine && engine.callMethod(IS_PREVIEW) != true
        else -> false
    }

    private fun processStatic(context: Context, wallpaper: StaticWallpaper) {
        backdrop = wallpaper.lock
        val engine = engineRef?.get()
        if (!wallpaper.lock && engine != null) return process(engine, wallpaper.bitmap)
        val bitmap = wallpaper.bitmap
        val display = context.getSystemService(WindowManager::class.java)?.maximumWindowMetrics?.bounds ?: return
        if (display.isEmpty) return
        val next = sampledKey(bitmap) + (if (wallpaper.lock) "@lock@" else "@static@") + display.flattenToString()
        if (next == key && frame != null) return
        val rendered = if (wallpaper.lock) render(bitmap, backdropPlacement(bitmap, display), display) else centerCrop(bitmap, display)
        if (wallpaper.lock && (scale != 1f || ScrimTracker.wallpaperDim != 0f)) {
            scale = 1f
            ScrimTracker.wallpaperDim = 0f
            mainHandler.post { scaleListeners.forEach { it() } }
        }
        key = next
        baked = null
        bakedDisplay = null
        shiftX = 0f
        shiftY = 0f
        frame = rendered
        lastScale = 1f
        mainHandler.post { listeners.forEach { it() } }
        publish(context, rendered, next, lastScale)
    }

    private fun process(engine: Any, bitmap: Bitmap) {
        val context = engine.callMethod(DISPLAY_CONTEXT) as? Context ?: return
        val display = context.getSystemService(WindowManager::class.java)?.currentWindowMetrics?.bounds ?: return
        if (display.isEmpty) return
        val crop = cropOf(context, engine, display)
        val placement = placementOf(engine, display, bitmap.width, bitmap.height, crop)
        val next = sampledKey(bitmap) + "@" + crop?.flattenToString() + "@" + display.flattenToString()
        if (next == key && frame != null) {
            updateShift(placement)
            return
        }
        val rendered = if (placement != null) render(bitmap, placement, display) else centerCrop(bitmap, display)
        key = next
        baked = placement
        bakedWidth = bitmap.width
        bakedHeight = bitmap.height
        bakedCrop = crop
        bakedDisplay = display
        shiftX = 0f
        shiftY = 0f
        frame = rendered
        lastScale = restScale(engine)
        mainHandler.post { listeners.forEach { it() } }
        publish(context, rendered, next, lastScale)
    }

    private fun publish(context: Context, rendered: Bitmap, frameKey: String, scale: Float) {
        val uri = Uri.parse(WallpaperContract.FRAME_URI)
        if (context.packageManager.resolveContentProvider(uri.authority ?: return, 0) == null) return
        val stream = context.contentResolver.openOutputStream(uri, WRITE_TRUNCATE) ?: return
        stream.use { rendered.compress(Bitmap.CompressFormat.PNG, FULL_QUALITY, it) }
        context.sendBroadcast(
            Intent(WallpaperContract.ACTION_FRAME)
                .setPackage(BuildConfig.APPLICATION_ID)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES or Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra(WallpaperContract.EXTRA_KEY, frameKey)
                .putExtra(WallpaperContract.EXTRA_SCALE, scale)
        )
    }

    private fun cropOf(context: Context, engine: Any, display: Rect): Rect? {
        val which = if ((engine.callMethod(FLAGS) as? Int ?: 0) and WallpaperManager.FLAG_SYSTEM != 0) WallpaperManager.FLAG_SYSTEM else WallpaperManager.FLAG_LOCK
        val manager = WallpaperManager.getInstance(context)
        val method = manager.javaClass.findMethod(BITMAP_CROPS) { it.parameterCount == 3 } ?: return null
        return runCatching {
            (method.invoke(manager, listOf(Point(display.width(), display.height())), which, false) as? List<*>)?.firstOrNull() as? Rect
        }.getOrNull()?.takeUnless { it.isEmpty }
    }

    private fun updateShift(current: RectF?) {
        if (backdrop) return
        val origin = baked ?: return
        val placement = current ?: return
        val x = placement.left - origin.left
        val y = placement.top - origin.top
        if (x == shiftX && y == shiftY) return
        shiftX = x
        shiftY = y
        mainHandler.post { scaleListeners.forEach { it() } }
    }

    private fun placementOf(engine: Any, display: Rect, bitmapWidth: Int, bitmapHeight: Int, crop: Rect?): RectF? {
        val surface = (engine.fieldValue(RELAYOUT)?.fieldValue(FRAMES)?.fieldValue(FRAME) as? Rect)?.takeUnless { it.isEmpty }
            ?: (engine.takeIf { it.javaClass.name in LEGACY_ENGINES }?.fieldValue(WIN_FRAMES)?.fieldValue(FRAME) as? Rect)?.takeUnless { it.isEmpty }
            ?: (engine.callMethod(SURFACE_HOLDER) as? SurfaceHolder)?.surfaceFrame?.takeUnless { it.isEmpty }
            ?: return null
        val xOffset = ((engine.fieldValue(X_OFFSET) as? Float) ?: CENTER).coerceIn(0f, 1f)
        val yOffset = ((engine.fieldValue(Y_OFFSET) as? Float) ?: CENTER).coerceIn(0f, 1f)
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return null
        val scaleX = surface.width().toFloat() / bitmapWidth
        val scaleY = surface.height().toFloat() / bitmapHeight
        val cropLeft = (crop?.left ?: 0) * scaleX
        val cropTop = (crop?.top ?: 0) * scaleY
        val cropWidth = crop?.let { it.width() * scaleX } ?: surface.width().toFloat()
        val cropHeight = crop?.let { it.height() * scaleY } ?: surface.height().toFloat()
        val spareX = cropWidth - display.width()
        val spareY = cropHeight - display.height()
        val left = surface.left - (cropLeft + (if (spareX > 0) spareX * xOffset else 0f) + ROUND).toInt()
        val top = surface.top - (cropTop + (if (spareY > 0) spareY * yOffset else 0f) + ROUND).toInt()
        return RectF(left.toFloat(), top.toFloat(), (left + surface.width()).toFloat(), (top + surface.height()).toFloat())
    }

    private fun render(bitmap: Bitmap, placement: RectF, display: Rect): Bitmap {
        val output = createBitmap(display.width(), display.height())
        Canvas(output).drawBitmap(bitmap, null, placement, Paint(Paint.FILTER_BITMAP_FLAG))
        return output
    }

    private fun backdropPlacement(bitmap: Bitmap, display: Rect): RectF {
        val viewWidth = display.width()
        val viewHeight = display.height()
        val scale = (if (bitmap.width.toLong() * viewHeight > viewWidth.toLong() * bitmap.height) {
            viewHeight.toFloat() / bitmap.height
        } else {
            viewWidth.toFloat() / bitmap.width
        }).coerceAtLeast(1f)
        val dy = (viewHeight - bitmap.height * scale) * CENTER
        return RectF(0f, dy.roundToInt().toFloat(), (bitmap.width * scale).roundToInt().toFloat(), (bitmap.height * scale + dy).roundToInt().toFloat())
    }

    private fun centerCrop(bitmap: Bitmap, display: Rect): Bitmap {
        val ratio = max(display.width().toFloat() / bitmap.width, display.height().toFloat() / bitmap.height)
        val width = (bitmap.width * ratio).roundToInt()
        val height = (bitmap.height * ratio).roundToInt()
        val left = (width - display.width()) / 2f
        val top = (height - display.height()) / 2f
        return render(bitmap, RectF(-left, -top, width - left, height - top), display)
    }

    private fun sampledKey(bitmap: Bitmap): String {
        val crc = CRC32()
        val row = IntArray(SAMPLES)
        for (y in 0 until SAMPLES) {
            val py = y * (bitmap.height - 1) / (SAMPLES - 1)
            for (x in 0 until SAMPLES) row[x] = bitmap.getPixel(x * (bitmap.width - 1) / (SAMPLES - 1), py)
            row.forEach { value -> crc.update(value) }
        }
        return "${bitmap.width}x${bitmap.height}:" + crc.value
    }

    private const val FULL_QUALITY = 100
    private const val WRITE_TRUNCATE = "wt"
    private const val SAMPLES = 64
}
