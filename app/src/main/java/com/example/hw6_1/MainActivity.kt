package com.example.hw6_1

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hw6_1.ui.theme.HW6_1Theme
import kotlin.math.pow

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private var currentPressure by mutableStateOf(1013.25f) // Default sea level pressure
    private var currentAltitude by mutableStateOf(0f)
    private var isSimulationMode by mutableStateOf(true) // Always start with simulation mode enabled

    companion object {
        private const val P0 = 1013.25f // Sea level standard atmospheric pressure in hPa
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)


        setContent {
            HW6_1Theme {
                AltimeterScreen(
                    pressure = currentPressure,
                    altitude = currentAltitude,
                    isSimulationMode = isSimulationMode,
                    onIncreaseAltitude = { simulatePressureChange(-10f) }, // Decrease pressure = increase altitude
                    onDecreaseAltitude = { simulatePressureChange(10f) }   // Increase pressure = decrease altitude
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pressureSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Only process sensor data if not in simulation mode
        if (!isSimulationMode) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_PRESSURE) {
                    currentPressure = it.values[0]
                    currentAltitude = calculateAltitude(currentPressure)
                }
            }
        }
    }


    private fun calculateAltitude(pressure: Float): Float {
        // Formula: h = 44330 × (1 - (P/P0)^(1/5.255))
        return 44330 * (1 - (pressure / P0).pow(1 / 5.255f))
    }

    // Simulate pressure changes for testing on emulator
    private fun simulatePressureChange(delta: Float) {
        currentPressure = (currentPressure + delta).coerceIn(800f, 1100f)
        currentAltitude = calculateAltitude(currentPressure)
    }
}

@Composable
fun AltimeterScreen(
    pressure: Float,
    altitude: Float,
    isSimulationMode: Boolean = false,
    onIncreaseAltitude: () -> Unit = {},
    onDecreaseAltitude: () -> Unit = {}
) {
    // Higher altitude = darker color
    val backgroundColor = calculateBackgroundColor(altitude)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Altimeter",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Altitude
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Altitude",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = String.format("%.2f m", altitude),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Pressure
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pressure",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = String.format("%.2f hPa", pressure),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            // Simulation Mode Controls
            if (isSimulationMode) {
                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Simulation Mode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = onIncreaseAltitude,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("+100m", fontSize = 16.sp)
                    }

                    Button(
                        onClick = onDecreaseAltitude,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Text("100m", fontSize = 16.sp)
                    }
                }

            }

            // Additional info
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Sea Level Reference: 1013.25 hPa",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

private fun calculateBackgroundColor(altitude: Float): Color {
    // Normalize altitude to a range (e.g., 0 to 3000 meters)
    val normalizedAltitude = (altitude.coerceIn(-500f, 3000f) + 500f) / 3500f

    val red = (100 * (1 - normalizedAltitude * 0.8f)).toInt().coerceIn(0, 255)
    val green = (150 * (1 - normalizedAltitude * 0.7f)).toInt().coerceIn(0, 255)
    val blue = (200 * (1 - normalizedAltitude * 0.5f)).toInt().coerceIn(0, 255)

    return Color(red, green, blue)
}

