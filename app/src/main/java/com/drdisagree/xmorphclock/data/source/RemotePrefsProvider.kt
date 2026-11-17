package com.drdisagree.xmorphclock.data.source

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process

class RemotePrefsProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != RemotePrefsContract.METHOD_READ) return super.call(method, arg, extras)
        enforceCaller()
        val context = context ?: return null
        val all = context.getSharedPreferences(RemotePrefsContract.MIRROR, Context.MODE_PRIVATE).all
        val values = Bundle()
        val types = Bundle()
        val removed = ArrayList<String>()
        if (arg == null) {
            all.forEach { (key, value) -> put(values, types, key, value) }
        } else if (!put(values, types, arg, all[arg])) {
            removed += arg
        }
        return Bundle().apply {
            putBundle(RemotePrefsContract.VALUES, values)
            putBundle(RemotePrefsContract.TYPES, types)
            putStringArrayList(RemotePrefsContract.REMOVED, removed)
        }
    }

    private fun enforceCaller() {
        if (Binder.getCallingUid() == Process.myUid()) return
        if (callingPackage in SCOPE) return
        throw SecurityException("Caller not in module scope")
    }

    private fun put(values: Bundle, types: Bundle, key: String, value: Any?): Boolean {
        val type = when (value) {
            is Boolean -> RemotePrefsContract.TYPE_BOOLEAN.also { values.putBoolean(key, value) }
            is Int -> RemotePrefsContract.TYPE_INT.also { values.putInt(key, value) }
            is Long -> RemotePrefsContract.TYPE_LONG.also { values.putLong(key, value) }
            is Float -> RemotePrefsContract.TYPE_FLOAT.also { values.putFloat(key, value) }
            is String -> RemotePrefsContract.TYPE_STRING.also { values.putString(key, value) }
            is Set<*> -> RemotePrefsContract.TYPE_STRING_SET.also { values.putStringArray(key, value.filterIsInstance<String>().toTypedArray()) }
            else -> return false
        }
        types.putString(key, type)
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private companion object {
        val SCOPE = setOf(ReportContract.SYSTEMUI_PACKAGE)
    }
}
