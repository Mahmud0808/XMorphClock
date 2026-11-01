package com.drdisagree.xmorphclock.data.source.presets

object PresetPaths {
    const val PRESETS = "presets"
    const val IMAGES = "images"
    const val JSON = ".json"
    const val LAYOUT = ".layout.json"
    const val WEBP = ".webp"

    fun of(dir: String, name: String): String = "$dir${PresetArchive.SEPARATOR}$name"
}
