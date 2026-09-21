package com.example.sih_26118_dosimeter_app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.sih_26118_dosimeter_app.database.ShiftLogEntity
import com.example.sih_26118_dosimeter_app.repository.DoseGuardRepository
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DoseGuardRepository(application)

    private val _currentFilter = MutableLiveData<String>("ALL")
    val currentFilter: LiveData<String> = _currentFilter

    val logs: LiveData<List<ShiftLogEntity>> = _currentFilter.switchMap { filter ->
        repository.getLogsByRiskLiveData(filter)
    }

    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _syncCount = MutableLiveData<Int?>(null)
    val syncCount: LiveData<Int?> = _syncCount

    init {
        refreshFromCloud()
    }

    fun setFilter(filter: String) {
        _currentFilter.value = filter
    }

    fun refreshFromCloud() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // First push any pending offline records
            val pushed = repository.syncPendingLogsToCloud()
            // Then pull latest records from the cloud
            repository.fetchCloudLogsAndCache()
            _syncCount.value = pushed
            _isRefreshing.value = false
        }
    }
}
