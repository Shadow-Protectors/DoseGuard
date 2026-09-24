package com.doseguard.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.repository.DoseGuardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class WorkerRegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DoseGuardRepository(application)

    sealed class RegistrationState {
        object Idle : RegistrationState()
        object Saving : RegistrationState()
        data class Success(val workerId: String) : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }

    private val _state = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val state: StateFlow<RegistrationState> = _state.asStateFlow()

    // Form field states — bound directly to TextField values in the Compose UI
    val name          = MutableStateFlow("")
    val employeeId    = MutableStateFlow("")
    val department    = MutableStateFlow("")
    val designation   = MutableStateFlow("")
    val shift         = MutableStateFlow("Morning")

    // Validation errors
    val nameError        = MutableStateFlow<String?>(null)
    val employeeIdError  = MutableStateFlow<String?>(null)
    val departmentError  = MutableStateFlow<String?>(null)
    val designationError = MutableStateFlow<String?>(null)

    fun register(bandId: String) {
        if (!validate()) return
        _state.value = RegistrationState.Saving

        viewModelScope.launch {
            try {
                val workerId = "W-${UUID.randomUUID().toString().takeLast(8).uppercase()}"
                val worker = WorkerEntity(
                    workerId    = workerId,
                    employeeId  = employeeId.value.trim(),
                    name        = name.value.trim(),
                    department  = department.value.trim(),
                    designation = designation.value.trim(),
                    shift       = shift.value
                )
                repo.registerWorker(worker)
                repo.assignBandToWorker(bandId, workerId)
                _state.value = RegistrationState.Success(workerId)
            } catch (e: Exception) {
                _state.value = RegistrationState.Error(e.message ?: "Registration failed")
            }
        }
    }

    private fun validate(): Boolean {
        var valid = true
        nameError.value        = if (name.value.isBlank())        "Name is required"        else null
        employeeIdError.value  = if (employeeId.value.isBlank())  "Employee ID is required"  else null
        departmentError.value  = if (department.value.isBlank())  "Department is required"  else null
        designationError.value = if (designation.value.isBlank()) "Designation is required" else null
        if (name.value.isBlank() || employeeId.value.isBlank() ||
            department.value.isBlank() || designation.value.isBlank()) valid = false
        return valid
    }

    fun resetState() { _state.value = RegistrationState.Idle }
}
