package com.example.jughead.ui.gesturemapping

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GestureMappingViewModel : ViewModel() {
    val gestureMappings = MutableLiveData<Map<String, String>>()

    fun updateMapping(gestureName: String, command: String) {
        val currentMappings = gestureMappings.value?.toMutableMap() ?: mutableMapOf()
        currentMappings[gestureName] = command
        gestureMappings.value = currentMappings
    }
}
