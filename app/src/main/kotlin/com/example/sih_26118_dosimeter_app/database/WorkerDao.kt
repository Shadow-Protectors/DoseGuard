package com.example.sih_26118_dosimeter_app.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity)

    @Query("SELECT * FROM workers ORDER BY registeredAt DESC")
    fun getAllWorkersLiveData(): LiveData<List<WorkerEntity>>

    @Query("SELECT * FROM workers ORDER BY registeredAt DESC")
    suspend fun getAllWorkers(): List<WorkerEntity>

    @Query("SELECT * FROM workers WHERE workerId = :workerId LIMIT 1")
    suspend fun getWorkerById(workerId: String): WorkerEntity?

    @Query("SELECT * FROM workers ORDER BY registeredAt DESC LIMIT 1")
    suspend fun getLatestWorker(): WorkerEntity?

    @Query("SELECT * FROM workers ORDER BY registeredAt DESC LIMIT 1")
    fun getLatestWorkerLiveData(): LiveData<WorkerEntity?>

    @Query("SELECT * FROM workers WHERE syncStatus = 'PENDING'")
    suspend fun getPendingWorkers(): List<WorkerEntity>

    @Query("UPDATE workers SET syncStatus = :status WHERE workerId = :workerId")
    suspend fun updateSyncStatus(workerId: String, status: String)
}
