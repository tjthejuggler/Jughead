package com.example.jughead.ui.mic

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MicViewModel : ViewModel() {
    private val _isListening = MutableLiveData<Boolean>().apply { value = false }
    val isListening: LiveData<Boolean> = _isListening

    private val _targetWord = MutableLiveData<String>()
    val targetWord: LiveData<String> = _targetWord

    private val _status = MutableLiveData<String>().apply { value = "Microphone is off" }
    val status: LiveData<String> = _status

    fun toggleListening() {
        _isListening.value = _isListening.value?.not() ?: false
        _status.value = if (_isListening.value == true) "Listening..." else "Microphone is off"
    }

    fun setTargetWord(word: String) {
        _targetWord.value = word
    }

    fun updateStatus(newStatus: String) {
        _status.value = newStatus
    }
}
