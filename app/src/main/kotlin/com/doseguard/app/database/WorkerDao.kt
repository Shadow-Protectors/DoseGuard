package com.doseguard.app.database

import androidx.room.*
import com.doseguard.app.model.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(worker: WorkerEntity)

    @Query("SELECT * FROM workers WHERE workerId = :workerId")
    suspend fun getById(workerId: String): WorkerEntity?

    @Query("SELECT * FROM workers WHERE UPPER(TRIM(employeeId)) = UPPER(TRIM(:employeeId)) LIMIT 1")
    suspend fun getByEmployeeId(employeeId: String): WorkerEntity?

    @Query("SELECT * FROM workers ORDER BY createdAt DESC")
    suspend fun getAll(): List<WorkerEntity>

    @Query("SELECT * FROM workers ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers ORDER BY createdAt DESC LIMIT 1")
    fun getLatestFlow(): Flow<WorkerEntity?>

    @Query("SELECT COUNT(*) FROM workers WHERE status = 'ACTIVE'")
    fun getActiveCountFlow(): Flow<Int>

    @Query("UPDATE workers SET status = :status WHERE workerId = :workerId")
    suspend fun updateStatus(workerId: String, status: String)

    @Delete
    suspend fun delete(worker: WorkerEntity)
}
