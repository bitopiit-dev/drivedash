package com.baridhi.drivedash.ui

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

const val KMH_TO_MPH = 0.621371f

fun speedIn(kmh: Float, mph: Boolean): Float = if (mph) kmh * KMH_TO_MPH else kmh
fun speedUnit(mph: Boolean) = if (mph) "mph" else "km/h"

/** Returns value + unit, e.g. ("12.4", "km") or ("850", "m"). */
fun formatDistance(meters: Float, mph: Boolean): Pair<String, String> {
    return if (mph) {
        val miles = meters / 1609.344f
        if (miles < 0.2f) "%.0f".format(meters * 3.28084f) to "ft"
        else if (miles < 100f) "%.1f".format(miles) to "mi"
        else "%.0f".format(miles) to "mi"
    } else {
        if (meters < 1000f) "%.0f".format(meters) to "m"
        else if (meters < 100_000f) "%.1f".format(meters / 1000f) to "km"
        else "%.0f".format(meters / 1000f) to "km"
    }
}

fun formatDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun formatEta(sec: Long): String {
    val h = sec / 3600
    val m = ((sec % 3600) + 59) / 60
    return when {
        h > 0 -> "${h} h ${m} min"
        m <= 1 -> "1 min"
        else -> "$m min"
    }
}

fun arrivalClock(etaSec: Long): String =
    LocalTime.now().plusSeconds(etaSec)
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

fun cardinal(deg: Float): String {
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return dirs[(((deg % 360f) + 360f) % 360f / 45f + 0.5f).toInt() % 8]
}
