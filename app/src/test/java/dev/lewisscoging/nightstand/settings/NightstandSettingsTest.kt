package dev.lewisscoging.nightstand.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightstandSettingsTest {
    private val defaults = NightstandSettings()

    @Test
    fun defaultsMatchTheSettingsTable() {
        assertTrue(defaults.enabled)
        assertEquals(22 * 60, defaults.startTime)
        assertEquals(7 * 60, defaults.endTime)
        assertEquals(15, defaults.clockTimeoutSeconds)
        assertTrue(defaults.requireLandscape)
        assertEquals(HourFormat.SYSTEM, defaults.use24Hour)
        assertEquals(ClockFont.SYSTEM, defaults.clockFont)
        assertEquals(TextColor.WHITE, defaults.textColor)
        assertEquals(BackgroundColor.OFF, defaults.backgroundColor)
        assertEquals(0.2f, defaults.clockBrightness, 0f)
        assertFalse(defaults.showDate)
        assertTrue(defaults.burnInShift)
    }

    @Test
    fun enumsHaveTheExpectedValues() {
        assertEquals(listOf("SYSTEM", "H12", "H24"), HourFormat.entries.map { it.name })
        assertEquals(listOf("SYSTEM", "DIGITAL", "BUBBLE"), ClockFont.entries.map { it.name })
        assertEquals(
            listOf("WHITE", "WARM", "RED", "AMBER", "GREEN", "CYAN"),
            TextColor.entries.map { it.name }
        )
        assertEquals(
            listOf("OFF", "DARK_GREY", "MIDNIGHT_BLUE", "DEEP_RED"),
            BackgroundColor.entries.map { it.name }
        )
    }

    @Test
    fun defaultsAreAlreadySanitised() = assertEquals(defaults, defaults.sanitised())

    @Test
    fun inRangeValuesAreUnchanged() {
        val settings = defaults.copy(
            startTime = 0,
            endTime = 1439,
            clockTimeoutSeconds = 60,
            clockBrightness = 0.5f
        )
        assertEquals(settings, settings.sanitised())
    }

    @Test
    fun rangeBoundariesAreUnchanged() {
        val lowest = defaults.copy(clockTimeoutSeconds = 5, clockBrightness = 0.05f)
        val highest = defaults.copy(clockTimeoutSeconds = 120, clockBrightness = 1.0f)
        assertEquals(lowest, lowest.sanitised())
        assertEquals(highest, highest.sanitised())
    }

    @Test
    fun timeoutBelowRangeClampsToMinimum() =
        assertEquals(5, defaults.copy(clockTimeoutSeconds = 4).sanitised().clockTimeoutSeconds)

    @Test
    fun timeoutAboveRangeClampsToMaximum() =
        assertEquals(120, defaults.copy(clockTimeoutSeconds = 121).sanitised().clockTimeoutSeconds)

    @Test
    fun brightnessBelowRangeClampsToMinimum() =
        assertEquals(0.05f, defaults.copy(clockBrightness = 0f).sanitised().clockBrightness, 0f)

    @Test
    fun brightnessAboveRangeClampsToMaximum() =
        assertEquals(1.0f, defaults.copy(clockBrightness = 1.5f).sanitised().clockBrightness, 0f)

    @Test
    fun nanBrightnessFallsBackToTheDefault() = assertEquals(
        DEFAULT_CLOCK_BRIGHTNESS,
        defaults.copy(clockBrightness = Float.NaN).sanitised().clockBrightness,
        0f
    )

    @Test
    fun negativeTimesClampToMidnight() {
        val sanitised = defaults.copy(startTime = -1, endTime = -100).sanitised()
        assertEquals(0, sanitised.startTime)
        assertEquals(0, sanitised.endTime)
    }

    @Test
    fun timesPastEndOfDayClampToLastMinute() {
        val sanitised = defaults.copy(startTime = 1440, endTime = 5000).sanitised()
        assertEquals(1439, sanitised.startTime)
        assertEquals(1439, sanitised.endTime)
    }

    @Test
    fun sanitisingLeavesNonRangedFieldsAlone() {
        val settings = NightstandSettings(
            enabled = false,
            requireLandscape = false,
            use24Hour = HourFormat.H24,
            clockFont = ClockFont.DIGITAL,
            textColor = TextColor.RED,
            backgroundColor = BackgroundColor.DEEP_RED,
            showDate = true,
            burnInShift = false,
            clockTimeoutSeconds = 0
        )
        assertEquals(settings.copy(clockTimeoutSeconds = 5), settings.sanitised())
    }
}
