package com.drdisagree.xmorphclock.xposed.report

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import com.drdisagree.xmorphclock.BuildConfig
import com.drdisagree.xmorphclock.data.mapper.HookReportJsonMapper
import com.drdisagree.xmorphclock.data.source.ReportContract
import com.drdisagree.xmorphclock.domain.model.ClockFrame
import com.drdisagree.xmorphclock.domain.model.HookReport
import com.drdisagree.xmorphclock.domain.model.HookSignal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

object HookReporter {

    private const val ACTIVITY_THREAD = "android.app.ActivityThread"
    private const val CURRENT_APPLICATION = "currentApplication"
    private const val MAX_ERRORS = 20
    private const val MAX_LOGS = 60
    private const val MAX_FRAMES = 4
    private const val FRAME_DEBOUNCE = 400L
    private const val TIME_PATTERN = "HH:mm:ss.SSS"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val signals = ConcurrentHashMap<String, String>()
    private val missing = ConcurrentHashMap.newKeySet<String>()
    private val errors = CopyOnWriteArrayList<String>()
    private val probes = CopyOnWriteArrayList<String>()
    private val counters = ConcurrentHashMap<String, AtomicInteger>()
    private val logs = ConcurrentLinkedDeque<String>()
    private val timeFormat = SimpleDateFormat(TIME_PATTERN, Locale.US)
    private val loadedAt = System.currentTimeMillis()
    private var registered = false
    private var loadedUpdateTime = 0L

    @Volatile
    private var stateProvider: (() -> Map<String, String>)? = null

    @Volatile
    private var framework = ""

    @Volatile
    private var adapter: String? = null

    @Volatile
    private var clockFace: String? = null

    @Volatile
    private var largeFrame: ClockFrame? = null

    @Volatile
    private var smallFrame: ClockFrame? = null

    @Volatile
    private var screenAspect: Float? = null

    private val sendRunnable = Runnable { send() }

    private val requestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            send()
        }
    }

    fun framework(name: String) {
        framework = name
    }

    fun signal(name: String, source: String?) {
        if (source == null) {
            signals.remove(name)
            missing += name
        } else {
            missing -= name
            signals[name] = source
        }
    }

    fun adapter(name: String?) {
        adapter = name
    }

    fun probe(line: String) {
        probes += line
    }

    fun count(key: String) {
        counters.getOrPut(key) { AtomicInteger() }.incrementAndGet()
    }

    fun state(provider: () -> Map<String, String>) {
        stateProvider = provider
    }

    fun clockFace(name: String) {
        clockFace = name
        mainHandler.post { send() }
    }

    fun frame(large: Boolean, frame: ClockFrame, aspect: Float) {
        val previous = if (large) largeFrame else smallFrame
        if (previous == frame && screenAspect == aspect) return
        if (large) largeFrame = frame else smallFrame = frame
        screenAspect = aspect
        log("Frame ${if (large) "large" else "small"} top=%.4f left=%.4f width=%.4f".format(frame.top, frame.left, frame.width))
        mainHandler.removeCallbacks(sendRunnable)
        mainHandler.postDelayed(sendRunnable, FRAME_DEBOUNCE)
    }

    fun log(message: String, throwable: Throwable? = null) {
        val time = synchronized(timeFormat) { timeFormat.format(Date()) }
        val level = if (throwable == null) "I" else "E"
        logs.addLast("$time $level $message" + (throwable?.let { " | ${describe(it)}" } ?: ""))
        while (logs.size > MAX_LOGS) logs.pollFirst()
        if (throwable != null && errors.size < MAX_ERRORS) errors += "$message: ${describe(throwable)}"
    }

    private fun describe(throwable: Throwable): String = buildString {
        append(throwable.javaClass.name)
        throwable.message?.let { append(": ").append(it) }
        throwable.stackTrace.take(MAX_FRAMES).forEach { append(" @ ").append(it) }
        throwable.cause?.let { cause ->
            append(" <- ").append(cause.javaClass.name)
            cause.message?.let { append(": ").append(it) }
        }
    }

    fun start() {
        mainHandler.post {
            register()
            send()
        }
    }

    private fun register() {
        if (registered) return
        val context = application() ?: return
        loadedUpdateTime = moduleUpdatedAt(context)
        context.registerReceiver(requestReceiver, IntentFilter(ReportContract.ACTION_REQUEST), Context.RECEIVER_EXPORTED)
        registered = true
    }

    private fun send() {
        val context = application() ?: return
        val state = try {
            stateProvider?.invoke().orEmpty()
        } catch (throwable: Throwable) {
            mapOf("stateError" to describe(throwable))
        }
        val report = HookReport(
            timestamp = System.currentTimeMillis(),
            framework = framework,
            systemUiVersion = systemUiVersion(context),
            buildId = Build.ID,
            adapter = adapter,
            signals = signals.map { HookSignal(it.key, it.value) } + missing.map { HookSignal(it, null) },
            clockFace = clockFace,
            errors = errors.toList(),
            largeFrame = largeFrame,
            smallFrame = smallFrame,
            screenAspect = screenAspect,
            moduleUpdatedAt = loadedUpdateTime,
            moduleLoadedAt = loadedAt,
            uptimeMs = SystemClock.uptimeMillis() - Process.getStartUptimeMillis(),
            probes = probes.toList(),
            counters = counters.mapValues { it.value.get() },
            state = state,
            logs = logs.toList()
        )
        val intent = Intent(ReportContract.ACTION_REPORT)
            .setPackage(BuildConfig.APPLICATION_ID)
            .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES or Intent.FLAG_RECEIVER_FOREGROUND)
            .putExtra(ReportContract.EXTRA_REPORT, HookReportJsonMapper.toJson(report))
        try {
            context.sendBroadcast(intent)
        } catch (_: Throwable) {
        }
    }

    private fun moduleUpdatedAt(context: Context): Long = try {
        context.packageManager.getPackageInfo(BuildConfig.APPLICATION_ID, 0).lastUpdateTime
    } catch (_: Throwable) {
        0L
    }

    private fun systemUiVersion(context: Context): String = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "${info.versionName} (${info.longVersionCode})"
    } catch (_: Throwable) {
        ""
    }

    @SuppressLint("PrivateApi")
    private fun application(): Application? = try {
        Class.forName(ACTIVITY_THREAD).getMethod(CURRENT_APPLICATION).invoke(null) as? Application
    } catch (_: Throwable) {
        null
    }
}
