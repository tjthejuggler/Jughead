package com.example.jughead.ui.balls

import android.graphics.Color
import android.widget.Toast
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class BallsViewModel(application: Application) : AndroidViewModel(application) {
    private val _ballIpAddresses = MutableLiveData<Map<Int, String>>().apply {
        value = mapOf(
            1 to "",
            2 to "",
            3 to "",
            4 to ""
        )
    }
    val ballIpAddresses: LiveData<Map<Int, String>> = _ballIpAddresses

    private val _ballColors = MutableLiveData<Map<Int, Int>>().apply {
        value = mapOf(
            1 to Color.WHITE,
            2 to Color.WHITE,
            3 to Color.WHITE,
            4 to Color.WHITE
        )
    }
    val ballColors: LiveData<Map<Int, Int>> = _ballColors

    private val _ballConnectionStates = MutableLiveData<Map<Int, Boolean>>().apply {
        value = mapOf(
            1 to false,
            2 to false,
            3 to false,
            4 to false
        )
    }
    val ballConnectionStates: LiveData<Map<Int, Boolean>> = _ballConnectionStates

    fun updateBallIpAddress(ballNumber: Int, ipAddress: String) {
        val currentAddresses = _ballIpAddresses.value?.toMutableMap() ?: mutableMapOf()
        currentAddresses[ballNumber] = ipAddress
        _ballIpAddresses.value = currentAddresses

        // Update connection state
        val currentStates = _ballConnectionStates.value?.toMutableMap() ?: mutableMapOf()
        currentStates[ballNumber] = ipAddress.isNotEmpty()
        _ballConnectionStates.value = currentStates
    }

    fun updateBallColor(ballNumber: Int, color: Int) {
        val currentColors = _ballColors.value?.toMutableMap() ?: mutableMapOf()
        currentColors[ballNumber] = color
        _ballColors.value = currentColors

        // Send color change command to the ball
        val ipAddress = _ballIpAddresses.value?.get(ballNumber)
        if (!ipAddress.isNullOrEmpty()) {
            sendColorChange(ipAddress, color)
        }
    }

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private fun sendColorChange(ipAddress: String, color: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                DatagramSocket().use { socket ->
                    socket.soTimeout = 1000 // 1 second timeout
                    val buffer = ByteArray(12)

                    // Command format from mymyonnaise project
                    buffer[0] = 66 // 'B' for Ball
                    buffer[8] = 0x0a.toByte() // Command for color change
                    buffer[9] = Color.red(color).toByte()
                    buffer[10] = Color.green(color).toByte()
                    buffer[11] = Color.blue(color).toByte()

                    val packet = DatagramPacket(
                        buffer,
                        buffer.size,
                        InetAddress.getByName(ipAddress),
                        41412 // Port number from mymyonnaise
                    )
                    socket.send(packet)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            getApplication(),
                            "Color sent to $ipAddress",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMsg = when {
                        e.message?.contains("failed to connect", ignoreCase = true) == true -> 
                            "Failed to connect to ball at $ipAddress"
                        e.message?.contains("network is unreachable", ignoreCase = true) == true -> 
                            "Network is unreachable. Check your connection."
                        e.message?.contains("timed out", ignoreCase = true) == true -> 
                            "Connection timed out. Ball at $ipAddress not responding."
                        else -> "Error sending color to ball at $ipAddress: ${e.message}"
                    }
                    _errorMessage.value = errorMsg
                    Toast.makeText(getApplication(), errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun getBallColor(ballNumber: Int): Int {
        return _ballColors.value?.get(ballNumber) ?: Color.WHITE
    }

    fun isBallConnected(ballNumber: Int): Boolean {
        return _ballConnectionStates.value?.get(ballNumber) ?: false
    }
}
