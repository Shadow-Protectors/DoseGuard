package com.doseguard.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Worker entity — stores personnel details linked to a dosimeter band.
 * workerId is the primary key; bandId is set when the band is assigned.
 */
@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey val workerId: String,        // UUID or "W-XXXX"
    val employeeId: String,                  // Company employee number
    val name: String,
    val department: String,
    val designation: String,
    val shift: String,                       // "Morning" | "Evening" | "Night"
    val status: String = "ACTIVE",           // "ACTIVE" | "INACTIVE"
    val createdAt: Long = System.currentTimeMillis()
)
