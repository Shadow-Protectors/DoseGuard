package com.example.sih_26118_dosimeter_app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Worker and Badge Registration Entity
 * Stored locally in Room SQLite and synchronized with Cloud Database.
 */
@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey val workerId: String,
    val name: String,
    val department: String,
    val role: String,
    val email: String,
    val assignedBandId: String,
    val registeredAt: String,
    val syncStatus: String = "PENDING"
)
