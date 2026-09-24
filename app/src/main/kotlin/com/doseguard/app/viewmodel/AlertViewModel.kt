package com.doseguard.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doseguard.app.model.AlertEntity
import com.doseguard.app.repository.DoseGuardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DoseGuardRepository(application)

    val activeAlerts: StateFlow<List<AlertEntity>> = repo.activeAlertsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeCount: StateFlow<Int> = repo.activeAlertCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun acknowledge(alertId: String) {
        viewModelScope.launch { repo.acknowledgeAlert(alertId) }
    }

    fun resolve(alertId: String) {
        viewModelScope.launch { repo.resolveAlert(alertId) }
    }

    fun acknowledgeAllForWorker(workerId: String) {
        viewModelScope.launch { repo.acknowledgeAllForWorker(workerId) }
    }
}
