package com.doseguard.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doseguard.app.model.AlertEntity
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.ExposureHistoryEntity
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.repository.DoseGuardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DoseGuardRepository(application)

    private val _workerId = MutableStateFlow<String?>(null)

    /** All history for the selected worker, reactive. */
    val historyFlow: StateFlow<List<ExposureHistoryEntity>> = _workerId
        .filterNotNull()
        .flatMapLatest { repo.getHistoryByWorkerFlow(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Last 7 days for trend chart. */
    val weeklyFlow: StateFlow<List<ExposureHistoryEntity>> = _workerId
        .filterNotNull()
        .flatMapLatest { id ->
            val since = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
            repo.getRecentHistoryFlow(id, since)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val worker = MutableStateFlow<WorkerEntity?>(null)
    val band   = MutableStateFlow<BandEntity?>(null)

    fun load(workerId: String, bandId: String) {
        _workerId.value = workerId
        viewModelScope.launch {
            worker.value = repo.getWorkerById(workerId)
            band.value   = repo.getBandById(bandId)
        }
    }
}
