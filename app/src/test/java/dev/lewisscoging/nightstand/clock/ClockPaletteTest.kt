package dev.lewisscoging.nightstand.clock

import dev.lewisscoging.nightstand.settings.BackgroundColor
import dev.lewisscoging.nightstand.settings.TextColor
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockPaletteTest {
    private fun hex(argb: Int) = "%08X".format(argb)

    @Test
    fun textColoursMapToTheirPaletteValues() {
        assertEquals("FFFFFFFF", hex(TextColor.WHITE.argb()))
        assertEquals("FFFFD8A8", hex(TextColor.WARM.argb()))
        assertEquals("FFFF2A1A", hex(TextColor.RED.argb()))
        assertEquals("FFFFB000", hex(TextColor.AMBER.argb()))
        assertEquals("FF3DDC84", hex(TextColor.GREEN.argb()))
        assertEquals("FF00E5FF", hex(TextColor.CYAN.argb()))
    }

    @Test
    fun backgroundColoursMapToTheirPaletteValues() {
        assertEquals("FF000000", hex(BackgroundColor.OFF.argb()))
        assertEquals("FF1C1C1E", hex(BackgroundColor.DARK_GREY.argb()))
        assertEquals("FF0B1033", hex(BackgroundColor.MIDNIGHT_BLUE.argb()))
        assertEquals("FF2A0505", hex(BackgroundColor.DEEP_RED.argb()))
    }

    @Test
    fun fullBrightnessChangesNothing() {
        TextColor.entries.forEach { assertEquals(it.argb(), scaleBrightness(it.argb(), 1.0f)) }
        BackgroundColor.entries.forEach {
            assertEquals(it.argb(), scaleBrightness(it.argb(), 1.0f))
        }
    }

    @Test
    fun halfBrightnessHalvesEachChannel() {
        assertEquals("FF80806C", hex(scaleBrightness(0xFFFFFFD8.toInt(), 0.5f)))
    }

    @Test
    fun minimumBrightnessFloorApplies() {
        assertEquals("FF0D0D0D", hex(scaleBrightness(TEXT_WHITE_ARGB, 0.05f)))
    }

    @Test
    fun outOfRangeFactorsAreClamped() {
        val floor = scaleBrightness(TEXT_WHITE_ARGB, 0.05f)
        assertEquals(floor, scaleBrightness(TEXT_WHITE_ARGB, 0.0f))
        assertEquals(floor, scaleBrightness(TEXT_WHITE_ARGB, -3.0f))
        assertEquals(TEXT_AMBER_ARGB, scaleBrightness(TEXT_AMBER_ARGB, 2.5f))
        assertEquals(
            scaleBrightness(TEXT_WHITE_ARGB, 0.2f),
            scaleBrightness(TEXT_WHITE_ARGB, Float.NaN)
        )
    }

    @Test
    fun offBackgroundNeverChanges() {
        listOf(-1.0f, 0.0f, 0.05f, 0.2f, 0.5f, 1.0f, 4.0f, Float.NaN).forEach {
            assertEquals("FF000000", hex(scaleBrightness(BackgroundColor.OFF.argb(), it)))
        }
    }

    @Test
    fun alphaIsPreserved() {
        assertEquals("80404040", hex(scaleBrightness(0x80808080.toInt(), 0.5f)))
        assertEquals("00000000", hex(scaleBrightness(0x00000000, 0.5f)))
        assertEquals("FF000000", hex(scaleBrightness(TEXT_WHITE_ARGB, 0.0f) and 0xFF000000.toInt()))
    }
}
