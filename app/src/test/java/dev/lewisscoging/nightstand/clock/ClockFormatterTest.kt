package dev.lewisscoging.nightstand.clock

import dev.lewisscoging.nightstand.settings.HourFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockFormatterTest {
    private fun clock24(hour: Int, minute: Int) =
        formatClock(LocalTime.of(hour, minute), use24Hour = true)

    private fun clock12(hour: Int, minute: Int) =
        formatClock(LocalTime.of(hour, minute), use24Hour = false)

    @Test
    fun twentyFourHourClockPadsHoursAndMinutes() {
        assertEquals("00:00", clock24(0, 0))
        assertEquals("00:05", clock24(0, 5))
        assertEquals("00:59", clock24(0, 59))
        assertEquals("01:00", clock24(1, 0))
        assertEquals("11:59", clock24(11, 59))
        assertEquals("12:00", clock24(12, 0))
        assertEquals("12:59", clock24(12, 59))
        assertEquals("13:00", clock24(13, 0))
        assertEquals("23:59", clock24(23, 59))
    }

    @Test
    fun twelveHourClockShowsMidnightHourAsTwelve() {
        assertEquals("12:00", clock12(0, 0))
        assertEquals("12:05", clock12(0, 5))
        assertEquals("12:59", clock12(0, 59))
    }

    @Test
    fun twelveHourClockMorningHasNoLeadingZero() {
        assertEquals("1:00", clock12(1, 0))
        assertEquals("11:59", clock12(11, 59))
    }

    @Test
    fun twelveHourClockNoonHourIsTwelve() {
        assertEquals("12:00", clock12(12, 0))
        assertEquals("12:59", clock12(12, 59))
    }

    @Test
    fun twelveHourClockAfternoonWrapsWithNoMarker() {
        assertEquals("1:00", clock12(13, 0))
        assertEquals("11:59", clock12(23, 59))
    }

    @Test
    fun systemFormatFollowsSystem24Hour() =
        assertTrue(resolveUse24Hour(HourFormat.SYSTEM, systemIs24Hour = true))

    @Test
    fun systemFormatFollowsSystem12Hour() =
        assertFalse(resolveUse24Hour(HourFormat.SYSTEM, systemIs24Hour = false))

    @Test
    fun forced12HourIgnoresSystem24Hour() =
        assertFalse(resolveUse24Hour(HourFormat.H12, systemIs24Hour = true))

    @Test
    fun forced12HourWithSystem12Hour() =
        assertFalse(resolveUse24Hour(HourFormat.H12, systemIs24Hour = false))

    @Test
    fun forced24HourWithSystem24Hour() =
        assertTrue(resolveUse24Hour(HourFormat.H24, systemIs24Hour = true))

    @Test
    fun forced24HourIgnoresSystem12Hour() =
        assertTrue(resolveUse24Hour(HourFormat.H24, systemIs24Hour = false))

    @Test
    fun formatDateUsesGivenPatternAndLocale() {
        val date = LocalDate.of(2026, 11, 8)
        assertEquals("Sun 8 Nov", formatDate(date, "EEE d MMM", Locale.UK))
    }
}
