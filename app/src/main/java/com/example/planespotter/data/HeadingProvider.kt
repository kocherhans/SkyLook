package com.example.planespotter.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlin.math.*

enum class HeadingMode { SENSOR, MANUAL }

data class HeadingState(
    val degrees: Float,
    val mode: HeadingMode,
    val accuracy: Int = 3   // 0=unreliable 1=low 2=medium 3=high (SensorManager constants)
)

@Composable
fun rememberHeading(manualDegrees: Float, forceManual: Boolean = false, active: Boolean = true): HeadingState {
    val context = LocalContext.current

    val hasSensor = remember {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
        (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
         sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
    }

    if (!hasSensor || forceManual) return HeadingState(manualDegrees, HeadingMode.MANUAL)

    var degrees  by remember { mutableFloatStateOf(0f) }
    var accuracy by remember { mutableIntStateOf(3) }

    DisposableEffect(active) {
        if (!active) return@DisposableEffect onDispose {}

        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotVecSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        // Smoothed sin/cos accumulator — handles 0/360 wraparound correctly
        var smoothedSin = 0.0
        var smoothedCos = 1.0

        // Lower alpha = more smoothing, less jitter (0.05 vs old 0.08)
        val alpha = 0.05

        // Minimum change in degrees before we update state — suppresses micro-jitter
        val deadbandDeg = 0.4f

        fun pushRawRadians(rawRad: Double) {
            smoothedSin = alpha * sin(rawRad) + (1 - alpha) * smoothedSin
            smoothedCos = alpha * cos(rawRad) + (1 - alpha) * smoothedCos
            val newDeg = (Math.toDegrees(atan2(smoothedSin, smoothedCos)).toFloat() + 360f) % 360f
            val diff = abs(newDeg - degrees).let { if (it > 180f) 360f - it else it }
            if (diff > deadbandDeg) degrees = newDeg
        }

        val rotMatrix  = FloatArray(9)
        val orientation = FloatArray(3)

        val listener: SensorEventListener

        if (rotVecSensor != null) {
            // Primary path: TYPE_ROTATION_VECTOR — Android sensor fusion using
            // accelerometer + magnetometer + gyroscope. Far smoother than raw sensors.
            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                    SensorManager.getOrientation(rotMatrix, orientation)
                    pushRawRadians(orientation[0].toDouble())
                }
                override fun onAccuracyChanged(sensor: Sensor, acc: Int) {
                    accuracy = acc
                }
            }
            sm.registerListener(listener, rotVecSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            // Fallback: TYPE_ACCELEROMETER + TYPE_MAGNETIC_FIELD (no gyroscope)
            val accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
            val magSensor   = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!

            val gravity     = FloatArray(3)
            val geomagnetic = FloatArray(3)
            var hasGravity     = false
            var hasGeomagnetic = false

            // Separate low-pass for gravity so hand vibration doesn't feed into the heading
            val gravAlpha = 0.1
            var gx = 0.0; var gy = 0.0; var gz = 0.0

            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            gx = gravAlpha * event.values[0] + (1 - gravAlpha) * gx
                            gy = gravAlpha * event.values[1] + (1 - gravAlpha) * gy
                            gz = gravAlpha * event.values[2] + (1 - gravAlpha) * gz
                            gravity[0] = gx.toFloat()
                            gravity[1] = gy.toFloat()
                            gravity[2] = gz.toFloat()
                            hasGravity = true
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                            hasGeomagnetic = true
                        }
                    }
                    if (!hasGravity || !hasGeomagnetic) return
                    if (!SensorManager.getRotationMatrix(rotMatrix, null, gravity, geomagnetic)) return
                    SensorManager.getOrientation(rotMatrix, orientation)
                    pushRawRadians(orientation[0].toDouble())
                }
                override fun onAccuracyChanged(sensor: Sensor, acc: Int) {
                    if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) accuracy = acc
                }
            }
            sm.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(listener, magSensor,   SensorManager.SENSOR_DELAY_UI)
        }

        onDispose { sm.unregisterListener(listener) }
    }

    return HeadingState(degrees, HeadingMode.SENSOR, accuracy)
}
