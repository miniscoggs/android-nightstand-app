package dev.lewisscoging.nightstand.clock

import dev.lewisscoging.nightstand.settings.HourFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

const val CLOCK_PATTERN_24_HOUR = "HH:mm"
const val CLOCK_PATTERN_12_HOUR = "h:mm"
const val FALLBACK_DATE_PATTERN = "EEE d MMM" // when java.time rejects the system's ICU pattern

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

private class DateFormatterCacheEntry(
    val pattern: String,
    val locale: Locale,
    val formatter: DateTimeFormatter
)

@Volatile private var dateFormatterCache: DateFormatterCacheEntry? = null // reused across ticks

/** Formats [date] with [pattern], or with [FALLBACK_DATE_PATTERN] if java.time rejects [pattern]. */
fun formatDate(date: LocalDate, pattern: String, locale: Locale): String {
    val cacheEntry = dateFormatterCache?.takeIf { it.pattern == pattern && it.locale == locale }
        ?: DateFormatterCacheEntry(pattern, locale, dateFormatterOrFallback(pattern, locale))
            .also { dateFormatterCache = it }
    return date.format(cacheEntry.formatter)
}

private fun dateFormatterOrFallback(pattern: String, locale: Locale): DateTimeFormatter = try {
    DateTimeFormatter.ofPattern(pattern, locale)
} catch (_: IllegalArgumentException) {
    DateTimeFormatter.ofPattern(FALLBACK_DATE_PATTERN, locale)
}
