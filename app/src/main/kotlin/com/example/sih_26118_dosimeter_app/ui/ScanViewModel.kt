package com.example.sih_26118_dosimeter_app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sih_26118_dosimeter_app.cv.KineticDoseSolver
import com.example.sih_26118_dosimeter_app.database.ShiftLogEntity
import com.example.sih_26118_dosimeter_app.repository.DoseGuardRepository
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DoseGuardRepository(application)

    private val _currentDeltaE = MutableLiveData<Double>(8.50)
    val currentDeltaE: LiveData<Double> = _currentDeltaE

    private val _doseResult = MutableLiveData<KineticDoseSolver.DoseResult>()
    val doseResult: LiveData<KineticDoseSolver.DoseResult> = _doseResult

    private val _saveStatus = MutableLiveData<SaveState>(SaveState.Idle)
    val saveStatus: LiveData<SaveState> = _saveStatus

    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        data class Success(val isOnlineSynced: Boolean, val logId: String) : SaveState()
        data class Error(val message: String) : SaveState()
    }

    init {
        analyzeReading(8.50, 8.0)
    }

    fun analyzeReading(deltaE: Double, shiftHours: Double = 8.0) {
        _currentDeltaE.value = deltaE
        val result = KineticDoseSolver.calculateDose(deltaE = deltaE, shiftHours = shiftHours)
        _doseResult.value = result
    }

    fun saveCurrentScan(
        workerId: String,
        workerName: String,
        department: String,
        bandId: String,
        durationHours: Double = 8.0
    ) {
        val result = _doseResult.value ?: return
        val deltaE = _currentDeltaE.value ?: 8.50

        _saveStatus.value = SaveState.Saving

        viewModelScope.launch {
            try {
                val now = Date()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                val logId = "LOG-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
                val rawHashInput = "$logId-$workerId-$bandId-${now.time}-$deltaE"
                val imageHash = computeSha256(rawHashInput)

                val entity = ShiftLogEntity(
                    logId = logId,
                    workerId = workerId,
                    workerName = workerName,
                    department = department,
                    bandId = bandId,
                    shiftDate = dateFormat.format(now),
                    scanTime = timeFormat.format(now),
                    durationHours = durationHours,
                    expiryStatus = "VALID",
                    rawDeltaE = deltaE,
                    estimatedDosePpmHr = result.dosePpmHr,
                    uncertaintyPpmHr = result.uncertaintyPpmHr,
                    twa8hrPpm = result.twa8hrPpm,
                    riskLevel = result.riskLevel,
                    actionRequired = result.actionRequired,
                    imageHash = imageHash,
                    syncStatus = "PENDING"
                )

                val onlineSynced = repository.saveScan(entity)
                _saveStatus.value = SaveState.Success(onlineSynced, logId)
            } catch (e: Exception) {
                _saveStatus.value = SaveState.Error(e.message ?: "Failed to save scan")
            }
        }
    }

    fun resetSaveState() {
        _saveStatus.value = SaveState.Idle
    }

    private fun computeSha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
