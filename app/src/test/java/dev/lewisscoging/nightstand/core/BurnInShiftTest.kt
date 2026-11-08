package dev.lewisscoging.nightstand.core

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BurnInShiftTest {
    private val dayStart = 29_000_000L
    private val day = dayStart until dayStart + MINUTES_PER_DAY

    @Test
    fun offsetsStayInBoundsAcrossADay() {
        for (minute in day) {
            val offset = burnInOffset(minute)
            assertTrue("x out of bounds at $minute: $offset", abs(offset.x) <= BURN_IN_MAX_SHIFT_PX)
            assertTrue("y out of bounds at $minute: $offset", abs(offset.y) <= BURN_IN_MAX_SHIFT_PX)
        }
    }

    @Test
    fun offsetsStayInBoundsForEarlyAndNegativeMinutes() {
        for (minute in -100L..100L) {
            val offset = burnInOffset(minute)
            assertTrue(abs(offset.x) <= BURN_IN_MAX_SHIFT_PX)
            assertTrue(abs(offset.y) <= BURN_IN_MAX_SHIFT_PX)
        }
    }

    @Test
    fun sameMinuteGivesSameOffset() {
        for (minute in day) {
            assertEquals(burnInOffset(minute), burnInOffset(minute))
        }
    }

    @Test
    fun consecutiveMinutesDiffer() {
        for (minute in day) {
            assertNotEquals("minute $minute", burnInOffset(minute), burnInOffset(minute + 1))
        }
    }

    @Test
    fun eachAxisReachesBothSignsWithinEveryHour() {
        for (start in day) {
            val hour = (start until start + MINUTES_PER_HOUR).map(::burnInOffset)
            assertTrue("x never negative from $start", hour.any { it.x < 0 })
            assertTrue("x never positive from $start", hour.any { it.x > 0 })
            assertTrue("y never negative from $start", hour.any { it.y < 0 })
            assertTrue("y never positive from $start", hour.any { it.y > 0 })
        }
    }

    @Test
    fun offsetsSpreadAcrossManyPositions() {
        val positions = day.map(::burnInOffset).toSet()
        val gridSide = 2 * BURN_IN_MAX_SHIFT_PX + 1
        val gridSize = gridSide * gridSide
        assertTrue(
            "only ${positions.size} of $gridSize positions in a day",
            positions.size * 3 >= gridSize
        )
    }
}
