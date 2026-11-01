package com.drdisagree.xmorphclock.data.source

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.drdisagree.xmorphclock.data.mapper.PaletteJsonMapper

class PaletteStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE)

    init {
        XposedServiceSource.addBindListener { push(load()) }
    }

    fun load(): Map<String, Int> = PaletteJsonMapper.fromJson(prefs.getString(PrefKeys.WALLPAPER_PALETTE, null))

    fun save(palette: Map<String, Int>) {
        if (palette.isEmpty() || palette == load()) return
        prefs.edit { putString(PrefKeys.WALLPAPER_PALETTE, PaletteJsonMapper.toJson(palette)) }
        push(palette)
    }

    private fun push(palette: Map<String, Int>) {
        if (palette.isEmpty()) return
        XposedServiceSource.edit { it.putString(PrefKeys.WALLPAPER_PALETTE, PaletteJsonMapper.toJson(palette)) }
    }
}
