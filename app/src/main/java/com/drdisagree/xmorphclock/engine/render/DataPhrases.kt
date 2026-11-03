package com.drdisagree.xmorphclock.engine.render

import android.content.res.Resources
import com.drdisagree.xmorphclock.R

internal data class DataPhrases(
    val charging: String,
    val charged: String,
    val chargingLeft: String,
    val hoursMinutes: String,
    val minutes: String,
    val noAlarm: String,
    val notCharging: String,
    val sunrise: String,
    val sunset: String,
    val uv: String,
    val noMedia: String
) {
    companion object {
        fun from(resources: Resources): DataPhrases = DataPhrases(
            charging = resources.getString(R.string.data_charging),
            charged = resources.getString(R.string.data_charged),
            chargingLeft = resources.getString(R.string.data_charging_left),
            hoursMinutes = resources.getString(R.string.data_hours_minutes),
            minutes = resources.getString(R.string.data_minutes),
            noAlarm = resources.getString(R.string.data_no_alarm),
            notCharging = resources.getString(R.string.data_not_charging),
            sunrise = resources.getString(R.string.data_sunrise),
            sunset = resources.getString(R.string.data_sunset),
            uv = resources.getString(R.string.data_uv),
            noMedia = resources.getString(R.string.data_no_media)
        )
    }
}
