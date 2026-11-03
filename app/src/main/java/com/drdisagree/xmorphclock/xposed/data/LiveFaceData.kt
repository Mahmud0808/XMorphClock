package com.drdisagree.xmorphclock.xposed.data

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.usage.NetworkStatsManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import com.drdisagree.xmorphclock.BuildConfig
import com.drdisagree.xmorphclock.data.source.FaceDataContract
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceLayerType
import com.drdisagree.xmorphclock.domain.model.FaceTile
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.prefs.ModulePrefs
import java.util.Calendar
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

object LiveFaceData {

    private val listeners = CopyOnWriteArrayList<(FaceData) -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var context: Context? = null
    private var tiles: TileController? = null
    private var needs: Set<FaceLayerType> = emptySet()
    private var tileKinds: Set<FaceTile> = emptySet()
    private var lastUsage = 0L
    private var lastStepsRequest = 0L

    @Volatile
    var current: FaceData = FaceData()
        private set
    private var batteryLevel: Int? = null
    private var charging = false
    private var batteryFull = false
    private var chargeRemaining: Long? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) battery(intent)
            if (intent.action == Intent.ACTION_SCREEN_ON) onScreenOn()
            refresh()
        }
    }

    fun start(context: Context) {
        if (this.context != null) return
        val app = context.applicationContext ?: context
        this.context = app
        val controller = TileController(app)
        tiles = controller
        controller.registerTorch(app.mainExecutor) { mainHandler.post { refresh() } }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(INTERNAL_RINGER_CHANGED)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(WIFI_AP_STATE_CHANGED)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)?.let(::battery)
        runCatching {
            app.getSystemService(TelephonyManager::class.java)?.registerTelephonyCallback(
                app.mainExecutor,
                object : TelephonyCallback(), TelephonyCallback.UserMobileDataStateListener {
                    override fun onUserMobileDataStateChanged(enabled: Boolean) = refresh()
                }
            )
        }
        ModulePrefs.addListener { refresh() }
        MediaWatcher.addListener { refresh() }
        NotificationCounter.addListener { refresh() }
        refresh()
    }

    fun configure(face: ClockFace) {
        needs = face.layers.map { it.type }.toSet()
        if (needs.any { it in MEDIA_TYPES }) context?.let(MediaWatcher::start)
        NotificationCounter.setEnabled(needs.any { it in NOTIFICATION_TYPES })
        tileKinds = face.layers.mapNotNull { it.tile }.toSet()
        lastUsage = 0L
        if (FaceLayerType.STEPS in needs) requestSteps(force = true)
        refresh()
    }

    fun toggle(tile: FaceTile) {
        val controller = tiles ?: return
        worker.execute {
            controller.toggle(tile)
            mainHandler.postDelayed({ refresh() }, TOGGLE_SETTLE_MS)
        }
    }

    fun addListener(listener: (FaceData) -> Unit) {
        listeners += listener
        listener(current)
    }

    private fun battery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        if (level < 0) return
        batteryLevel = level * 100 / scale
        charging = plugged
        batteryFull = plugged && intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_FULL
        chargeRemaining = if (plugged && !batteryFull) {
            context?.getSystemService(BatteryManager::class.java)?.computeChargeTimeRemaining()?.takeIf { it > 0 }
        } else {
            null
        }
    }

    private fun onScreenOn() {
        if (FaceLayerType.STEPS in needs) requestSteps(force = false)
        lastUsage = 0L
    }

    private fun requestSteps(force: Boolean) {
        val app = context ?: return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastStepsRequest < STEPS_INTERVAL_MS) return
        lastStepsRequest = now
        runCatching {
            app.sendBroadcast(
                Intent(FaceDataContract.ACTION_REQUEST_STEPS)
                    .setPackage(BuildConfig.APPLICATION_ID)
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES or Intent.FLAG_RECEIVER_FOREGROUND)
            )
        }
    }

    private fun refresh() {
        val controller = tiles
        val base = current
        val tileStates = if (controller == null) emptyMap() else tileKinds.associateWith { controller.state(it) }
        val usage = if (FaceLayerType.DATA_USAGE in needs) dataUsage() ?: base.dataUsageBytes else null
        val app = context
        val next = base.copy(
            batteryLevel = batteryLevel,
            charging = charging,
            batteryFull = batteryFull,
            chargeRemainingMs = chargeRemaining,
            nextAlarm = app?.let { runCatching { it.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime }.getOrNull() },
            use24Hour = app?.let { DateFormat.is24HourFormat(it) } ?: base.use24Hour,
            mediaTitle = MediaWatcher.title,
            mediaArtist = MediaWatcher.artist,
            mediaPlaying = MediaWatcher.playing,
            notificationCount = NotificationCounter.count,
            weather = ModulePrefs.weather,
            steps = ModulePrefs.steps,
            dataUsageBytes = usage,
            tiles = tileStates
        )
        if (next == current && listeners.isNotEmpty()) return
        current = next
        listeners.forEach { it(next) }
    }

    private fun dataUsage(): Long? {
        val app = context ?: return null
        val now = SystemClock.elapsedRealtime()
        if (now - lastUsage < USAGE_INTERVAL_MS && lastUsage != 0L) return null
        lastUsage = now
        return runCatching {
            val start = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            @Suppress("DEPRECATION")
            val bucket = app.getSystemService(NetworkStatsManager::class.java)
                .querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, start, System.currentTimeMillis())
            bucket.rxBytes + bucket.txBytes
        }.onFailure { HookEnv.log("Data usage unavailable", it) }.getOrNull()
    }

    private const val TOGGLE_SETTLE_MS = 350L
    private const val STEPS_INTERVAL_MS = 5 * 60_000L
    private const val USAGE_INTERVAL_MS = 5 * 60_000L
    private const val INTERNAL_RINGER_CHANGED = "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION"
    private const val WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
}

private val MEDIA_TYPES = setOf(FaceLayerType.MEDIA_TITLE, FaceLayerType.MEDIA_ARTIST, FaceLayerType.MEDIA_ICON)
private val NOTIFICATION_TYPES = setOf(FaceLayerType.NOTIFICATION_COUNT, FaceLayerType.NOTIFICATION_ICON)
