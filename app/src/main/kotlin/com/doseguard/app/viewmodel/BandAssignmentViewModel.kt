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
 * BandAssignmentViewModel — drives the worker-first band assignment flow.
 *
 *   1. Identify worker (typed Employee ID or scanned employee card)
 *   2. Display worker details from the industry database
 *   3. Scan the dosimeter band QR code
 *   4. Validate the band (exists / unassigned / in-date / not saturated)
 *   5. Confirm and persist the Worker ↔ Band mapping
 */
class BandAssignmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DoseGuardRepository(application)

    // ── Worker identification state ──────────────────────────────────────────
    sealed class WorkerLookup {
        object Idle : WorkerLookup()
        object Searching : WorkerLookup()
        data class NotFound(val query: String) : WorkerLookup()
        data class Found(val worker: WorkerEntity) : WorkerLookup()
        data class Error(val message: String) : WorkerLookup()
    }

    // ── Band scan / validation state ─────────────────────────────────────────
    sealed class BandScan {
        object Scanning : BandScan()
        object Validating : BandScan()
        /** Band passed all checks — awaiting operator confirmation. */
        data class Confirm(val band: BandEntity) : BandScan()
        data class Rejected(val title: String, val message: String, val bandId: String) : BandScan()
        object Saving : BandScan()
        data class Success(val assignment: BandAssignmentEntity, val band: BandEntity) : BandScan()
    }

    private val _workerLookup = MutableStateFlow<WorkerLookup>(WorkerLookup.Idle)
    val workerLookup: StateFlow<WorkerLookup> = _workerLookup.asStateFlow()

    private val _bandScan = MutableStateFlow<BandScan>(BandScan.Scanning)
    val bandScan: StateFlow<BandScan> = _bandScan.asStateFlow()

    private val _workerIdInput = MutableStateFlow("")
    val workerIdInput: StateFlow<String> = _workerIdInput.asStateFlow()

    /** The worker currently selected for assignment (set once lookup succeeds). */
    val selectedWorker: WorkerEntity?
        get() = (_workerLookup.value as? WorkerLookup.Found)?.worker

    // ── Step 1 & 2: worker identification ────────────────────────────────────

    fun onWorkerIdInputChange(value: String) {
        _workerIdInput.value = value
        if (_workerLookup.value is WorkerLookup.NotFound) _workerLookup.value = WorkerLookup.Idle
    }

    /** Search by typed Employee ID / Worker ID. */
    fun searchWorker(query: String = _workerIdInput.value) {
        val q = query.trim()
        if (q.isBlank() || _workerLookup.value is WorkerLookup.Searching) return

        _workerLookup.value = WorkerLookup.Searching
        viewModelScope.launch {
            try {
                val worker = repo.findWorker(q)
                _workerLookup.value =
                    if (worker == null) WorkerLookup.NotFound(q)
                    else WorkerLookup.Found(worker)
            } catch (e: Exception) {
                _workerLookup.value = WorkerLookup.Error(e.message ?: "Worker lookup failed")
            }
        }
    }

    /** Employee ID card QR / barcode was decoded. */
    fun onWorkerCardScanned(rawValue: String) {
        if (_workerLookup.value is WorkerLookup.Searching) return   // debounce
        val identifier = repo.parseWorkerIdentifier(rawValue)
        _workerIdInput.value = identifier
        searchWorker(identifier)
    }

    /**
     * Re-hydrate the selected worker from the database when the flow is resumed
     * on a route that only carries the workerId (e.g. after process death).
     */
    fun ensureWorker(workerId: String) {
        if (workerId.isBlank()) return
        if (selectedWorker?.workerId == workerId) return
        if (_workerLookup.value is WorkerLookup.Searching) return
        viewModelScope.launch {
            repo.getWorkerById(workerId)?.let { _workerLookup.value = WorkerLookup.Found(it) }
        }
    }

    fun clearWorker() {
        _workerLookup.value = WorkerLookup.Idle
        _workerIdInput.value = ""
        _bandScan.value = BandScan.Scanning
    }

    // ── Step 3 & 4: band scan + validation ───────────────────────────────────

    fun resetBandScan() {
        _bandScan.value = BandScan.Scanning
    }

    /** Band QR decoded (or manually entered) — run the validation gate. */
    fun onBandScanned(rawValue: String) {
        if (_bandScan.value !is BandScan.Scanning) return           // debounce
        _bandScan.value = BandScan.Validating

        viewModelScope.launch {
            try {
                when (val check = repo.checkBandForAssignment(rawValue)) {
                    is DoseGuardRepository.AssignmentCheck.Available ->
                        _bandScan.value = BandScan.Confirm(check.band)

                    is DoseGuardRepository.AssignmentCheck.AlreadyAssigned -> {
                        val holder = check.holder
                        val who =
                            if (holder != null) "${holder.name} (${holder.employeeId})"
                            else "worker ${check.band.workerId}"
                        _bandScan.value = BandScan.Rejected(
                            title = "Band Already Assigned",
                            message = "${check.band.bandId} is currently issued to $who. " +
                                "Release it before reassigning.",
                            bandId = check.band.bandId
                        )
                    }

                    is DoseGuardRepository.AssignmentCheck.Expired ->
                        _bandScan.value = BandScan.Rejected(
                            title = "Band Expired",
                            message = "${check.band.bandId} has passed its chemical shelf life. " +
                                "The sensor strip is no longer calibrated and cannot be issued.",
                            bandId = check.band.bandId
                        )

                    is DoseGuardRepository.AssignmentCheck.Saturated ->
                        _bandScan.value = BandScan.Rejected(
                            title = "Band Saturated",
                            message = "${check.band.bandId} has reached its maximum dose capacity " +
                                "(${check.band.maximumDose} ppm·hr). Discard and use a fresh band.",
                            bandId = check.band.bandId
                        )

                    is DoseGuardRepository.AssignmentCheck.Retired ->
                        _bandScan.value = BandScan.Rejected(
                            title = "Band Retired",
                            message = "${check.band.bandId} was replaced and withdrawn from service.",
                            bandId = check.band.bandId
                        )

                    is DoseGuardRepository.AssignmentCheck.NotFound ->
                        _bandScan.value = BandScan.Rejected(
                            title = "Band Not Found",
                            message = "${check.bandId} is not registered in the dosimeter inventory. " +
                                "Check the QR code or register the band first.",
                            bandId = check.bandId
                        )
                }
            } catch (e: Exception) {
                _bandScan.value = BandScan.Rejected(
                    title = "Validation Failed",
                    message = e.message ?: "Could not validate this band.",
                    bandId = ""
                )
            }
        }
    }

    // ── Step 5: commit the mapping ───────────────────────────────────────────

    /** Confirm dialog accepted — write the band row and the assignment record. */
    fun confirmAssignment() {
        val band = (_bandScan.value as? BandScan.Confirm)?.band ?: return
        val worker = selectedWorker ?: return

        _bandScan.value = BandScan.Saving
        viewModelScope.launch {
            try {
                val record = repo.assignBand(worker.workerId, band.bandId)
                _bandScan.value = BandScan.Success(record, band)
            } catch (e: Exception) {
                _bandScan.value = BandScan.Rejected(
                    title = "Assignment Failed",
                    message = e.message ?: "Could not save the assignment.",
                    bandId = band.bandId
                )
            }
        }
    }

    /** Cancel from the confirm dialog — go back to scanning. */
    fun cancelConfirmation() {
        if (_bandScan.value is BandScan.Confirm) _bandScan.value = BandScan.Scanning
    }

    /** Full reset after "Done" on the success screen. */
    fun reset() {
        _workerLookup.value = WorkerLookup.Idle
        _workerIdInput.value = ""
        _bandScan.value = BandScan.Scanning
    }
}
