package dev.lewisscoging.nightstand.clock

import java.awt.Font
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class ClockFontsGlyphTest {
    companion object {
        private const val FONT_DIRECTORY = "src/main/res/font" // relative to the module directory
        private const val CLOCK_CHARACTERS = "0123456789:"

        @JvmStatic
        @BeforeClass
        fun useHeadlessAwt() {
            System.setProperty("java.awt.headless", "true")
        }

        private fun loadFont(fileName: String): Font =
            File(FONT_DIRECTORY, fileName).inputStream().use { stream ->
                Font.createFont(Font.TRUETYPE_FONT, stream)
            }

        private fun assertDisplaysClockCharacters(fileName: String) {
            val font = loadFont(fileName)
            CLOCK_CHARACTERS.forEach { character ->
                assertTrue("$fileName lacks '$character'", font.canDisplay(character))
            }
        }
    }

    @Test
    fun digitalFontLoads() {
        assertTrue(loadFont("clock_digital.ttf").numGlyphs > 0)
    }

    @Test
    fun bubbleFontLoads() {
        assertTrue(loadFont("clock_bubble.ttf").numGlyphs > 0)
    }

    @Test
    fun digitalFontDisplaysEveryClockCharacter() {
        assertDisplaysClockCharacters("clock_digital.ttf")
    }

    @Test
    fun bubbleFontDisplaysEveryClockCharacter() {
        assertDisplaysClockCharacters("clock_bubble.ttf")
    }
}
