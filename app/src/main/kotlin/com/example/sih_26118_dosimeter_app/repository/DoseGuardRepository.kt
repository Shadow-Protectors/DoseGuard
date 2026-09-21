package com.example.sih_26118_dosimeter_app.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sih_26118_dosimeter_app.database.AppDatabase
import com.example.sih_26118_dosimeter_app.database.ShiftLogEntity
import com.example.sih_26118_dosimeter_app.database.WorkerEntity
import com.example.sih_26118_dosimeter_app.network.ApiClient
import com.example.sih_26118_dosimeter_app.network.DashboardMetricsDto
import com.example.sih_26118_dosimeter_app.network.RegisterWorkerRequest
import com.example.sih_26118_dosimeter_app.network.ShiftLogDto
import com.example.sih_26118_dosimeter_app.network.SyncLogsRequest
import com.example.sih_26118_dosimeter_app.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DoseGuardRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val shiftLogDao = db.shiftLogDao()
    private val workerDao = db.workerDao()
    private val apiService = ApiClient.apiService

    // Observables for UI
    val allLogsLiveData: LiveData<List<ShiftLogEntity>> = shiftLogDao.getAllLogsLiveData()
    val allWorkersLiveData: LiveData<List<WorkerEntity>> = workerDao.getAllWorkersLiveData()
    val latestWorkerLiveData: LiveData<WorkerEntity?> = workerDao.getLatestWorkerLiveData()
    val latestLogLiveData: LiveData<ShiftLogEntity?> = shiftLogDao.getLatestLogLiveData()

    fun getLogsByRiskLiveData(risk: String): LiveData<List<ShiftLogEntity>> {
        return if (risk == "ALL") {
            shiftLogDao.getAllLogsLiveData()
        } else {
            shiftLogDao.getLogsByRiskLiveData(risk)
        }
    }

    suspend fun getLatestWorker(): WorkerEntity? = withContext(Dispatchers.IO) {
        workerDao.getLatestWorker()
    }

    suspend fun saveScan(log: ShiftLogEntity): Boolean = withContext(Dispatchers.IO) {
        // 1. Save locally with status PENDING
        shiftLogDao.insertLog(log)

        // 2. Attempt immediate online cloud sync
        try {
            val dto = toDto(log)
            val response = apiService.syncShiftLogs(SyncLogsRequest(listOf(dto)))
            if (response.isSuccessful && response.body()?.success == true) {
                shiftLogDao.updateSyncStatus(log.logId, "SYNCED")
                return@withContext true
            }
        } catch (e: Exception) {
            // Network failure: schedule background WorkManager sync
            enqueueWorkManagerSync()
        }
        return@withContext false
    }

    suspend fun registerWorkerAndBadge(worker: WorkerEntity): Boolean = withContext(Dispatchers.IO) {
        // 1. Save locally
        workerDao.insertWorker(worker)

        // 2. Push to cloud
        try {
            val req = RegisterWorkerRequest(
                name = worker.name,
                workerId = worker.workerId,
                department = worker.department,
                email = worker.email
            )
            val response = apiService.registerWorker(req)
            if (response.isSuccessful && response.body()?.success == true) {
                workerDao.updateSyncStatus(worker.workerId, "SYNCED")
                return@withContext true
            }
        } catch (e: Exception) {
            // Keep local
        }
        return@withContext false
    }

    suspend fun syncPendingLogsToCloud(): Int = withContext(Dispatchers.IO) {
        val pending = shiftLogDao.getPendingLogs()
        if (pending.isEmpty()) return@withContext 0

        val dtoList = pending.map { toDto(it) }
        try {
            val response = apiService.syncShiftLogs(SyncLogsRequest(dtoList))
            if (response.isSuccessful && response.body()?.success == true) {
                for (log in pending) {
                    shiftLogDao.updateSyncStatus(log.logId, "SYNCED")
                }
                return@withContext pending.size
            }
        } catch (e: Exception) {
            // Retry later
        }
        return@withContext 0
    }

    suspend fun fetchCloudLogsAndCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getShiftLogs(page = 1, limit = 50)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data ?: emptyList()
                val entities = data.map { toEntity(it) }
                if (entities.isNotEmpty()) {
                    shiftLogDao.insertAll(entities)
                }
                return@withContext true
            }
        } catch (e: Exception) {
            // Fallback to local
        }
        return@withContext false
    }

    suspend fun fetchDashboardMetrics(): DashboardMetricsDto? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDashboardMetrics()
            if (response.isSuccessful && response.body()?.success == true) {
                return@withContext response.body()?.metrics
            }
        } catch (e: Exception) {
            // Compute from local Room database when offline
            val total = shiftLogDao.getTotalCount()
            val totalDose = shiftLogDao.getTotalDose() ?: 0.0
            val maxTwa = shiftLogDao.getMaxTwa() ?: 0.0
            val safe = shiftLogDao.getCountByRisk("SAFE")
            val caution = shiftLogDao.getCountByRisk("CAUTION") + shiftLogDao.getCountByRisk("MODERATE")
            val high = shiftLogDao.getCountByRisk("HIGH")
            val critical = shiftLogDao.getCountByRisk("CRITICAL")

            return@withContext DashboardMetricsDto(
                totalScans = total,
                safeCount = safe,
                cautionCount = caution,
                highCount = high,
                criticalCount = critical,
                totalDosePpmHr = Math.round(totalDose * 100.0) / 100.0,
                avgTwaPpm = Math.round(maxTwa * 100.0) / 100.0,
                complianceRatePct = if (total > 0) Math.round(((safe + caution).toDouble() / total) * 1000.0) / 10.0 else 100.0
            )
        }
        return@withContext null
    }

    fun enqueueWorkManagerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    private fun toDto(entity: ShiftLogEntity): ShiftLogDto {
        return ShiftLogDto(
            logId = entity.logId,
            workerId = entity.workerId,
            workerName = entity.workerName,
            department = entity.department,
            bandId = entity.bandId,
            shiftDate = entity.shiftDate,
            scanTime = entity.scanTime,
            durationHours = entity.durationHours,
            expiryStatus = entity.expiryStatus,
            rawDeltaE = entity.rawDeltaE,
            estimatedDosePpmHr = entity.estimatedDosePpmHr,
            uncertaintyPpmHr = entity.uncertaintyPpmHr,
            twa8hrPpm = entity.twa8hrPpm,
            riskLevel = entity.riskLevel,
            actionRequired = entity.actionRequired,
            imageHash = entity.imageHash,
            syncStatus = "SYNCED"
        )
    }

    private fun toEntity(dto: ShiftLogDto): ShiftLogEntity {
        return ShiftLogEntity(
            logId = dto.logId,
            workerId = dto.workerId,
            workerName = dto.workerName,
            department = dto.department,
            bandId = dto.bandId,
            shiftDate = dto.shiftDate,
            scanTime = dto.scanTime,
            durationHours = dto.durationHours,
            expiryStatus = dto.expiryStatus,
            rawDeltaE = dto.rawDeltaE,
            estimatedDosePpmHr = dto.estimatedDosePpmHr,
            uncertaintyPpmHr = dto.uncertaintyPpmHr,
            twa8hrPpm = dto.twa8hrPpm,
            riskLevel = dto.riskLevel,
            actionRequired = dto.actionRequired,
            imageHash = dto.imageHash,
            syncStatus = "SYNCED"
        )
    }
}
