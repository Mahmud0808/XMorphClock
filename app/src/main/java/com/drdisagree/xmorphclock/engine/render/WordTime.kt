package com.drdisagree.xmorphclock.engine.render

import android.icu.text.MessageFormat
import java.util.Locale

internal object WordTime {

    private val UNITS = listOf(
        "twelve", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven",
        "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
    )
    private val TENS = listOf("", "", "twenty", "thirty", "forty", "fifty")

    fun hour(hour: Int, locale: Locale = Locale.ENGLISH): String {
        if (!locale.isEnglish()) return spell(locale, (hour % HALF_DAY).let { if (it == 0) HALF_DAY else it })
        return UNITS[hour % HALF_DAY]
    }

    fun minute(minute: Int, locale: Locale = Locale.ENGLISH): String {
        if (!locale.isEnglish()) return if (minute == 0) "" else spell(locale, minute)
        return englishMinute(minute)
    }

    fun time(hour: Int, minute: Int, locale: Locale = Locale.ENGLISH): String =
        listOf(hour(hour, locale), minute(minute, locale)).filter { it.isNotEmpty() }.joinToString(" ")

    private fun englishMinute(minute: Int): String = when {
        minute == 0 -> O_CLOCK
        minute < 10 -> "oh " + UNITS[minute]
        minute < 20 -> UNITS[minute]
        minute % 10 == 0 -> TENS[minute / 10]
        else -> TENS[minute / 10] + "-" + UNITS[minute % 10]
    }

    fun fuzzyLead(hour: Int, minute: Int): String {
        val rounded = rounded(minute)
        return when {
            rounded == 0 || rounded == HOUR -> ""
            rounded == QUARTER -> "quarter past"
            rounded == HALF -> "half past"
            rounded == HOUR - QUARTER -> "quarter to"
            rounded < HALF -> spoken(rounded) + " past"
            else -> spoken(HOUR - rounded) + " to"
        }
    }

    fun fuzzyHour(hour: Int, minute: Int): String {
        val rounded = rounded(minute)
        return when {
            rounded == 0 -> hour(hour) + " " + O_CLOCK
            rounded == HOUR -> hour(hour + 1) + " " + O_CLOCK
            rounded > HALF -> hour(hour + 1)
            else -> hour(hour)
        }
    }

    fun fuzzy(hour: Int, minute: Int): String = listOf(fuzzyLead(hour, minute), fuzzyHour(hour, minute))
        .filter { it.isNotEmpty() }
        .joinToString(" ")

    private fun Locale.isEnglish(): Boolean = language == Locale.ENGLISH.language

    private fun spell(locale: Locale, value: Int): String = runCatching {
        formats.getOrPut(locale) { MessageFormat(SPELLOUT, locale) }.format(arrayOf<Any>(value))
    }.getOrDefault(value.toString())

    private val formats = HashMap<Locale, MessageFormat>()

    private fun rounded(minute: Int): Int = (minute + 2) / STEP * STEP

    private fun spoken(minutes: Int): String = when {
        minutes < 20 -> UNITS[minutes]
        minutes % 10 == 0 -> TENS[minutes / 10]
        else -> TENS[minutes / 10] + "-" + UNITS[minutes % 10]
    }

    private const val O_CLOCK = "o'clock"
    private const val SPELLOUT = "{0,spellout}"
    private const val HALF_DAY = 12
    private const val HOUR = 60
    private const val HALF = 30
    private const val QUARTER = 15
    private const val STEP = 5
}
