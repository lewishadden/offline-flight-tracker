package com.lewishadden.flighttracker.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_TIME_FMT = DateTimeFormatter.ofPattern("MMM d · HH:mm")

fun Instant?.formatTime(zone: String?): String {
    if (this == null) return "—"
    val zid = runCatching { ZoneId.of(zone ?: "UTC") }.getOrDefault(ZoneId.of("UTC"))
    return atZone(zid).format(TIME_FMT)
}

fun Instant?.formatDateTime(zone: String?): String {
    if (this == null) return "—"
    val zid = runCatching { ZoneId.of(zone ?: "UTC") }.getOrDefault(ZoneId.of("UTC"))
    return atZone(zid).format(DATE_TIME_FMT)
}

fun Long?.formatDelay(): String = when {
    this == null -> "—"
    this == 0L -> "On time"
    this > 0 -> "+${this}m late"
    else -> "${this}m early"
}
