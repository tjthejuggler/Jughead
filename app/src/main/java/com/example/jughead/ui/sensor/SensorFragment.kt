// SensorFragment.kt
package com.example.jughead.ui.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SensorViewModel : ViewModel() {
    private val _sensorReadings = mutableMapOf<Int, String>()
    val sensorReadings: Map<Int, String> get() = _sensorReadings

    fun updateReading(sensorType: Int, reading: String) {
        _sensorReadings[sensorType] = reading
    }
}

class SensorFragment : Fragment(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private val activeSensors = mutableListOf<Sensor>()
    private lateinit var viewModel: SensorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        viewModel = ViewModelProvider(this)[SensorViewModel::class.java]

        // Initialize sensors
        listOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY
        ).forEach { sensorType ->
            sensorManager.getDefaultSensor(sensorType)?.let { sensor ->
                activeSensors.add(sensor)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SensorReadingsScreen(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activeSensors.forEach { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensorName = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field"
            Sensor.TYPE_LIGHT -> "Light"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            else -> "Unknown"
        }

        val values = event.values.joinToString(", ") { "%.2f".format(it) }
        viewModel.updateReading(event.sensor.type, "$sensorName: $values")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used in this example
    }
}

@Composable
fun SensorReadingsScreen(viewModel: SensorViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        viewModel.sensorReadings.values.forEach { reading ->
            Text(
                text = reading,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}