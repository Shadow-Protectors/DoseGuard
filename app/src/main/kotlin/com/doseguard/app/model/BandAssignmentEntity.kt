package com.doseguard.app.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * BandAssignmentEntity — records historical and active assignments of bands to workers.
 * Ensures a full audit trail of who wore which band during what shift.
 */
@Entity(
    tableName = "band_assignments",
    indices = [
        Index(value = ["workerId"]),
        Index(value = ["bandId"])
    ]
)
data class BandAssignmentEntity(
    @PrimaryKey val assignmentId: String,       // UUID
    val workerId: String,                       // Foreign Key to WorkerEntity.workerId
    val bandId: String,                         // Foreign Key to BandEntity.bandId
    val assignedAt: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE",              // "ACTIVE" | "RELEASED"
    val releasedAt: Long? = null
)
