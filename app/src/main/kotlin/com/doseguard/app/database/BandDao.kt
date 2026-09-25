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

    @Query("UPDATE bands SET workerId = :workerId, bandStatus = 'ACTIVE' WHERE bandId = :bandId")
    suspend fun assignWorker(bandId: String, workerId: String)

    /**
     * Worker ↔ Band mapping used by the Band Assignment flow.
     * Sets the owning worker, activates the band and stamps the issue date.
     */
    @Query("UPDATE bands SET workerId = :workerId, bandStatus = 'ACTIVE', issueDate = :issueDate WHERE bandId = :bandId")
    suspend fun assignWorkerWithIssueDate(bandId: String, workerId: String, issueDate: Long)

    @Query("SELECT * FROM bands WHERE workerId = '' OR bandStatus = 'UNASSIGNED' ORDER BY issueDate DESC")
    suspend fun getUnassigned(): List<BandEntity>

    @Query("UPDATE bands SET bandStatus = :status WHERE bandId = :bandId")
    suspend fun updateStatus(bandId: String, status: String)

    @Query("UPDATE bands SET bandStatus = 'REPLACED' WHERE bandId = :oldBandId")
    suspend fun markBandReplaced(oldBandId: String)
}
