package dev.lewisscoging.nightstand.core

import kotlin.math.roundToInt

/** Largest distance, in pixels, the clock moves away from centre on either axis. */
const val BURN_IN_MAX_SHIFT_PX = 16

/**
 * Minutes each axis takes to sweep from one edge to the other. The two differ so the pair only
 * repeats after their combined cycle (1856 minutes), spreading wear over many positions. Both stay
 * at or below the shift span (32 px) so every minute moves each axis by at least one pixel.
 */
const val BURN_IN_X_HALF_PERIOD_MINUTES = 32
const val BURN_IN_Y_HALF_PERIOD_MINUTES = 29

data class BurnInOffset(val x: Int, val y: Int)

/** Deterministic clock offset for [epochMinute]: each axis is a triangle wave within +/-[BURN_IN_MAX_SHIFT_PX]. */
fun burnInOffset(epochMinute: Long): BurnInOffset = BurnInOffset(
    x = triangleWave(epochMinute, BURN_IN_X_HALF_PERIOD_MINUTES),
    y = triangleWave(epochMinute, BURN_IN_Y_HALF_PERIOD_MINUTES)
)

private fun triangleWave(epochMinute: Long, halfPeriod: Int): Int {
    val phase = Math.floorMod(epochMinute, 2L * halfPeriod).toInt()
    val distanceFromStart = if (phase <= halfPeriod) phase else 2 * halfPeriod - phase
    val span = 2 * BURN_IN_MAX_SHIFT_PX
    return (span.toFloat() * distanceFromStart / halfPeriod).roundToInt() - BURN_IN_MAX_SHIFT_PX
}
