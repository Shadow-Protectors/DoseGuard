package com.example.sih_26118_dosimeter_app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_logs")
data class ShiftLogEntity(
    @PrimaryKey val logId: String,
    val workerId: String,
    val workerName: String,
    val department: String,
    val bandId: String,
    val shiftDate: String,
    val scanTime: String,
    val durationHours: Double,
    val expiryStatus: String,
    val rawDeltaE: Double,
    val estimatedDosePpmHr: Double,
    val uncertaintyPpmHr: Double,
    val twa8hrPpm: Double,
    val riskLevel: String,
    val actionRequired: String,
    val imageHash: String,
    val syncStatus: String = "PENDING"
)
