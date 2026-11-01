package com.drdisagree.xmorphclock.domain.model

data class FaceData(
    val weather: WeatherSnapshot? = null,
    val batteryLevel: Int? = null,
    val charging: Boolean = false,
    val steps: Int? = null,
    val dataUsageBytes: Long? = null,
    val tiles: Map<FaceTile, Int> = emptyMap()
) {
    fun tileState(tile: FaceTile): Int = tiles[tile] ?: TILE_OFF

    companion object {
        const val TILE_OFF = 0
        const val TILE_ON = 1
        const val TILE_ALT = 2
    }
}
