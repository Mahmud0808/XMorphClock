package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.model.LyricsTuning
import org.json.JSONObject

object LyricsTuningJsonMapper {

    fun toJson(tunings: Map<String, LyricsTuning>): String {
        val root = JSONObject()
        tunings.forEach { (key, tuning) ->
            if (tuning.isDefault) return@forEach
            root.put(key, JSONObject().apply {
                if (tuning.offsetMs != 0L) put(OFFSET, tuning.offsetMs)
                tuning.pinned?.let { put(PINNED, it) }
                if (tuning.hidden) put(HIDDEN, true)
            })
        }
        return root.toString()
    }

    fun fromJson(json: String?): Map<String, LyricsTuning> {
        if (json.isNullOrBlank()) return emptyMap()
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyMap()
        val result = LinkedHashMap<String, LyricsTuning>()
        root.keys().forEach { key ->
            val item = root.optJSONObject(key) ?: return@forEach
            result[key] = LyricsTuning(
                offsetMs = item.optLong(OFFSET, 0L),
                pinned = item.optString(PINNED).takeIf { it.isNotEmpty() },
                hidden = item.optBoolean(HIDDEN, false)
            )
        }
        return result
    }

    private const val OFFSET = "o"
    private const val PINNED = "p"
    private const val HIDDEN = "h"
}
