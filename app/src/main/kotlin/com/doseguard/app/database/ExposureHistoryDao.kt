package com.doseguard.app.database

import androidx.room.*
import com.doseguard.app.model.ExposureHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExposureHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ExposureHistoryEntity)

    @Query("SELECT * FROM exposure_history ORDER BY scanTime DESC")
    fun getAllFlow(): Flow<List<ExposureHistoryEntity>>

    @Query("SELECT * FROM exposure_history WHERE workerId = :workerId ORDER BY scanTime DESC")
    fun getByWorkerFlow(workerId: String): Flow<List<ExposureHistoryEntity>>

    @Query("SELECT * FROM exposure_history WHERE bandId = :bandId ORDER BY scanTime DESC")
    fun getByBandFlow(bandId: String): Flow<List<ExposureHistoryEntity>>

    /** Last 7 days of scans for a worker — used on the history trend chart. */
    @Query("""
        SELECT * FROM exposure_history
        WHERE workerId = :workerId
          AND scanTime >= :since
        ORDER BY scanTime ASC
    """)
    fun getRecentByWorkerFlow(workerId: String, since: Long): Flow<List<ExposureHistoryEntity>>

    @Query("SELECT SUM(estimatedDose) FROM exposure_history WHERE workerId = :workerId")
    suspend fun getTotalDoseForWorker(workerId: String): Double?

    @Query("SELECT COUNT(*) FROM exposure_history")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM exposure_history WHERE riskLevel IN ('HIGH', 'CRITICAL')")
    fun getHighRiskCountFlow(): Flow<Int>

    @Delete
    suspend fun delete(history: ExposureHistoryEntity)
}
