package dev.lewisscoging.nightstand.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

const val LONG_EDGE_MAX_TILT_DEG = 25.0
const val MIN_SCREEN_TILT_FROM_FLAT_DEG = 30.0

/** Weight kept from the previous reading when low-pass filtering the accelerometer into gravity. */
const val LOW_PASS_ALPHA = 0.8f

/** Below this magnitude (m/s^2) the reading carries no usable direction, for example in free fall. */
const val MIN_GRAVITY_MAGNITUDE = 1.0f

private val maxLongAxisRatio = sin(Math.toRadians(LONG_EDGE_MAX_TILT_DEG))
private val maxScreenNormalRatio = cos(Math.toRadians(MIN_SCREEN_TILT_FROM_FLAT_DEG))

/**
 * True when the phone rests on its long edge: the long axis (y) is within [LONG_EDGE_MAX_TILT_DEG]
 * of horizontal and the screen (z) is tilted at least [MIN_SCREEN_TILT_FROM_FLAT_DEG] away from flat.
 * Takes a gravity vector in device sensor coordinates.
 */
fun isRestingOnLongEdge(x: Float, y: Float, z: Float): Boolean {
    val magnitude = sqrt(x * x + y * y + z * z)
    if (magnitude < MIN_GRAVITY_MAGNITUDE) return false
    val longAxisNearHorizontal = abs(y) / magnitude <= maxLongAxisRatio
    val screenOffFlat = abs(z) / magnitude <= maxScreenNormalRatio
    return longAxisNearHorizontal && screenOffFlat
}

/** Keeps [alpha] of [previous] and blends in the rest from [current]. */
fun lowPass(previous: Float, current: Float, alpha: Float): Float =
    alpha * previous + (1f - alpha) * current
