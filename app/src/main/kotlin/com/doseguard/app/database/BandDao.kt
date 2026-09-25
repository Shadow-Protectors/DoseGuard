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

    @Query("SELECT * FROM bands WHERE workerId = :workerId AND bandStatus IN ('ACTIVE', 'ASSIGNED') ORDER BY issueDate DESC LIMIT 1")
    suspend fun getActiveByWorker(workerId: String): BandEntity?

    @Query("SELECT * FROM bands ORDER BY issueDate DESC")
    fun getAllFlow(): Flow<List<BandEntity>>

    @Query("SELECT * FROM bands WHERE bandStatus = 'AVAILABLE' OR workerId = '' ORDER BY bandId ASC")
    fun getAvailableBandsFlow(): Flow<List<BandEntity>>

    @Query("SELECT * FROM bands WHERE bandStatus = 'AVAILABLE' OR workerId = '' ORDER BY bandId ASC")
    suspend fun getAvailableBands(): List<BandEntity>

    /**
     * Update cumulative dose directly from latest optical reading.
     * Prevents double-counting since strip color measurement is intrinsically cumulative.
     * Automatically transitions status to 'SATURATED' if limit reached.
     */
    @Query("""
        UPDATE bands
        SET currentEstimatedDose = :latestCumulativeDose,
            bandStatus = CASE 
                WHEN :latestCumulativeDose >= maximumDose THEN 'SATURATED' 
                ELSE bandStatus 
            END,
            lastScanTime = :scanTime
        WHERE bandId = :bandId
    """)
    suspend fun updateDose(bandId: String, latestCumulativeDose: Double, scanTime: Long)

    @Query("""
        UPDATE bands 
        SET workerId = :workerId, 
            bandStatus = 'ACTIVE', 
            issueDate = :issueTime 
        WHERE bandId = :bandId
    """)
    suspend fun assignWorker(bandId: String, workerId: String, issueTime: Long = System.currentTimeMillis())

    @Query("UPDATE bands SET workerId = '', bandStatus = 'AVAILABLE' WHERE workerId = :workerId AND bandStatus IN ('ACTIVE', 'ASSIGNED')")
    suspend fun unassignPreviousBandsForWorker(workerId: String)

    @Query("UPDATE bands SET bandStatus = :status WHERE bandId = :bandId")
    suspend fun updateStatus(bandId: String, status: String)

    @Query("UPDATE bands SET bandStatus = 'RELEASED' WHERE bandId = :oldBandId")
    suspend fun markBandReplaced(oldBandId: String)
}
