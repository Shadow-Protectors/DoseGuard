package com.doseguard.app.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * BandAssignmentEntity — join table recording every Worker ↔ Band mapping event.
 *
 * Relationship:
 *   Worker (1) ──< BandAssignment (Many) >── Band (1)
 *
 * A new row is inserted each time a band is assigned to a worker. The most recent
 * row with releasedTime == null is the currently active assignment for that band.
 */
@Entity(
    tableName = "band_assignments",
    indices = [Index("workerId"), Index("bandId")]
)
data class BandAssignmentEntity(
    @PrimaryKey val assignmentId: String,        // UUID
    val workerId: String,                        // FK → workers.workerId
    val bandId: String,                          // FK → bands.bandId
    val assignedTime: Long = System.currentTimeMillis(),
    val releasedTime: Long? = null,              // null while the assignment is active
    val assignedBy: String = "OPERATOR",         // Who performed the assignment
    val status: String = "ACTIVE"                // "ACTIVE" | "RELEASED"
)
