package com.drdisagree.xmorphclock.ui.status

import android.content.Context
import android.os.Build
import com.drdisagree.xmorphclock.BuildConfig
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.HookReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatusReportFormatter {

    fun format(context: Context, state: StatusUiState): String = buildString {
        val installedAt = installedAt(context)
        section(context, R.string.report_section_diagnosis)
        diagnose(context, state, installedAt).forEach { appendLine("- $it") }

        section(context, R.string.report_section_device)
        appendLine(context.getString(R.string.report_app, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
        appendLine(context.getString(R.string.report_installed, time(installedAt)))
        appendLine(context.getString(R.string.report_device, Build.MANUFACTURER, Build.MODEL))
        appendLine(context.getString(R.string.report_android, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.ID))
        appendLine(context.getString(R.string.report_rom, Build.DISPLAY, Build.VERSION.SECURITY_PATCH))
        appendLine(context.getString(R.string.report_fingerprint, Build.FINGERPRINT))
        val framework = state.framework
        val frameworkText = framework?.let {
            context.getString(R.string.report_framework_value, it.name, it.version, it.apiVersion)
        } ?: context.getString(R.string.not_detected)
        appendLine(context.getString(R.string.report_framework, frameworkText))
        appendLine(context.getString(R.string.report_scope, framework?.scope?.joinToString().orEmpty()))

        val report = state.report ?: return@buildString
        section(context, R.string.report_section_systemui)
        appendLine(context.getString(R.string.report_systemui, report.systemUiVersion))
        appendLine(context.getString(R.string.report_time, time(report.timestamp)))
        appendLine(context.getString(R.string.report_uptime, report.uptimeMs / MS_PER_SECOND))
        appendLine(context.getString(R.string.report_module_loaded, time(report.moduleLoadedAt), time(report.moduleUpdatedAt)))
        appendLine(context.getString(R.string.report_hooked_framework, report.framework))
        appendLine(context.getString(R.string.report_adapter, report.adapter ?: context.getString(R.string.report_none)))
        appendLine(context.getString(R.string.report_face, report.clockFace ?: context.getString(R.string.report_not_attached)))
        report.largeFrame?.let { appendLine(context.getString(R.string.report_frame_large, it.left, it.top, it.width)) }
        report.smallFrame?.let { appendLine(context.getString(R.string.report_frame_small, it.left, it.top, it.width)) }

        lines(context, R.string.report_section_probes, report.probes)
        lines(context, R.string.report_section_signals, report.signals.map {
            "${it.name}: ${it.source ?: context.getString(R.string.report_missing)}"
        })
        lines(context, R.string.report_section_counters, report.counters.toSortedMap().map { "${it.key}: ${it.value}" })
        lines(context, R.string.report_section_state, report.state.map { "${it.key}: ${it.value}" })
        lines(context, R.string.report_section_errors, report.errors)
        lines(context, R.string.report_section_log, report.logs)
    }.trimEnd()

    private fun diagnose(context: Context, state: StatusUiState, installedAt: Long): List<String> {
        val issues = mutableListOf<String>()
        val report = state.report
        if (state.framework == null) issues += context.getString(R.string.diag_no_framework)
        else if (!state.isScoped) issues += context.getString(R.string.diag_not_scoped)
        if (report == null) {
            issues += context.getString(R.string.diag_no_report)
            return issues
        }
        if (report.moduleUpdatedAt in 1 until installedAt - STALE_SLACK_MS) {
            issues += context.getString(R.string.diag_stale, time(report.moduleUpdatedAt), time(installedAt))
        }
        if (report.state[STATE_ENABLED] == false.toString()) issues += context.getString(R.string.diag_disabled)
        if (report.adapter == null) issues += context.getString(R.string.diag_no_adapter)
        report.signals.filter { it.source == null }.forEach {
            issues += context.getString(R.string.diag_signal_missing, it.name, signalImpact(context, it.name))
        }
        if (report.adapter != null && HOOK_COUNTERS.none { (report.counters[it] ?: 0) > 0 }) {
            issues += context.getString(R.string.diag_not_bound)
        } else if (report.adapter != null && clockDetached(report)) {
            issues += context.getString(R.string.diag_not_attached)
        }
        if (report.errors.isNotEmpty()) issues += context.getString(R.string.diag_errors, report.errors.size)
        if (issues.isEmpty()) issues += context.getString(R.string.diag_ok)
        return issues
    }

    private fun clockDetached(report: HookReport): Boolean {
        val view = report.state[STATE_CLOCK_VIEW] ?: return false
        return view.startsWith(NOT_CREATED) || view.startsWith(DETACHED)
    }

    private fun signalImpact(context: Context, name: String): String = context.getString(
        when (name) {
            SIGNAL_SIZE -> R.string.diag_impact_size
            SIGNAL_PROGRESS -> R.string.diag_impact_progress
            else -> R.string.diag_impact_unknown
        }
    )

    private fun StringBuilder.section(context: Context, title: Int) {
        if (isNotEmpty()) appendLine()
        appendLine("## " + context.getString(title))
    }

    private fun StringBuilder.lines(context: Context, title: Int, lines: List<String>) {
        if (lines.isEmpty()) return
        section(context, title)
        lines.forEach { appendLine(it) }
    }

    private fun installedAt(context: Context): Long = try {
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
    } catch (_: Exception) {
        0L
    }

    private fun time(millis: Long): String =
        if (millis <= 0L) "-" else SimpleDateFormat(TIME_PATTERN, Locale.US).format(Date(millis))
}

private const val MS_PER_SECOND = 1000L
private const val TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
private const val STALE_SLACK_MS = 2_000L
private const val STATE_ENABLED = "enabled"
private const val STATE_CLOCK_VIEW = "clockView"
private const val NOT_CREATED = "not created"
private const val DETACHED = "detached"
private const val SIGNAL_SIZE = "CLOCK_SIZE"
private const val SIGNAL_PROGRESS = "KEYGUARD_PROGRESS"
private val HOOK_COUNTERS = listOf("addClockViews", "composeSmall", "composeLarge")
