package dev.lewisscoging.nightstand.clock

import dev.lewisscoging.nightstand.settings.HourFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

const val CLOCK_PATTERN_24_HOUR = "HH:mm"
const val CLOCK_PATTERN_12_HOUR = "h:mm"

private val clockFormatter24Hour = DateTimeFormatter.ofPattern(CLOCK_PATTERN_24_HOUR, Locale.ROOT)
private val clockFormatter12Hour = DateTimeFormatter.ofPattern(CLOCK_PATTERN_12_HOUR, Locale.ROOT)

fun resolveUse24Hour(format: HourFormat, systemIs24Hour: Boolean): Boolean = when (format) {
    HourFormat.SYSTEM -> systemIs24Hour
    HourFormat.H12 -> false
    HourFormat.H24 -> true
}

/** Formats [time] as `HH:mm`, or as `h:mm` with no AM/PM marker when [use24Hour] is false. */
fun formatClock(time: LocalTime, use24Hour: Boolean): String {
    val formatter = if (use24Hour) clockFormatter24Hour else clockFormatter12Hour
    return time.format(formatter)
}

fun formatDate(date: LocalDate, pattern: String, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern(pattern, locale))
