package com.drdisagree.xmorphclock.ui.home

import com.drdisagree.xmorphclock.domain.model.ElementKind

internal fun elementGlyph(kind: ElementKind): String = when (kind) {
    ElementKind.TEXT -> "\uE264"
    ElementKind.ICON -> "\uF09A"
    ElementKind.IMAGE -> "\uE3F4"
    ElementKind.CARD -> "\uEB54"
    ElementKind.RING -> "\uE917"
    ElementKind.BAR -> "\uE260"
    ElementKind.TIME -> "\uEFD6"
    ElementKind.HOURS -> "\uEA5B"
    ElementKind.MINUTES -> "\uE425"
    ElementKind.HOUR_TENS, ElementKind.HOUR_ONES -> "\uEA5B"
    ElementKind.MINUTE_TENS, ElementKind.MINUTE_ONES -> "\uE425"
    ElementKind.DATE -> "\uE935"
    ElementKind.HANDS -> "\uF8BB"
    ElementKind.TEMPERATURE -> "\uF076"
    ElementKind.WEATHER_ICON -> "\uF172"
    ElementKind.CONDITION -> "\uF15C"
    ElementKind.HIGH_LOW -> "\uE8D5"
    ElementKind.HUMIDITY -> "\uF87E"
    ElementKind.BATTERY -> "\uE1A5"
    ElementKind.BATTERY_ICON -> "\uE1A3"
    ElementKind.STEPS -> "\uE536"
    ElementKind.DATA_USAGE -> "\uEFF2"
    ElementKind.TILE -> "\uE9F6"
    ElementKind.ALARM -> "\uE855"
    ElementKind.ALARM_ICON -> "\uE855"
    ElementKind.CHARGING -> "\uEA0B"
    ElementKind.CHARGE_ICON -> "\uEA0B"
    ElementKind.SPACER -> "\uEA16"
}
