package com.drdisagree.xmorphclock.data.source.weather

import android.content.Context
import com.drdisagree.xmorphclock.R

internal object WeatherCodes {

    fun fromWmo(code: Int, isDay: Boolean): Int = when (code) {
        0 -> if (isDay) 32 else 31
        1 -> if (isDay) 34 else 33
        2 -> if (isDay) 30 else 29
        3 -> 26
        45, 48 -> 20
        51, 53, 55 -> 9
        56, 57 -> 8
        61, 63 -> 11
        65 -> 12
        66, 67 -> 10
        71 -> 14
        73 -> 16
        75 -> 41
        77 -> 13
        80 -> 40
        81 -> 11
        82 -> 12
        85 -> 46
        86 -> 42
        95 -> 4
        96 -> 38
        99 -> 3
        else -> -1
    }

    fun fromMetNorway(symbol: String, isDay: Boolean): Int = when {
        symbol.contains("thunder") -> 4
        symbol.contains("sleet") -> 18
        symbol.endsWith("snowshowers") -> 46
        symbol == "heavysnow" -> 41
        symbol == "snow" -> 16
        symbol == "lightsnow" -> 14
        symbol.endsWith("rainshowers") -> if (symbol.startsWith("heavy")) 12 else 40
        symbol == "heavyrain" -> 12
        symbol == "rain" || symbol == "lightrain" -> 11
        symbol == "fog" -> 20
        symbol == "cloudy" -> 26
        symbol == "partlycloudy" -> if (isDay) 30 else 29
        symbol == "fair" -> if (isDay) 34 else 33
        symbol == "clearsky" -> if (isDay) 32 else 31
        else -> -1
    }

    fun fromOpenWeatherMap(id: Int, isDay: Boolean): Int = when (id) {
        in 200..299 -> 4
        in 300..399 -> 9
        500, 501 -> 11
        in 502..504 -> 12
        511 -> 10
        in 520..531 -> 40
        600 -> 14
        601 -> 16
        602 -> 41
        in 611..616 -> 18
        in 620..622 -> 46
        701, 721, 741 -> 20
        711 -> 22
        731, 751, 761, 762 -> 19
        771 -> 23
        781 -> 0
        800 -> if (isDay) 32 else 31
        801 -> if (isDay) 34 else 33
        802 -> if (isDay) 30 else 29
        803 -> if (isDay) 28 else 27
        804 -> 26
        else -> -1
    }

    fun condition(context: Context, code: Int): String = context.getString(
        when (code) {
            31, 32, 36 -> R.string.weather_clear
            33, 34 -> R.string.weather_mostly_clear
            29, 30, 44 -> R.string.weather_partly_cloudy
            27, 28 -> R.string.weather_mostly_cloudy
            26 -> R.string.weather_cloudy
            19, 20, 21, 22 -> R.string.weather_fog
            9 -> R.string.weather_drizzle
            8, 10 -> R.string.weather_freezing_rain
            11 -> R.string.weather_rain
            12 -> R.string.weather_heavy_rain
            40 -> R.string.weather_showers
            13, 14, 15, 16, 41, 43 -> R.string.weather_snow
            42, 46 -> R.string.weather_snow_showers
            5, 6, 7, 18 -> R.string.weather_sleet
            17, 35 -> R.string.weather_hail
            3, 4, 37, 38, 39, 45, 47 -> R.string.weather_thunderstorm
            23, 24 -> R.string.weather_windy
            0, 1, 2 -> R.string.weather_storm
            else -> R.string.weather_unknown
        }
    )
}
