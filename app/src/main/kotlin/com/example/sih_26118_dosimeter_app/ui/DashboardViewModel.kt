package com.example.sih_26118_dosimeter_app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sih_26118_dosimeter_app.database.ShiftLogEntity
import com.example.sih_26118_dosimeter_app.database.WorkerEntity
import com.example.sih_26118_dosimeter_app.network.DashboardMetricsDto
import com.example.sih_26118_dosimeter_app.repository.DoseGuardRepository
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DoseGuardRepository(application)

    val latestWorker: LiveData<WorkerEntity?> = repository.latestWorkerLiveData
    val latestLog: LiveData<ShiftLogEntity?> = repository.latestLogLiveData

    private val _metrics = MutableLiveData<DashboardMetricsDto?>()
    val metrics: LiveData<DashboardMetricsDto?> = _metrics

    private val _isSyncing = MutableLiveData<Boolean>(false)
    val isSyncing: LiveData<Boolean> = _isSyncing

    private val _syncMessage = MutableLiveData<String?>()
    val syncMessage: LiveData<String?> = _syncMessage

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _isSyncing.value = true
            val data = repository.fetchDashboardMetrics()
            _metrics.value = data
            _isSyncing.value = false
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val count = repository.syncPendingLogsToCloud()
            repository.fetchCloudLogsAndCache()
            refreshDashboard()
            _syncMessage.value = if (count > 0) {
                "Synced $count offline logs to Cloud Database"
            } else {
                "All logs up to date with Cloud"
            }
            _isSyncing.value = false
        }
    }
}
