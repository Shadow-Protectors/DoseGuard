package com.doseguard.app.database

import androidx.room.*
import com.doseguard.app.model.BandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BandDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(band: BandEntity)

    @Update
    suspend fun update(band: BandEntity)

    @Query("SELECT * FROM bands WHERE bandId = :bandId")
    suspend fun getById(bandId: String): BandEntity?

    @Query("SELECT * FROM bands WHERE workerId = :workerId ORDER BY issueDate DESC LIMIT 1")
    suspend fun getActiveByWorker(workerId: String): BandEntity?

    @Query("SELECT * FROM bands ORDER BY issueDate DESC")
    fun getAllFlow(): Flow<List<BandEntity>>

    /**
     * Update cumulative dose and last scan timestamp.
     * Called after every successful exposure scan.
     */
    @Query("""
        UPDATE bands
        SET currentEstimatedDose = currentEstimatedDose + :additionalDose,
            lastScanTime = :scanTime
        WHERE bandId = :bandId
    """)
    suspend fun addDose(bandId: String, additionalDose: Double, scanTime: Long)

    @Query("UPDATE bands SET workerId = :workerId, bandStatus = 'ACTIVE' WHERE bandId = :bandId")
    suspend fun assignWorker(bandId: String, workerId: String)

    @Query("UPDATE bands SET bandStatus = :status WHERE bandId = :bandId")
    suspend fun updateStatus(bandId: String, status: String)
}
