package dev.lewisscoging.nightstand.core

import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrientationMathTest {
    private val g = 9.81f

    /**
     * Gravity for a phone on its long edge whose screen is [screenTiltFromFlatDeg] away from lying flat
     * (90 = upright), with the long axis tilted [longAxisTiltDeg] from horizontal.
     */
    private fun restingOnLongEdge(
        screenTiltFromFlatDeg: Double,
        longAxisTiltDeg: Double = 0.0
    ): Boolean {
        val screenTilt = Math.toRadians(screenTiltFromFlatDeg)
        val longAxisTilt = Math.toRadians(longAxisTiltDeg)
        val inScreenPlane = g * sin(screenTilt)
        val x = (inScreenPlane * cos(longAxisTilt)).toFloat()
        val y = (inScreenPlane * sin(longAxisTilt)).toFloat()
        val z = (g * cos(screenTilt)).toFloat()
        return isRestingOnLongEdge(x, y, z)
    }

    @Test
    fun uprightLandscapeOnTheLeftEdgePasses() = assertTrue(isRestingOnLongEdge(g, 0f, 0f))

    @Test
    fun uprightLandscapeOnTheRightEdgePasses() = assertTrue(isRestingOnLongEdge(-g, 0f, 0f))

    @Test
    fun reclinedAt60DegreesPasses() = assertTrue(restingOnLongEdge(screenTiltFromFlatDeg = 60.0))

    @Test
    fun reclinedAt35DegreesPasses() = assertTrue(restingOnLongEdge(screenTiltFromFlatDeg = 35.0))

    @Test
    fun screen25DegreesFromFlatFails() =
        assertFalse(restingOnLongEdge(screenTiltFromFlatDeg = 25.0))

    @Test
    fun flatOnItsBackFails() = assertFalse(isRestingOnLongEdge(0f, 0f, g))

    @Test
    fun faceDownFails() = assertFalse(isRestingOnLongEdge(0f, 0f, -g))

    @Test
    fun uprightPortraitFails() = assertFalse(isRestingOnLongEdge(0f, g, 0f))

    @Test
    fun upsideDownPortraitFails() = assertFalse(isRestingOnLongEdge(0f, -g, 0f))

    @Test
    fun longAxisTilted20DegreesPasses() =
        assertTrue(restingOnLongEdge(screenTiltFromFlatDeg = 90.0, longAxisTiltDeg = 20.0))

    @Test
    fun longAxisTilted30DegreesFails() =
        assertFalse(restingOnLongEdge(screenTiltFromFlatDeg = 90.0, longAxisTiltDeg = 30.0))

    @Test
    fun zeroVectorFails() = assertFalse(isRestingOnLongEdge(0f, 0f, 0f))

    @Test
    fun belowMinimumMagnitudeFails() =
        assertFalse(isRestingOnLongEdge(MIN_GRAVITY_MAGNITUDE / 2, 0f, 0f))

    @Test
    fun lowPassKeepsAlphaOfThePreviousValue() =
        assertEquals(8.2f, lowPass(10f, 1f, LOW_PASS_ALPHA), 1e-5f)

    @Test
    fun lowPassWithZeroAlphaReturnsTheCurrentValue() = assertEquals(3f, lowPass(10f, 3f, 0f), 0f)

    @Test
    fun lowPassWithFullAlphaReturnsThePreviousValue() = assertEquals(10f, lowPass(10f, 3f, 1f), 0f)
}
