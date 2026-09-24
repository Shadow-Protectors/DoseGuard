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
 * QrScanViewModel — handles band lookup and statutory validity gating.
 *
 * Prevents expired or saturated bands from reaching the camera shutter.
 */
class QrScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DoseGuardRepository(application)

    sealed class ScanState {
        object Idle : ScanState()
        object Processing : ScanState()
        /** Valid band linked to an active worker. */
        data class BandAssigned(val band: BandEntity, val worker: WorkerEntity) : ScanState()
        /** New or unassigned band — needs worker registration or assignment. */
        data class BandUnassigned(val band: BandEntity) : ScanState()
        /** Brand new band never registered before. */
        data class NewBand(val bandId: String, val qrData: String) : ScanState()
        /** Band is expired, saturated, or replaced — blocked from camera shutter. */
        data class BandInvalid(val bandId: String, val reason: String, val workerId: String) : ScanState()
        data class Error(val message: String) : ScanState()
    }

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun onQrScanned(rawQrValue: String) {
        if (_state.value is ScanState.Processing) return // debounce
        _state.value = ScanState.Processing

        viewModelScope.launch {
            try {
                val bandId = parseBandId(rawQrValue)
                val band = repo.getBandById(bandId)

                if (band == null) {
                    // First time this band is seen — register it in local DB
                    repo.registerNewBand(bandId, rawQrValue)
                    _state.value = ScanState.NewBand(bandId, rawQrValue)
                } else if (band.workerId.isBlank()) {
                    _state.value = ScanState.BandUnassigned(band)
                } else {
                    // Evaluate validity gate (check expiration & saturation)
                    val validity = repo.evaluateBandValidity(band)
                    when (validity) {
                        is DoseGuardRepository.BandValidity.Invalid -> {
                            _state.value = ScanState.BandInvalid(
                                bandId = band.bandId,
                                reason = validity.reason,
                                workerId = band.workerId
                            )
                        }
                        is DoseGuardRepository.BandValidity.Valid -> {
                            val worker = repo.getWorkerById(band.workerId)
                            if (worker != null) {
                                _state.value = ScanState.BandAssigned(band, worker)
                            } else {
                                _state.value = ScanState.BandUnassigned(band)
                            }
                        }
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

    private fun parseBandId(raw: String): String {
        return when {
            raw.contains("DG:BAND:") -> raw.substringAfter("DG:BAND:").trim()
            raw.contains("/band/")   -> raw.substringAfterLast("/band/").trim()
            else                     -> raw.trim()
        }
    }
}
