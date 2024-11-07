package com.example.jughead.ui.gesturemapping

import android.app.Application
import androidx.lifecycle.*
import com.example.jughead.data.AppDatabase
import com.example.jughead.data.GestureMapping
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GestureMappingViewModel(application: Application) : AndroidViewModel(application) {

    private val gestureMappingDao = AppDatabase.getDatabase(application).gestureMappingDao()

    private val _gestureMappings = MutableLiveData<List<GestureMapping>>()
    val gestureMappings: LiveData<List<GestureMapping>> get() = _gestureMappings

    init {
        loadMappings()
    }

    private fun loadMappings() {
        viewModelScope.launch {
            gestureMappingDao.getAllMappings().collectLatest { mappings ->
                _gestureMappings.value = mappings
            }
        }
    }

    fun insertMapping(gestureName: String, commandName: String) {
        viewModelScope.launch {
            val mapping = GestureMapping(gestureName = gestureName, commandName = commandName)
            gestureMappingDao.insertMapping(mapping)
            // No need to call loadMappings() as Flow will automatically emit new values
        }
    }

    fun getCommandForGesture(gestureName: String, callback: (String?) -> Unit) {
        viewModelScope.launch {
            val mapping = gestureMappingDao.getMappingByGesture(gestureName)
            callback(mapping?.commandName)
        }
    }
}
