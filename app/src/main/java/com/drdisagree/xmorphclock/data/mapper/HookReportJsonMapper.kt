package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.model.ClockFrame
import com.drdisagree.xmorphclock.domain.model.HookReport
import com.drdisagree.xmorphclock.domain.model.HookSignal
import org.json.JSONArray
import org.json.JSONObject

object HookReportJsonMapper {

    fun toJson(report: HookReport): String = JSONObject()
        .put("timestamp", report.timestamp)
        .put("framework", report.framework)
        .put("systemUiVersion", report.systemUiVersion)
        .put("buildId", report.buildId)
        .put("adapter", report.adapter ?: JSONObject.NULL)
        .put("signals", JSONArray().apply {
            report.signals.forEach { signal ->
                put(JSONObject().put("name", signal.name).put("source", signal.source ?: JSONObject.NULL))
            }
        })
        .put("clockFace", report.clockFace ?: JSONObject.NULL)
        .put("errors", JSONArray(report.errors))
        .put("moduleUpdatedAt", report.moduleUpdatedAt)
        .put("moduleLoadedAt", report.moduleLoadedAt)
        .put("uptimeMs", report.uptimeMs)
        .put("probes", JSONArray(report.probes))
        .put("counters", JSONObject().apply { report.counters.forEach { (key, value) -> put(key, value) } })
        .put("state", JSONObject().apply { report.state.forEach { (key, value) -> put(key, value) } })
        .put("logs", JSONArray(report.logs))
        .apply {
            report.largeFrame?.let { put("largeFrame", it.toJson()) }
            report.smallFrame?.let { put("smallFrame", it.toJson()) }
            report.screenAspect?.let { put("screenAspect", it.toDouble()) }
        }
        .toString()

    fun fromJson(json: String): HookReport? = try {
        val root = JSONObject(json)
        val signals = root.getJSONArray("signals")
        HookReport(
            timestamp = root.getLong("timestamp"),
            framework = root.optString("framework"),
            systemUiVersion = root.optString("systemUiVersion"),
            buildId = root.optString("buildId"),
            adapter = root.optStringOrNull("adapter"),
            signals = List(signals.length()) { index ->
                val signal = signals.getJSONObject(index)
                HookSignal(signal.getString("name"), signal.optStringOrNull("source"))
            },
            clockFace = root.optStringOrNull("clockFace"),
            errors = root.optStrings("errors"),
            largeFrame = root.optJSONObject("largeFrame")?.let(::frameOf),
            smallFrame = root.optJSONObject("smallFrame")?.let(::frameOf),
            screenAspect = if (root.has("screenAspect")) root.getDouble("screenAspect").toFloat() else null,
            moduleUpdatedAt = root.optLong("moduleUpdatedAt"),
            moduleLoadedAt = root.optLong("moduleLoadedAt"),
            uptimeMs = root.optLong("uptimeMs"),
            probes = root.optStrings("probes"),
            counters = root.optJSONObject("counters")?.let { counters ->
                counters.keys().asSequence().associateWith { counters.optInt(it) }
            }.orEmpty(),
            state = root.optJSONObject("state")?.let { state ->
                state.keys().asSequence().associateWith { state.optString(it) }
            }.orEmpty(),
            logs = root.optStrings("logs")
        )
    } catch (_: Exception) {
        null
    }

    private fun JSONObject.optStrings(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return List(array.length()) { array.optString(it) }
    }

    private fun ClockFrame.toJson(): JSONObject = JSONObject()
        .put("left", left.toDouble())
        .put("top", top.toDouble())
        .put("width", width.toDouble())

    private fun frameOf(json: JSONObject): ClockFrame = ClockFrame(
        left = json.getDouble("left").toFloat(),
        top = json.getDouble("top").toFloat(),
        width = json.getDouble("width").toFloat()
    )

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (isNull(name)) null else optString(name)
}
