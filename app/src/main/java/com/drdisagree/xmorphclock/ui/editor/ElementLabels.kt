package com.drdisagree.xmorphclock.ui.editor

import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.ElementKind

internal fun elementLabel(kind: ElementKind): Int = when (kind) {
    ElementKind.TIME -> R.string.layer_time
    ElementKind.HOURS -> R.string.layer_hours
    ElementKind.MINUTES -> R.string.layer_minutes
    ElementKind.HOUR_TENS -> R.string.layer_hour_tens
    ElementKind.HOUR_ONES -> R.string.layer_hour_ones
    ElementKind.MINUTE_TENS -> R.string.layer_minute_tens
    ElementKind.MINUTE_ONES -> R.string.layer_minute_ones
    ElementKind.DATE -> R.string.layer_date
    ElementKind.HANDS -> R.string.element_hands
    ElementKind.TEXT -> R.string.layer_text
    ElementKind.ICON -> R.string.element_icon
    ElementKind.IMAGE -> R.string.layer_image
    ElementKind.CARD -> R.string.layer_card
    ElementKind.RING -> R.string.element_ring
    ElementKind.BAR -> R.string.element_bar
    ElementKind.TEMPERATURE -> R.string.layer_weather_temp
    ElementKind.WEATHER_ICON -> R.string.layer_weather_icon
    ElementKind.CONDITION -> R.string.layer_weather_condition
    ElementKind.HIGH_LOW -> R.string.layer_weather_high_low
    ElementKind.HUMIDITY -> R.string.layer_weather_humidity
    ElementKind.BATTERY -> R.string.layer_battery_level
    ElementKind.BATTERY_ICON -> R.string.layer_battery_icon
    ElementKind.STEPS -> R.string.layer_steps
    ElementKind.DATA_USAGE -> R.string.layer_data_usage
    ElementKind.TILE -> R.string.layer_tile
    ElementKind.ALARM -> R.string.layer_alarm_time
    ElementKind.ALARM_ICON -> R.string.layer_alarm_icon
    ElementKind.CHARGING -> R.string.layer_charge_status
    ElementKind.CHARGE_ICON -> R.string.layer_charge_icon
    ElementKind.SUN_TIME -> R.string.layer_sun_time
    ElementKind.SUN_ICON -> R.string.layer_sun_icon
    ElementKind.UV -> R.string.layer_uv
    ElementKind.HOURLY_TEMP -> R.string.layer_hourly_temp
    ElementKind.HOURLY_ICON -> R.string.layer_hourly_icon
    ElementKind.HOURLY_TIME -> R.string.layer_hourly_time
    ElementKind.MEDIA_TITLE -> R.string.layer_media_title
    ElementKind.MEDIA_ARTIST -> R.string.layer_media_artist
    ElementKind.MEDIA_ICON -> R.string.layer_media_icon
    ElementKind.MEDIA_CONTROL -> R.string.tile_media
    ElementKind.MEDIA_ART -> R.string.layer_media_art
    ElementKind.NOTIFICATIONS -> R.string.layer_notification_count
    ElementKind.NOTIFICATION_ICON -> R.string.layer_notification_icon
    ElementKind.EVENT_TITLE -> R.string.layer_event_title
    ElementKind.EVENT_TIME -> R.string.layer_event_time
    ElementKind.EVENT_ICON -> R.string.layer_event_icon
    ElementKind.SPACER -> R.string.layer_spacer
}
