package dev.lewisscoging.nightstand.core

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeWindowTest {
    private fun minutes(hour: Int, minute: Int) = LocalTime.of(hour, minute).toMinutesOfDay()

    private val nightStart = minutes(22, 0)
    private val nightEnd = minutes(7, 0)
    private val dayStart = minutes(9, 0)
    private val dayEnd = minutes(17, 0)

    private fun inNightWindow(hour: Int, minute: Int) =
        isWithinWindow(minutes(hour, minute), nightStart, nightEnd)

    private fun inDayWindow(hour: Int, minute: Int) =
        isWithinWindow(minutes(hour, minute), dayStart, dayEnd)

    @Test
    fun toMinutesOfDayConvertsMidnightAndLastMinute() {
        assertEquals(0, minutes(0, 0))
        assertEquals(LAST_MINUTE_OF_DAY, minutes(23, 59))
        assertEquals(1320, minutes(22, 0))
    }

    @Test
    fun nightWindowExcludesMinuteBeforeStart() = assertFalse(inNightWindow(21, 59))

    @Test
    fun nightWindowIncludesStartMinute() = assertTrue(inNightWindow(22, 0))

    @Test
    fun nightWindowIncludesLastMinuteOfDay() = assertTrue(inNightWindow(23, 59))

    @Test
    fun nightWindowIncludesMidnight() = assertTrue(inNightWindow(0, 0))

    @Test
    fun nightWindowIncludesMinuteBeforeEnd() = assertTrue(inNightWindow(6, 59))

    @Test
    fun nightWindowExcludesEndMinute() = assertFalse(inNightWindow(7, 0))

    @Test
    fun nightWindowExcludesMidday() = assertFalse(inNightWindow(12, 0))

    @Test
    fun dayWindowIncludesStartMinute() = assertTrue(inDayWindow(9, 0))

    @Test
    fun dayWindowExcludesMinuteBeforeStart() = assertFalse(inDayWindow(8, 59))

    @Test
    fun dayWindowIncludesMinuteBeforeEnd() = assertTrue(inDayWindow(16, 59))

    @Test
    fun dayWindowExcludesEndMinute() = assertFalse(inDayWindow(17, 0))

    @Test
    fun equalStartAndEndIsAlwaysActive() {
        val sameMinute = minutes(8, 30)
        listOf(
            0,
            minutes(8, 29),
            sameMinute,
            minutes(8, 31),
            minutes(20, 0),
            LAST_MINUTE_OF_DAY
        ).forEach { now ->
            assertTrue("now=$now", isWithinWindow(now, sameMinute, sameMinute))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeNowThrows() {
        isWithinWindow(-1, nightStart, nightEnd)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nowOfFullDayThrows() {
        isWithinWindow(MINUTES_PER_DAY, nightStart, nightEnd)
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfRangeStartThrows() {
        isWithinWindow(0, MINUTES_PER_DAY, nightEnd)
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfRangeEndThrows() {
        isWithinWindow(0, nightStart, -1)
    }

    @Test
    fun minutesOfDayToLocalTimeRoundTrips() {
        assertEquals(LocalTime.MIDNIGHT, minutesOfDayToLocalTime(0))
        assertEquals(LocalTime.of(22, 0), minutesOfDayToLocalTime(1320))
        assertEquals(LocalTime.of(23, 59), minutesOfDayToLocalTime(LAST_MINUTE_OF_DAY))
        for (minutes in 0..LAST_MINUTE_OF_DAY) {
            assertEquals(minutes, minutesOfDayToLocalTime(minutes).toMinutesOfDay())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun minutesOfDayToLocalTimeRejectsOutOfRange() {
        minutesOfDayToLocalTime(MINUTES_PER_DAY)
    }
}
