package com.example.sih_26118_dosimeter_app.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ShiftLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ShiftLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<ShiftLogEntity>)

    @Query("SELECT * FROM shift_logs ORDER BY shiftDate DESC, scanTime DESC")
    fun getAllLogsLiveData(): LiveData<List<ShiftLogEntity>>

    @Query("SELECT * FROM shift_logs ORDER BY shiftDate DESC, scanTime DESC")
    suspend fun getAllLogs(): List<ShiftLogEntity>

    @Query("SELECT * FROM shift_logs WHERE riskLevel = :risk ORDER BY shiftDate DESC, scanTime DESC")
    fun getLogsByRiskLiveData(risk: String): LiveData<List<ShiftLogEntity>>

    @Query("SELECT * FROM shift_logs WHERE syncStatus = 'PENDING'")
    suspend fun getPendingLogs(): List<ShiftLogEntity>

    @Query("UPDATE shift_logs SET syncStatus = :status WHERE logId = :logId")
    suspend fun updateSyncStatus(logId: String, status: String)

    @Query("SELECT SUM(estimatedDosePpmHr) FROM shift_logs")
    suspend fun getTotalDose(): Double?

    @Query("SELECT MAX(twa8hrPpm) FROM shift_logs")
    suspend fun getMaxTwa(): Double?

    @Query("SELECT COUNT(*) FROM shift_logs")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM shift_logs WHERE riskLevel = :risk")
    suspend fun getCountByRisk(risk: String): Int

    @Query("SELECT * FROM shift_logs ORDER BY shiftDate DESC, scanTime DESC LIMIT 1")
    fun getLatestLogLiveData(): LiveData<ShiftLogEntity?>

    @Query("SELECT * FROM shift_logs ORDER BY shiftDate DESC, scanTime DESC LIMIT 1")
    suspend fun getLatestLog(): ShiftLogEntity?
}
