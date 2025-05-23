// SensorLoggerViewModel.kt
package com.example.mythomash.ui.location

import android.app.Application
import androidx.lifecycle.*
import com.example.mythomash.ui.location.AppDatabase
import com.example.mythomash.ui.location.LocationLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SensorLoggerViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: LocationLogDao = AppDatabase.getInstance(application).locationLogDao()

    val allLogs: LiveData<List<LocationLog>> = dao.getAllLogs()

    fun insertLog(log: LocationLog) {
        viewModelScope.launch {
            dao.insert(log)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }
}

