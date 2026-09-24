package com.doseguard.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.ExposureHistoryEntity
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.repository.DoseGuardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DoseGuardRepository(application)

    enum class TimeRange(val label: String, val days: Int) {
        WEEK_7_DAYS("7-Day Trend", 7),
        MONTH_30_DAYS("30-Day Monthly", 30),
        ALL_TIME("All Logs", 365)
    }

    private val _workerId = MutableStateFlow<String?>(null)
    private val _bandId = MutableStateFlow<String?>(null)
    val selectedTimeRange = MutableStateFlow(TimeRange.WEEK_7_DAYS)

    val historyFlow: StateFlow<List<ExposureHistoryEntity>> = _workerId
        .filterNotNull()
        .flatMapLatest { repo.getHistoryByWorkerFlow(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recent exposure trend based on selected time range (7-day or 30-day). */
    val trendFlow: StateFlow<List<ExposureHistoryEntity>> = combine(_workerId.filterNotNull(), selectedTimeRange) { id, range ->
        Pair(id, range)
    }.flatMapLatest { (id, range) ->
        val since = System.currentTimeMillis() - (range.days.toLong() * 24 * 3600 * 1000)
        repo.getRecentHistoryFlow(id, since)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val worker = MutableStateFlow<WorkerEntity?>(null)
    val band   = MutableStateFlow<BandEntity?>(null)

    fun load(workerId: String, bandId: String = "") {
        _workerId.value = workerId
        _bandId.value = if (bandId == "all" || bandId == "none") "" else bandId

        viewModelScope.launch {
            worker.value = repo.getWorkerById(workerId)
            if (_bandId.value?.isNotBlank() == true) {
                band.value = repo.getBandById(_bandId.value!!)
            } else {
                // Look up latest active band for worker
                val allBands = repo.allBandsFlow.firstOrNull() ?: emptyList()
                band.value = allBands.firstOrNull { it.workerId == workerId }
            }
        }
    }

    fun setTimeRange(range: TimeRange) {
        selectedTimeRange.value = range
    }
}
