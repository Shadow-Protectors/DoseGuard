package com.doseguard.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.repository.DoseGuardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class RosterFilter(val label: String) {
    ALL("All Workers"),
    ACTIVE_BANDS("Active Bands"),
    EXPIRED_SATURATED("Expired / Saturated"),
    ALERTS_ONLY("Hazard Alerts"),
    NO_BAND("Unassigned")
}

data class WorkerRosterItem(
    val worker: WorkerEntity,
    val activeBand: BandEntity?,
    val hasActiveAlert: Boolean,
    val activeAlertLevel: String? = null
)

data class PlantMetrics(
    val totalWorkers: Int = 0,
    val activeBandsCount: Int = 0,
    val alertCount: Int = 0,
    val totalScans: Int = 0,
    val complianceRatePercent: Double = 100.0
)

class SupervisorDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DoseGuardRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(RosterFilter.ALL)
    val selectedFilter: StateFlow<RosterFilter> = _selectedFilter.asStateFlow()

    private val _showEnrollDialog = MutableStateFlow(false)
    val showEnrollDialog: StateFlow<Boolean> = _showEnrollDialog.asStateFlow()

    val activeAlertCount: StateFlow<Int> = repo.activeAlertCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val plantMetrics: StateFlow<PlantMetrics> = combine(
        repo.allWorkersFlow,
        repo.allBandsFlow,
        repo.activeAlertCountFlow,
        repo.totalScanCountFlow
    ) { workers, bands, alertCount, scanCount ->
        val assignedBands = bands.filter { it.workerId.isNotBlank() && it.bandStatus in listOf("ACTIVE", "ASSIGNED") }
        val nonAlertWorkers = (workers.size - alertCount).coerceAtLeast(0)
        val complianceRate = if (workers.isEmpty()) 100.0 else (nonAlertWorkers.toDouble() / workers.size.toDouble()) * 100.0
        PlantMetrics(
            totalWorkers = workers.size,
            activeBandsCount = assignedBands.size,
            alertCount = alertCount,
            totalScans = scanCount,
            complianceRatePercent = complianceRate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlantMetrics())

    val rosterList: StateFlow<List<WorkerRosterItem>> = combine(
        repo.allWorkersFlow,
        repo.allBandsFlow,
        repo.activeAlertsFlow,
        _searchQuery,
        _selectedFilter
    ) { workers, bands, alerts, query, filter ->
        val alertWorkerMap = alerts.associateBy({ it.workerId }, { it.riskLevel })
        
        // Map active or latest band per worker
        val workerBandMap = mutableMapOf<String, BandEntity>()
        bands.filter { it.workerId.isNotBlank() }.forEach { band ->
            val existing = workerBandMap[band.workerId]
            if (existing == null || band.issueDate > existing.issueDate) {
                workerBandMap[band.workerId] = band
            }
        }

        val items = workers.map { worker ->
            val band = workerBandMap[worker.workerId]
            val hasAlert = alertWorkerMap.containsKey(worker.workerId)
            WorkerRosterItem(
                worker = worker,
                activeBand = band,
                hasActiveAlert = hasAlert,
                activeAlertLevel = alertWorkerMap[worker.workerId]
            )
        }

        // Apply Search Filtering
        val filteredBySearch = if (query.isBlank()) {
            items
        } else {
            val q = query.trim().lowercase()
            items.filter { item ->
                item.worker.name.lowercase().contains(q) ||
                item.worker.employeeId.lowercase().contains(q) ||
                item.worker.department.lowercase().contains(q) ||
                (item.activeBand?.bandId?.lowercase()?.contains(q) == true)
            }
        }

        // Apply Status Filter
        when (filter) {
            RosterFilter.ALL -> filteredBySearch
            RosterFilter.ACTIVE_BANDS -> filteredBySearch.filter { it.activeBand != null && it.activeBand.bandStatus in listOf("ACTIVE", "ASSIGNED") }
            RosterFilter.EXPIRED_SATURATED -> filteredBySearch.filter { 
                it.activeBand?.bandStatus == "EXPIRED" || it.activeBand?.bandStatus == "SATURATED" 
            }
            RosterFilter.ALERTS_ONLY -> filteredBySearch.filter { it.hasActiveAlert }
            RosterFilter.NO_BAND -> filteredBySearch.filter { it.activeBand == null }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: RosterFilter) {
        _selectedFilter.value = filter
    }

    fun setEnrollDialogVisible(visible: Boolean) {
        _showEnrollDialog.value = visible
    }

    fun enrollWorker(
        name: String,
        employeeId: String,
        department: String,
        designation: String,
        shift: String
    ) {
        viewModelScope.launch {
            val worker = WorkerEntity(
                workerId = "W-${UUID.randomUUID().toString().take(6).uppercase()}",
                employeeId = employeeId.trim(),
                name = name.trim(),
                department = department.trim(),
                designation = designation.trim(),
                shift = shift.trim(),
                status = "ACTIVE",
                createdAt = System.currentTimeMillis()
            )
            repo.registerWorker(worker)
            _showEnrollDialog.value = false
        }
    }
}
