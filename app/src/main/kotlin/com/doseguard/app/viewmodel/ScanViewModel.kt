package com.doseguard.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doseguard.app.imageprocessing.ExposureEstimator
import com.doseguard.app.imageprocessing.KineticColorEstimator
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.repository.DoseGuardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ScanViewModel — manages the camera capture → exposure analysis → save pipeline.
 *
 * The ViewModel holds the last captured image bytes so the Result screen
 * can display result details without re-processing.
 */
class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DoseGuardRepository(application)
    private val estimator = KineticColorEstimator()

    sealed class ScanUiState {
        object Idle : ScanUiState()
        object Analyzing : ScanUiState()
        object Saving : ScanUiState()
        data class Result(val result: ExposureEstimator.EstimationResult) : ScanUiState()
        data class Error(val message: String) : ScanUiState()
    }

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    // Held so ResultScreen can read without re-fetching from DB
    private val _lastResult = MutableStateFlow<ExposureEstimator.EstimationResult?>(null)
    val lastResult: StateFlow<ExposureEstimator.EstimationResult?> = _lastResult.asStateFlow()

    private val _worker = MutableStateFlow<WorkerEntity?>(null)
    val worker: StateFlow<WorkerEntity?> = _worker.asStateFlow()

    private val _band = MutableStateFlow<BandEntity?>(null)
    val band: StateFlow<BandEntity?> = _band.asStateFlow()

    fun loadContext(workerId: String, bandId: String) {
        viewModelScope.launch {
            _worker.value = repo.getWorkerById(workerId)
            _band.value   = repo.getBandById(bandId)
        }
    }

    /** Called when CameraX captures an image and returns JPEG bytes + local file path. */
    fun onImageCaptured(
        imageBytes: ByteArray,
        imagePath: String,
        bandId: String,
        workerId: String,
        shiftHours: Double = 8.0
    ) {
        _uiState.value = ScanUiState.Analyzing
        viewModelScope.launch {
            try {
                val result = repo.processScanAndSave(
                    bandId      = bandId,
                    workerId    = workerId,
                    imageBytes  = imageBytes,
                    imagePath   = imagePath,
                    shiftHours  = shiftHours
                )
                _lastResult.value = result
                _uiState.value = ScanUiState.Result(result)
            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error(e.message ?: "Analysis failed")
            }
        }
    }

    /**
     * Demo-mode scan: uses a synthetic ΔE instead of a real image.
     * Triggered by preset chips (Safe / Caution / Critical).
     */
    fun onSimulatedScan(
        deltaE: Double,
        bandId: String,
        workerId: String,
        shiftHours: Double = 8.0
    ) {
        _uiState.value = ScanUiState.Analyzing
        viewModelScope.launch {
            try {
                val result = repo.processSimulatedScan(bandId, workerId, deltaE, shiftHours)
                _lastResult.value = result
                _uiState.value = ScanUiState.Result(result)
            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error(e.message ?: "Simulated scan failed")
            }
        }
    }

    fun reset() {
        _uiState.value = ScanUiState.Idle
        _lastResult.value = null
    }
}
