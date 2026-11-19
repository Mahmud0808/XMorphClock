package com.drdisagree.xmorphclock.xposed.prefs

import android.content.Context
import android.util.AtomicFile
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

internal object PrefsSnapshot {

    private const val FILE = "xmorph_prefs.json"
    private const val COMPLETE = "complete"
    private const val VALUES = "values"
    private const val TYPE = "t"
    private const val VALUE = "v"
    private const val BOOLEAN = "b"
    private const val INT = "i"
    private const val LONG = "l"
    private const val FLOAT = "f"
    private const val STRING = "s"
    private const val STRING_SET = "S"

    fun load(context: Context): Pair<Map<String, Any>, Boolean>? {
        val file = fileOf(context)
        if (!file.baseFile.exists()) return null
        val root = JSONObject(file.readFully().decodeToString())
        val values = root.optJSONObject(VALUES) ?: return null
        val result = HashMap<String, Any>()
        values.keys().forEach { key ->
            val entry = values.optJSONObject(key) ?: return@forEach
            valueOf(entry)?.let { result[key] = it }
        }
        return result to root.optBoolean(COMPLETE)
    }

    fun save(context: Context, values: Map<String, Any?>, complete: Boolean) {
        val entries = JSONObject()
        values.forEach { (key, value) -> entryOf(value)?.let { entries.put(key, it) } }
        val root = JSONObject().put(COMPLETE, complete).put(VALUES, entries)
        val file = fileOf(context)
        val stream = file.startWrite()
        try {
            stream.write(root.toString().encodeToByteArray())
            file.finishWrite(stream)
        } catch (throwable: Throwable) {
            file.failWrite(stream)
            throw throwable
        }
    }

    private fun fileOf(context: Context): AtomicFile =
        AtomicFile(File(context.createDeviceProtectedStorageContext().filesDir, FILE))

    private fun entryOf(value: Any?): JSONObject? = when (value) {
        is Boolean -> JSONObject().put(TYPE, BOOLEAN).put(VALUE, value)
        is Int -> JSONObject().put(TYPE, INT).put(VALUE, value)
        is Long -> JSONObject().put(TYPE, LONG).put(VALUE, value.toString())
        is Float -> JSONObject().put(TYPE, FLOAT).put(VALUE, value.toString())
        is String -> JSONObject().put(TYPE, STRING).put(VALUE, value)
        is Set<*> -> JSONObject().put(TYPE, STRING_SET).put(VALUE, JSONArray(value.filterIsInstance<String>()))
        else -> null
    }

    private fun valueOf(entry: JSONObject): Any? = when (entry.optString(TYPE)) {
        BOOLEAN -> entry.optBoolean(VALUE)
        INT -> entry.optInt(VALUE)
        LONG -> entry.optString(VALUE).toLongOrNull()
        FLOAT -> entry.optString(VALUE).toFloatOrNull()
        STRING -> if (entry.has(VALUE)) entry.optString(VALUE) else null
        STRING_SET -> entry.optJSONArray(VALUE)?.let { array -> (0 until array.length()).map { array.optString(it) }.toSet() }
        else -> null
    }
}
