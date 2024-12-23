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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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

    fun hasChanged(): Boolean {
        return minValue != Float.POSITIVE_INFINITY && 
               maxValue != Float.NEGATIVE_INFINITY && 
               minValue != maxValue  // Check if the range actually varies
    }
}

data class SensorReading(
    val name: String,
    val values: List<Float>,
    val ranges: List<SensorValueRange>
)

enum class SensorCategory(val title: String) {
    VISION("Vision"),
    MOVEMENT("Movement"),
    POSITION("Position"),
    ENVIRONMENT("Environment")
}

fun Sensor.getCategory(): SensorCategory = when (this.type) {
    // Vision sensors
    Sensor.TYPE_LIGHT,
    Sensor.TYPE_PROXIMITY,
    65578, // WideIR Light
    65579, // NarrowIR Light
    65580  // ColorIR Light
    -> SensorCategory.VISION
    
    // Movement sensors
    Sensor.TYPE_ACCELEROMETER,
    Sensor.TYPE_GYROSCOPE,
    Sensor.TYPE_LINEAR_ACCELERATION,
    Sensor.TYPE_STEP_COUNTER,
    Sensor.TYPE_STEP_DETECTOR,
    Sensor.TYPE_SIGNIFICANT_MOTION,
    Sensor.TYPE_GRAVITY
    -> SensorCategory.MOVEMENT
    
    // Position sensors
    Sensor.TYPE_MAGNETIC_FIELD,
    Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
    Sensor.TYPE_ORIENTATION,
    Sensor.TYPE_ROTATION_VECTOR,
    Sensor.TYPE_GAME_ROTATION_VECTOR,
    Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
    -> SensorCategory.POSITION
    
    // Environment sensors
    Sensor.TYPE_PRESSURE,
    Sensor.TYPE_AMBIENT_TEMPERATURE,
    Sensor.TYPE_RELATIVE_HUMIDITY
    -> SensorCategory.ENVIRONMENT
    
    else -> when {
        // Additional categorization based on sensor name
        this.name.contains("gyro", ignoreCase = true) -> SensorCategory.MOVEMENT
        this.name.contains("accel", ignoreCase = true) -> SensorCategory.MOVEMENT
        this.name.contains("light", ignoreCase = true) -> SensorCategory.VISION
        this.name.contains("ir", ignoreCase = true) -> SensorCategory.VISION
        this.name.contains("motion", ignoreCase = true) -> SensorCategory.MOVEMENT
        this.name.contains("magnetic", ignoreCase = true) -> SensorCategory.POSITION
        this.name.contains("orientation", ignoreCase = true) -> SensorCategory.POSITION
        this.name.contains("rotation", ignoreCase = true) -> SensorCategory.POSITION
        this.name.contains("temp", ignoreCase = true) -> SensorCategory.ENVIRONMENT
        this.name.contains("humidity", ignoreCase = true) -> SensorCategory.ENVIRONMENT
        this.name.contains("pressure", ignoreCase = true) -> SensorCategory.ENVIRONMENT
        else -> SensorCategory.POSITION
    }
}

class SensorViewModel(private val context: Context) : ViewModel() {
    private val _sensorReadings = MutableLiveData<Map<Int, SensorReading>>()
    val sensorReadings: LiveData<Map<Int, SensorReading>> = _sensorReadings

    private val readings = mutableMapOf<Int, SensorReading>()
    private val sensorRanges = mutableMapOf<Int, List<SensorValueRange>>()
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        // Initialize with empty map
        _sensorReadings.value = emptyMap()

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

    fun isActiveSensor(sensorType: Int): Boolean {
        // Camera RGB sensor is always considered active
        if (sensorType == CameraRGBSensor.TYPE_CAMERA_RGB) {
            return true
        }
        // Other sensors are active only if they have changed values
        return readings[sensorType]?.ranges?.any { it.hasChanged() } == true
    }

    fun getSensorManager(): SensorManager {
        return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
}

class SensorViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SensorViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SensorFragment : Fragment(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private val activeSensors = mutableListOf<Sensor>()
    private val viewModel: SensorViewModel by viewModels { 
        SensorViewModelFactory(requireContext())
    }
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
private fun CollapsibleSection(
    title: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }  // Changed to false by default
    val rotationState by animateFloatAsState(if (expanded) 180f else 0f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationState),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        AnimatedVisibility(visible = expanded) {
            content()
        }
        
        Spacer(modifier = Modifier.height(8.dp))
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
            drawLine(
                color = Color.Gray,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            val position = (value - currentRange.first) / (currentRange.second - currentRange.first)
            val x = position * size.width
            
            drawCircle(
                color = Color.Green,
                radius = 8f,
                center = Offset(x.coerceIn(0f, size.width), size.height / 2)
            )
        }

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
    var hideUnchangedSensors by remember { mutableStateOf(true) }
    
    val groupedSensors = remember(readings, hideUnchangedSensors) {
        readings.entries
            .filter { (sensorType, reading) ->
                !hideUnchangedSensors || reading.ranges.any { it.hasChanged() }
            }
            .groupBy { (sensorType, _) ->
                when (sensorType) {
                    CameraRGBSensor.TYPE_CAMERA_RGB -> SensorCategory.VISION
                    else -> {
                        val sensorManager = viewModel.getSensorManager()
                        val sensor = sensorManager.getSensorList(Sensor.TYPE_ALL).find { it.type == sensorType }
                        sensor?.getCategory() ?: SensorCategory.POSITION
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SensorCategory.values().forEach { category ->
                val sensorsInCategory = groupedSensors[category] ?: emptyList()
                if (sensorsInCategory.isNotEmpty()) {
                    CollapsibleSection(title = category.title) {
                        Column {
                            sensorsInCategory.forEach { (sensorType, reading) ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    val shortName = when {
                                        // First check specific sensor types
                                        sensorType == Sensor.TYPE_ACCELEROMETER -> "Accel"
                                        sensorType == Sensor.TYPE_GYROSCOPE -> "Gyro"
                                        sensorType == Sensor.TYPE_LINEAR_ACCELERATION -> "LinAccel"
                                        sensorType == Sensor.TYPE_STEP_COUNTER -> "Steps"
                                        sensorType == Sensor.TYPE_STEP_DETECTOR -> "StepDet"
                                        sensorType == Sensor.TYPE_SIGNIFICANT_MOTION -> "SigMot"
                                        sensorType == Sensor.TYPE_GRAVITY -> "Grav"
                                        sensorType == Sensor.TYPE_MAGNETIC_FIELD -> "Mag"
                                        sensorType == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "MagUncal"
                                        sensorType == Sensor.TYPE_ORIENTATION -> "Orient"
                                        sensorType == Sensor.TYPE_ROTATION_VECTOR -> "Rot"
                                        sensorType == Sensor.TYPE_GAME_ROTATION_VECTOR -> "GameRot"
                                        sensorType == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "GeoRot"
                                        sensorType == Sensor.TYPE_LIGHT -> "Light"
                                        sensorType == Sensor.TYPE_PROXIMITY -> "Prox"
                                        sensorType == CameraRGBSensor.TYPE_CAMERA_RGB -> "RGB"
                                        sensorType == Sensor.TYPE_PRESSURE -> "Press"
                                        sensorType == Sensor.TYPE_AMBIENT_TEMPERATURE -> "Temp"
                                        sensorType == Sensor.TYPE_RELATIVE_HUMIDITY -> "Humid"
                                        
                                        // Then check special sensor types by their numeric values
                                        sensorType == 65578 -> "WideIR"
                                        sensorType == 65579 -> "NarrowIR"
                                        sensorType == 65580 -> "ColorIR"
                                        
                                        // For any other sensors, derive a name from their full name
                                        else -> {
                                            val fullName = reading.name.lowercase()
                                            when {
                                                fullName.contains("gyroscope") -> "Gyro${if(fullName.contains("uncalibrated")) "Uncal" else ""}"
                                                fullName.contains("accelerometer") -> "Accel${if(fullName.contains("uncalibrated")) "Uncal" else ""}"
                                                fullName.contains("magnetometer") -> "Mag${if(fullName.contains("uncalibrated")) "Uncal" else ""}"
                                                fullName.contains("light") -> when {
                                                    fullName.contains("wide") -> "WideIR"
                                                    fullName.contains("narrow") -> "NarrowIR"
                                                    fullName.contains("color") -> "ColorIR"
                                                    else -> "Light"
                                                }
                                                fullName.contains("proximity") -> "Prox"
                                                fullName.contains("rotation") -> when {
                                                    fullName.contains("game") -> "GameRot"
                                                    fullName.contains("geomagnetic") -> "GeoRot"
                                                    else -> "Rot"
                                                }
                                                fullName.contains("gravity") -> "Grav"
                                                fullName.contains("orientation") -> "Orient"
                                                fullName.contains("temperature") -> "Temp"
                                                fullName.contains("humidity") -> "Humid"
                                                fullName.contains("pressure") -> "Press"
                                                else -> "Sensor${sensorType}" // Fallback with type number
                                            }
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = shortName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = reading.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                    }

                                    if (sensorType == CameraRGBSensor.TYPE_CAMERA_RGB && (!hideUnchangedSensors || reading.ranges.any { it.hasChanged() })) {
                                        AndroidView(
                                            factory = { context ->
                                                PreviewView(context).apply {
                                                    this.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                                                    this.scaleType = PreviewView.ScaleType.FIT_CENTER
                                                    layoutParams = ViewGroup.LayoutParams(
                                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                                        300
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
                                        if (index < reading.ranges.size && (!hideUnchangedSensors || reading.ranges[index].hasChanged())) {
                                                val axis = when {
                                                    sensorType == CameraRGBSensor.TYPE_CAMERA_RGB -> when(index) {
                                                        0 -> "R"
                                                        1 -> "G"
                                                        2 -> "B"
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
                                }
                            }
                        }
                    }
                }
            }
        }

        // Checkbox at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = hideUnchangedSensors,
                onCheckedChange = { hideUnchangedSensors = it }
            )
            Text(
                text = "Hide unchanged sensors",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
