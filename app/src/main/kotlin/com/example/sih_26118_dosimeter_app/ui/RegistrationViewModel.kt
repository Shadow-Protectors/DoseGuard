package com.example.sih_26118_dosimeter_app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sih_26118_dosimeter_app.database.WorkerEntity
import com.example.sih_26118_dosimeter_app.repository.DoseGuardRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DoseGuardRepository(application)

    val registeredWorkers: LiveData<List<WorkerEntity>> = repository.allWorkersLiveData

    private val _registrationStatus = MutableLiveData<RegistrationState>(RegistrationState.Idle)
    val registrationStatus: LiveData<RegistrationState> = _registrationStatus

    sealed class RegistrationState {
        object Idle : RegistrationState()
        object Loading : RegistrationState()
        data class Success(val worker: WorkerEntity, val isCloudSynced: Boolean) : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }

    fun registerWorkerAndBadge(
        workerId: String,
        name: String,
        department: String,
        role: String,
        email: String,
        assignedBandId: String
    ) {
        if (workerId.isBlank() || name.isBlank() || assignedBandId.isBlank()) {
            _registrationStatus.value = RegistrationState.Error("Worker ID, Name, and Badge ID are required")
            return
        }

        _registrationStatus.value = RegistrationState.Loading

        viewModelScope.launch {
            try {
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val worker = WorkerEntity(
                    workerId = workerId.trim().uppercase(),
                    name = name.trim(),
                    department = department.trim().ifEmpty { "Industrial Operations" },
                    role = role.trim().ifEmpty { "Plant Operator" },
                    email = email.trim().ifEmpty { "${workerId.lowercase()}@refinery.com" },
                    assignedBandId = assignedBandId.trim().uppercase(),
                    registeredAt = now,
                    syncStatus = "PENDING"
                )

                val synced = repository.registerWorkerAndBadge(worker)
                _registrationStatus.value = RegistrationState.Success(worker, synced)
            } catch (e: Exception) {
                _registrationStatus.value = RegistrationState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun resetStatus() {
        _registrationStatus.value = RegistrationState.Idle
    }
}
