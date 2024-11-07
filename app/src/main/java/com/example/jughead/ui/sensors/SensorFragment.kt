package com.example.jughead.ui.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.max
import kotlin.math.min

data class SensorValueRange(
    var minValue: Float = Float.POSITIVE_INFINITY,
    var maxValue: Float = Float.NEGATIVE_INFINITY
) {
    fun update(value: Float) {
        minValue = min(minValue, value)
        maxValue = max(maxValue, value)
    }

    fun getRange(): Pair<Float, Float> {
        if (minValue == Float.POSITIVE_INFINITY || maxValue == Float.NEGATIVE_INFINITY) {
            return Pair(0f, 1f)  // Default range for RGB values
        }
        val range = maxValue - minValue
        val padding = range * 0.1f
        return Pair(minValue - padding, maxValue + padding)
    }
}

data class SensorReading(
    val name: String,
    val values: List<Float>,
    val ranges: List<SensorValueRange>
)

class SensorViewModel : ViewModel() {
    private val _sensorReadings = MutableLiveData<Map<Int, SensorReading>>()
    val sensorReadings: LiveData<Map<Int, SensorReading>> = _sensorReadings

    private val readings = mutableMapOf<Int, SensorReading>()
    private val sensorRanges = mutableMapOf<Int, List<SensorValueRange>>()
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        // Initialize RGB sensor with default values
        val rgbRanges = List(3) { SensorValueRange() }
        readings[CameraRGBSensor.TYPE_CAMERA_RGB] = SensorReading(
            name = "Camera RGB Sensor",
            values = listOf(0f, 0f, 0f),
            ranges = rgbRanges
        )
        sensorRanges[CameraRGBSensor.TYPE_CAMERA_RGB] = rgbRanges
        _sensorReadings.value = readings.toMap()
    }

    private fun getOrCreateRanges(sensorType: Int, valueCount: Int): List<SensorValueRange> {
        return sensorRanges.getOrPut(sensorType) {
            List(valueCount) { SensorValueRange() }
        }
    }

    fun updateReading(sensor: Sensor, values: FloatArray) {
        mainHandler.post {
            val ranges = getOrCreateRanges(sensor.type, values.size)
            values.forEachIndexed { index, value ->
                ranges[index].update(value)
            }

            readings[sensor.type] = SensorReading(
                name = sensor.name,
                values = values.toList(),
                ranges = ranges
            )
            _sensorReadings.value = readings.toMap()
        }
    }

    fun updateCameraRGBReading(values: FloatArray) {
        mainHandler.post {
            val ranges = getOrCreateRanges(CameraRGBSensor.TYPE_CAMERA_RGB, 3)
            values.forEachIndexed { index, value ->
                ranges[index].update(value)
            }

            readings[CameraRGBSensor.TYPE_CAMERA_RGB] = SensorReading(
                name = "Camera RGB Sensor",
                values = values.toList(),
                ranges = ranges
            )
            _sensorReadings.value = readings.toMap()
        }
    }
}

class SensorFragment : Fragment(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private val activeSensors = mutableListOf<Sensor>()
    private val viewModel: SensorViewModel by viewModels()
    private var cameraRGBSensor: CameraRGBSensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sensorManager.getSensorList(Sensor.TYPE_ALL).forEach { sensor ->
            activeSensors.add(sensor)
            Log.d("SensorFragment", "Found sensor: ${sensor.name} (Type: ${sensor.type})")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SensorReadingsScreen(viewModel) { previewView ->
                            // Initialize camera RGB sensor with the PreviewView
                            if (cameraRGBSensor == null) {
                                cameraRGBSensor = CameraRGBSensor(
                                    requireContext(),
                                    viewLifecycleOwner,
                                    previewView
                                ) { rgbValues ->
                                    viewModel.updateCameraRGBReading(rgbValues)
                                }
                            }
                        }
                    }
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        cameraRGBSensor?.release()
        cameraRGBSensor = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        viewModel.updateReading(event.sensor, event.values)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used in this example
    }
}

@Composable
private fun SensorRangeIndicator(
    value: Float,
    range: SensorValueRange,
    modifier: Modifier = Modifier
) {
    val currentRange = range.getRange()
    
    Box(modifier = modifier.height(30.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw background line
            drawLine(
                color = Color.Gray,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Calculate marker position
            val position = (value - currentRange.first) / (currentRange.second - currentRange.first)
            val x = position * size.width
            
            // Draw marker
            drawCircle(
                color = Color.Green,
                radius = 8f,
                center = Offset(x.coerceIn(0f, size.width), size.height / 2)
            )
        }

        // Draw range values
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format("%.2f", currentRange.first),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = String.format("%.2f", currentRange.second),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun SensorReadingsScreen(
    viewModel: SensorViewModel,
    onPreviewCreated: (PreviewView) -> Unit
) {
    val readings: Map<Int, SensorReading> by viewModel.sensorReadings.observeAsState(emptyMap())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        readings.entries.sortedBy { 
            // Put RGB sensor at the top
            if (it.key == CameraRGBSensor.TYPE_CAMERA_RGB) -1 else it.key 
        }.forEach { (sensorType, reading) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = reading.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                // Add camera preview for RGB sensor
                if (sensorType == CameraRGBSensor.TYPE_CAMERA_RGB) {
                    AndroidView(
                        factory = { context ->
                            PreviewView(context).apply {
                                this.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                this.scaleType = PreviewView.ScaleType.FILL_CENTER
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    300 // Taller preview in the list
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(vertical = 8.dp)
                    ) { previewView ->
                        onPreviewCreated(previewView)
                    }
                }
                
                reading.values.forEachIndexed { index, value ->
                    if (index < reading.ranges.size) {
                        val axis = when {
                            sensorType == CameraRGBSensor.TYPE_CAMERA_RGB -> when(index) {
                                0 -> "Red"
                                1 -> "Green"
                                2 -> "Blue"
                                else -> index.toString()
                            }
                            else -> when(index) {
                                0 -> "X"
                                1 -> "Y"
                                2 -> "Z"
                                else -> index.toString()
                            }
                        }
                        
                        Text(
                            text = "$axis: ${String.format("%.2f", value)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        SensorRangeIndicator(
                            value = value,
                            range = reading.ranges[index],
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
