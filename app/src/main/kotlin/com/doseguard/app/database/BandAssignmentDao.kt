package com.doseguard.app.database

import androidx.room.*
import com.doseguard.app.model.BandAssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BandAssignmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assignment: BandAssignmentEntity)

    @Query("SELECT * FROM band_assignments WHERE assignmentId = :assignmentId")
    suspend fun getById(assignmentId: String): BandAssignmentEntity?

    /** Currently active assignment for a band (if any). */
    @Query("SELECT * FROM band_assignments WHERE bandId = :bandId AND status = 'ACTIVE' ORDER BY assignedTime DESC LIMIT 1")
    suspend fun getActiveForBand(bandId: String): BandAssignmentEntity?

    /** Currently active assignment for a worker (if any). */
    @Query("SELECT * FROM band_assignments WHERE workerId = :workerId AND status = 'ACTIVE' ORDER BY assignedTime DESC LIMIT 1")
    suspend fun getActiveForWorker(workerId: String): BandAssignmentEntity?

    @Query("SELECT * FROM band_assignments WHERE workerId = :workerId ORDER BY assignedTime DESC")
    fun getByWorkerFlow(workerId: String): Flow<List<BandAssignmentEntity>>

    @Query("SELECT * FROM band_assignments ORDER BY assignedTime DESC")
    fun getAllFlow(): Flow<List<BandAssignmentEntity>>

    /** Close out any open assignment rows for a band (used when a band is re-issued). */
    @Query("UPDATE band_assignments SET status = 'RELEASED', releasedTime = :time WHERE bandId = :bandId AND status = 'ACTIVE'")
    suspend fun releaseForBand(bandId: String, time: Long)

    /** Close out any open assignment rows for a worker (a worker wears one band at a time). */
    @Query("UPDATE band_assignments SET status = 'RELEASED', releasedTime = :time WHERE workerId = :workerId AND status = 'ACTIVE'")
    suspend fun releaseForWorker(workerId: String, time: Long)
}
