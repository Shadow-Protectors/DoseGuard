package com.doseguard.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.repository.DoseGuardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * QrScanViewModel — handles band lookup after a QR scan.
 *
 * Flow:
 *   Idle → Scanning → BandFound(worker linked) / BandUnassigned / Error
 */
class QrScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DoseGuardRepository(application)

    sealed class ScanState {
        object Idle : ScanState()
        object Processing : ScanState()
        /** Band exists in DB and is linked to a worker. */
        data class BandAssigned(val band: BandEntity, val worker: WorkerEntity) : ScanState()
        /** Band exists but no worker linked yet. */
        data class BandUnassigned(val band: BandEntity) : ScanState()
        /** Brand new band — never seen before. */
        data class NewBand(val bandId: String, val qrData: String) : ScanState()
        data class Error(val message: String) : ScanState()
    }

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun onQrScanned(rawQrValue: String) {
        if (_state.value is ScanState.Processing) return // debounce
        _state.value = ScanState.Processing

        viewModelScope.launch {
            try {
                // Extract band ID — QR payload format: "DG:BAND-ID:BAND-0042" or plain "BAND-0042"
                val bandId = parseBandId(rawQrValue)

                val band = repo.getBandById(bandId)
                if (band == null) {
                    // First time this band is seen — register it and ask for worker details
                    repo.registerNewBand(bandId, rawQrValue)
                    _state.value = ScanState.NewBand(bandId, rawQrValue)
                } else if (band.workerId.isBlank()) {
                    _state.value = ScanState.BandUnassigned(band)
                } else {
                    val worker = repo.getWorkerById(band.workerId)
                    if (worker != null) {
                        _state.value = ScanState.BandAssigned(band, worker)
                    } else {
                        // Band has a workerId but the worker record was deleted — treat as unassigned
                        _state.value = ScanState.BandUnassigned(band)
                    }
                }
            } catch (e: Exception) {
                _state.value = ScanState.Error(e.message ?: "QR lookup failed")
            }
        }
    }

    fun reset() {
        _state.value = ScanState.Idle
    }

    /** Parse band ID from various QR formats:
     *  "DG:BAND:BAND-0042", "BAND-0042", "https://doseguard.app/band/BAND-0042" */
    private fun parseBandId(raw: String): String {
        return when {
            raw.contains("DG:BAND:") -> raw.substringAfter("DG:BAND:").trim()
            raw.contains("/band/")   -> raw.substringAfterLast("/band/").trim()
            else                     -> raw.trim()
        }
    }
}
