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

    @Query("SELECT * FROM workers WHERE UPPER(REPLACE(employeeId, '-', '')) = UPPER(REPLACE(:employeeId, '-', '')) LIMIT 1")
    suspend fun getByEmployeeId(employeeId: String): WorkerEntity?

    /**
     * Industry-database style lookup: matches either the internal workerId or the
     * company employee number, tolerant of case and dash formatting
     * (e.g. "emp02345", "EMP-02345" and "EMP02345" all match).
     */
    @Query("""
        SELECT * FROM workers
        WHERE UPPER(REPLACE(workerId, '-', ''))   = UPPER(REPLACE(:identifier, '-', ''))
           OR UPPER(REPLACE(employeeId, '-', '')) = UPPER(REPLACE(:identifier, '-', ''))
        LIMIT 1
    """)
    suspend fun findByIdentifier(identifier: String): WorkerEntity?

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
