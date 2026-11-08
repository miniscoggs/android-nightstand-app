package dev.lewisscoging.nightstand.core

import java.time.LocalTime

const val MINUTES_PER_HOUR = 60
const val MINUTES_PER_DAY = 1440
const val LAST_MINUTE_OF_DAY = MINUTES_PER_DAY - 1

fun LocalTime.toMinutesOfDay(): Int = hour * MINUTES_PER_HOUR + minute

/** True when [nowMinutes] lies in the window, including its start minute and excluding its end minute. */
fun isWithinWindow(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
    requireMinuteOfDay(nowMinutes, "nowMinutes")
    requireMinuteOfDay(startMinutes, "startMinutes")
    requireMinuteOfDay(endMinutes, "endMinutes")
    return when {
        startMinutes == endMinutes -> true
        startMinutes < endMinutes -> nowMinutes in startMinutes until endMinutes
        else -> nowMinutes >= startMinutes || nowMinutes < endMinutes // crosses midnight
    }
}

private fun requireMinuteOfDay(minutes: Int, name: String) {
    require(minutes in 0..LAST_MINUTE_OF_DAY) {
        "$name must be in 0..$LAST_MINUTE_OF_DAY but was $minutes"
    }
}
