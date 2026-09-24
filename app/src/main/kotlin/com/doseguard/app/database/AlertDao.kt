package com.doseguard.app.database

import androidx.room.*
import com.doseguard.app.model.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertEntity)

    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActiveAlertsFlow(): Flow<List<AlertEntity>>

    @Query("SELECT COUNT(*) FROM alerts WHERE status = 'ACTIVE'")
    fun getActiveCountFlow(): Flow<Int>

    @Query("UPDATE alerts SET status = :status WHERE alertId = :alertId")
    suspend fun updateStatus(alertId: String, status: String)

    @Query("UPDATE alerts SET status = 'ACKNOWLEDGED' WHERE workerId = :workerId AND status = 'ACTIVE'")
    suspend fun acknowledgeForWorker(workerId: String)
}
