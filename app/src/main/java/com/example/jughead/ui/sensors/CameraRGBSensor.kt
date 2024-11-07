package com.example.jughead.ui.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.media.Image
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.view.PreviewView
import java.nio.ByteBuffer
import android.os.Handler
import android.os.Looper

class CameraRGBSensor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onRGBValuesUpdated: (FloatArray) -> Unit
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Set up Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageAnalyzer = ImageAnalysis.Builder()
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { image ->
                        processImage(image)
                        image.close()
                    }
                }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA, // Changed to front camera
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("CameraRGBSensor", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImage(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var pixelCount = 0

        // Process every 4th pixel for performance (RGBA format)
        for (i in 0 until data.size step 16) {
            if (i + 3 >= data.size) break
            
            val r = data[i].toInt() and 0xff
            val g = data[i + 1].toInt() and 0xff
            val b = data[i + 2].toInt() and 0xff
            
            totalR += r
            totalG += g
            totalB += b
            pixelCount++
        }

        if (pixelCount > 0) {
            val avgR = totalR.toFloat() / pixelCount / 255f  // Normalize to 0-1 range
            val avgG = totalG.toFloat() / pixelCount / 255f
            val avgB = totalB.toFloat() / pixelCount / 255f

            // Post to main thread
            mainHandler.post {
                onRGBValuesUpdated(floatArrayOf(avgR, avgG, avgB))
            }
        }
    }

    fun release() {
        cameraExecutor.shutdown()
        imageAnalyzer = null
        cameraProvider?.unbindAll()
    }

    companion object {
        // Custom sensor type for RGB values
        const val TYPE_CAMERA_RGB = Sensor.TYPE_ALL + 1
    }
}
