package com.doseguard.app.database

import androidx.room.*
import com.doseguard.app.model.BandAssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BandAssignmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assignment: BandAssignmentEntity)

    @Query("SELECT * FROM band_assignments WHERE workerId = :workerId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveForWorker(workerId: String): BandAssignmentEntity?

    @Query("SELECT * FROM band_assignments WHERE bandId = :bandId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveForBand(bandId: String): BandAssignmentEntity?

    @Query("UPDATE band_assignments SET status = 'RELEASED', releasedAt = :releaseTime WHERE workerId = :workerId AND status = 'ACTIVE'")
    suspend fun releaseActiveForWorker(workerId: String, releaseTime: Long = System.currentTimeMillis())

    @Query("UPDATE band_assignments SET status = 'RELEASED', releasedAt = :releaseTime WHERE bandId = :bandId AND status = 'ACTIVE'")
    suspend fun releaseActiveForBand(bandId: String, releaseTime: Long = System.currentTimeMillis())

    @Query("SELECT * FROM band_assignments ORDER BY assignedAt DESC")
    fun getAllFlow(): Flow<List<BandAssignmentEntity>>

    @Query("SELECT * FROM band_assignments WHERE workerId = :workerId ORDER BY assignedAt DESC")
    fun getByWorkerFlow(workerId: String): Flow<List<BandAssignmentEntity>>
}
