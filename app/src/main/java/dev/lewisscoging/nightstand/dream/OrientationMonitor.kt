package dev.lewisscoging.nightstand.dream

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import dev.lewisscoging.nightstand.core.LOW_PASS_ALPHA
import dev.lewisscoging.nightstand.core.ORIENTATION_STABLE_MS
import dev.lewisscoging.nightstand.core.isRestingOnLongEdge
import dev.lewisscoging.nightstand.core.lowPass
import dev.lewisscoging.nightstand.core.stableFor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

private const val TAG = "OrientationMonitor"
private const val AXIS_COUNT = 3

/** Watches the gravity direction to tell whether the phone rests on its long edge. */
class OrientationMonitor(private val sensorManager: SensorManager) {

    /**
     * Cold flow: a sensor is registered only while collected. Emits after each reading has held
     * for [ORIENTATION_STABLE_MS]. With no gravity sensor and no accelerometer it emits true once.
     */
    fun restingOnLongEdge(): Flow<Boolean> {
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val sensor = gravity ?: accelerometer
        if (sensor == null) {
            return flow {
                Log.w(TAG, "No gravity sensor or accelerometer; orientation gate passes")
                emit(true)
            }
        }
        return readings(sensor, filter = gravity == null).stableFor(ORIENTATION_STABLE_MS)
    }

    private fun readings(sensor: Sensor, filter: Boolean): Flow<Boolean> = callbackFlow {
        var filtered: FloatArray? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val current = event.values
                val vector = if (filter) {
                    val previous = filtered
                    val next = if (previous == null) {
                        current.copyOf(AXIS_COUNT)
                    } else {
                        FloatArray(AXIS_COUNT) {
                            lowPass(previous[it], current[it], LOW_PASS_ALPHA)
                        }
                    }
                    filtered = next
                    next
                } else {
                    current
                }
                trySend(isRestingOnLongEdge(vector[0], vector[1], vector[2]))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
