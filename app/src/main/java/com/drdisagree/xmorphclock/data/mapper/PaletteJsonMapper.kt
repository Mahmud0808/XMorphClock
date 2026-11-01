package com.drdisagree.xmorphclock.data.mapper

import org.json.JSONException
import org.json.JSONObject

object PaletteJsonMapper {

    fun toJson(palette: Map<String, Int>): String = JSONObject().apply { palette.forEach { (key, value) -> put(key, value) } }.toString()

    fun fromJson(json: String?): Map<String, Int> {
        if (json.isNullOrEmpty()) return emptyMap()
        return try {
            val root = JSONObject(json)
            root.keys().asSequence().associateWith { root.getInt(it) }
        } catch (_: JSONException) {
            emptyMap()
        }
    }
}
