package com.doseguard.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doseguard.app.model.BandAssignmentEntity
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.repository.DoseGuardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * BandAssignmentViewModel — manages the worker-first dosimeter band assignment flow.
 * Implements a single sealed UiState hierarchy with reactive state transitions.
 */
class BandAssignmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DoseGuardRepository(application)

    sealed class UiState {
        object Idle : UiState()
        object SearchingWorker : UiState()
        data class WorkerNotFound(val employeeId: String) : UiState()
        data class WorkerFound(val worker: WorkerEntity, val activeBand: BandEntity?) : UiState()
        data class ScanningBand(val worker: WorkerEntity) : UiState()
        data class BandRejected(val worker: WorkerEntity, val reason: String, val details: String) : UiState()
        data class ConfirmPending(val worker: WorkerEntity, val band: BandEntity) : UiState()
        data class Assigning(val worker: WorkerEntity, val band: BandEntity) : UiState()
        data class Assigned(val worker: WorkerEntity, val band: BandEntity, val assignment: BandAssignmentEntity) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _employeeInput = MutableStateFlow("")
    val employeeInput: StateFlow<String> = _employeeInput.asStateFlow()

    fun onEmployeeInputChanged(input: String) {
        _employeeInput.value = input
    }

    /** Search worker by employee ID or worker ID. */
    fun searchWorker(query: String? = null) {
        val empId = (query ?: _employeeInput.value).trim()
        if (empId.isBlank()) return

        _uiState.value = UiState.SearchingWorker

        viewModelScope.launch {
            try {
                // Search by Employee ID first, then Worker ID fallback
                val worker = repo.getWorkerByEmployeeId(empId) ?: repo.getWorkerById(empId)
                if (worker != null) {
                    val activeBand = repo.getActiveBandForWorker(worker.workerId)
                    _uiState.value = UiState.WorkerFound(worker, activeBand)
                } else {
                    _uiState.value = UiState.WorkerNotFound(empId)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to query worker database.")
            }
        }
    }

    fun proceedToScanBand() {
        val current = _uiState.value
        if (current is UiState.WorkerFound) {
            _uiState.value = UiState.ScanningBand(current.worker)
        }
    }

    /**
     * Decode and validate scanned or manually entered band QR payload.
     * Decodes payloads like "DG:BAND:BAND-001285", "https://doseguard.app/band/WB-1001", or bare "BAND-001285".
     */
    fun onBandDecoded(rawQrValue: String) {
        val current = _uiState.value
        val worker = when (current) {
            is UiState.ScanningBand -> current.worker
            is UiState.BandRejected -> current.worker
            is UiState.WorkerFound -> current.worker
            else -> return
        }

        val bandId = parseBandId(rawQrValue)
        if (bandId.isBlank()) {
            _uiState.value = UiState.BandRejected(worker, "INVALID_QR", "Invalid or unreadable QR format.")
            return
        }

        viewModelScope.launch {
            try {
                val validation = repo.validateBandForAssignment(bandId, worker.workerId)
                when (validation) {
                    is DoseGuardRepository.BandValidationResult.Valid -> {
                        _uiState.value = UiState.ConfirmPending(worker, validation.band)
                    }
                    is DoseGuardRepository.BandValidationResult.Invalid -> {
                        _uiState.value = UiState.BandRejected(worker, validation.reason, validation.message)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.BandRejected(worker, "ERROR", e.message ?: "Validation failed.")
            }
        }
    }

    /** Executes the atomic assignment transaction. */
    fun confirmAssignment() {
        val current = _uiState.value
        if (current is UiState.ConfirmPending) {
            val worker = current.worker
            val band = current.band
            _uiState.value = UiState.Assigning(worker, band)

            viewModelScope.launch {
                try {
                    val assignment = repo.assignBandToWorkerTransaction(worker.workerId, band.bandId)
                    _uiState.value = UiState.Assigned(worker, band, assignment)
                } catch (e: Exception) {
                    _uiState.value = UiState.Error(e.message ?: "Failed to persist band assignment.")
                }
            }
        }
    }

    fun retryScanBand() {
        val current = _uiState.value
        if (current is UiState.BandRejected) {
            _uiState.value = UiState.ScanningBand(current.worker)
        } else if (current is UiState.ConfirmPending) {
            _uiState.value = UiState.ScanningBand(current.worker)
        }
    }

    fun selectDifferentWorker() {
        _uiState.value = UiState.Idle
        _employeeInput.value = ""
    }

    fun resetAll() {
        _uiState.value = UiState.Idle
        _employeeInput.value = ""
    }

    private fun parseBandId(raw: String): String {
        return when {
            raw.contains("DG:BAND:") -> raw.substringAfter("DG:BAND:").trim()
            raw.contains("/band/")   -> raw.substringAfterLast("/band/").trim()
            else                     -> raw.trim()
        }
    }
}
