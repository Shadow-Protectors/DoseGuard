package com.doseguard.app.repository

import android.content.Context
import androidx.room.withTransaction
import com.doseguard.app.database.AppDatabase
import com.doseguard.app.imageprocessing.ExposureEstimator
import com.doseguard.app.imageprocessing.KineticColorEstimator
import com.doseguard.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * DoseGuardRepository — single source of truth for all worker directory, dosimeter bands,
 * assignments, and exposure metrics.
 */
class DoseGuardRepository(
    context: Context,
    private val estimator: ExposureEstimator = KineticColorEstimator()
) {

    private val db = AppDatabase.getInstance(context)
    private val workerDao = db.workerDao()
    private val bandDao = db.bandDao()
    private val assignmentDao = db.bandAssignmentDao()
    private val historyDao = db.exposureHistoryDao()
    private val alertDao = db.alertDao()

    sealed class BandValidationResult {
        data class Valid(val band: BandEntity) : BandValidationResult()
        data class Invalid(val reason: String, val message: String) : BandValidationResult()
    }

    // ── Reactive flows for UI ────────────────────────────────────────────────────

    val allWorkersFlow: Flow<List<WorkerEntity>> = workerDao.getAllFlow()
    val allBandsFlow: Flow<List<BandEntity>> = bandDao.getAllFlow()
    val availableBandsFlow: Flow<List<BandEntity>> = bandDao.getAvailableBandsFlow()
    val allHistoryFlow: Flow<List<ExposureHistoryEntity>> = historyDao.getAllFlow()
    val activeAlertsFlow: Flow<List<AlertEntity>> = alertDao.getActiveAlertsFlow()
    val activeAlertCountFlow: Flow<Int> = alertDao.getActiveCountFlow()
    val totalScanCountFlow: Flow<Int> = historyDao.getTotalCountFlow()

    // ── Worker operations ────────────────────────────────────────────────────────

    suspend fun registerWorker(worker: WorkerEntity) = withContext(Dispatchers.IO) {
        workerDao.insert(worker)
    }

    suspend fun getWorkerById(workerId: String): WorkerEntity? = withContext(Dispatchers.IO) {
        workerDao.getById(workerId)
    }

    suspend fun getWorkerByEmployeeId(employeeId: String): WorkerEntity? = withContext(Dispatchers.IO) {
        workerDao.getByEmployeeId(employeeId)
    }

    suspend fun getAllWorkers(): List<WorkerEntity> = withContext(Dispatchers.IO) {
        workerDao.getAll()
    }

    // ── Band operations & Validations ────────────────────────────────────────────

    suspend fun getBandById(bandId: String): BandEntity? = withContext(Dispatchers.IO) {
        bandDao.getById(bandId)
    }

    suspend fun getActiveBandForWorker(workerId: String): BandEntity? = withContext(Dispatchers.IO) {
        bandDao.getActiveByWorker(workerId)
    }

    suspend fun registerNewBand(bandId: String, qrData: String): BandEntity = withContext(Dispatchers.IO) {
        val band = BandEntity(bandId = bandId, qrData = qrData)
        bandDao.insert(band)
        band
    }

    /**
     * Validates a scanned or entered band for assignment to a target worker.
     * Checks database existence, previous assignments, shelf-life expiration, and saturation.
     */
    suspend fun validateBandForAssignment(bandId: String, targetWorkerId: String): BandValidationResult = withContext(Dispatchers.IO) {
        val band = bandDao.getById(bandId)
            ?: return@withContext BandValidationResult.Invalid(
                reason = "NOT_FOUND",
                message = "Unknown band ($bandId). Not registered in the industrial inventory system."
            )

        val now = System.currentTimeMillis()

        // 1. Expiry Check
        if (band.bandStatus == "EXPIRED" || now > band.expiryDate) {
            bandDao.updateStatus(bandId, "EXPIRED")
            return@withContext BandValidationResult.Invalid(
                reason = "EXPIRED",
                message = "Chemical shelf life expired. Sensor strip reagent is no longer calibrated."
            )
        }

        // 2. Saturation Check
        if (band.bandStatus == "SATURATED" || band.currentEstimatedDose >= band.maximumDose) {
            bandDao.updateStatus(bandId, "SATURATED")
            return@withContext BandValidationResult.Invalid(
                reason = "SATURATED",
                message = "Strip saturated (${band.currentEstimatedDose} / ${band.maximumDose} ppm·hr). Maximum capacity reached."
            )
        }

        // 3. Already Assigned to Another Worker Check
        if (band.workerId.isNotBlank() && band.workerId != targetWorkerId && band.bandStatus in listOf("ACTIVE", "ASSIGNED")) {
            val assignedWorker = workerDao.getById(band.workerId)
            val workerName = assignedWorker?.name ?: "another worker"
            val empId = assignedWorker?.employeeId ?: band.workerId
            return@withContext BandValidationResult.Invalid(
                reason = "ALREADY_ASSIGNED",
                message = "Already assigned to $workerName ($empId)."
            )
        }

        BandValidationResult.Valid(band)
    }

    /**
     * Executes the atomic assignment of a band to a worker.
     * 1. Releases any previous active assignment for that worker or band
     * 2. Updates the band record with workerId, ACTIVE status, and issue timestamp
     * 3. Inserts a new BandAssignment audit log record
     */
    suspend fun assignBandToWorkerTransaction(workerId: String, bandId: String): BandAssignmentEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Release previous active assignments for this worker
        assignmentDao.releaseActiveForWorker(workerId, now)
        bandDao.unassignPreviousBandsForWorker(workerId)

        // 2. Release any active assignment previously attached to this specific band
        assignmentDao.releaseActiveForBand(bandId, now)

        // 3. Assign and update band table
        bandDao.assignWorker(bandId, workerId, now)

        // 4. Create new audit assignment entry
        val assignment = BandAssignmentEntity(
            assignmentId = "ASG-${UUID.randomUUID().toString().take(8).uppercase()}",
            workerId = workerId,
            bandId = bandId,
            assignedAt = now,
            status = "ACTIVE"
        )
        assignmentDao.insert(assignment)

        assignment
    }

    suspend fun assignBandToWorker(bandId: String, workerId: String): BandAssignmentEntity =
        assignBandToWorkerTransaction(workerId, bandId)

    fun getHistoryByWorkerFlow(workerId: String): Flow<List<ExposureHistoryEntity>> =
        historyDao.getByWorkerFlow(workerId)

    fun getRecentHistoryFlow(workerId: String, since: Long): Flow<List<ExposureHistoryEntity>> =
        historyDao.getRecentByWorkerFlow(workerId, since)

    // ── Core scan & save (Direct Cumulative Dose Update) ─────────────────────────

    suspend fun processScanAndSave(
        bandId: String,
        workerId: String,
        imageBytes: ByteArray,
        imagePath: String,
        shiftHours: Double = 8.0,
        temperature: Double = 25.0,
        humidity: Double = 60.0
    ): ExposureEstimator.EstimationResult = withContext(Dispatchers.IO) {

        val result = estimator.estimate(imageBytes, shiftHours)
        val now = System.currentTimeMillis()

        historyDao.insert(
            ExposureHistoryEntity(
                historyId = UUID.randomUUID().toString(),
                workerId = workerId,
                bandId = bandId,
                scanTime = now,
                estimatedDose = result.estimatedDosePpmHr,
                confidence = result.confidence,
                imagePath = imagePath,
                riskLevel = result.riskLevel,
                temperature = temperature,
                humidity = humidity
            )
        )

        // Update cumulative dose directly
        bandDao.updateDose(bandId, result.estimatedDosePpmHr, now)

        // Fire alert if HIGH or CRITICAL
        if (result.riskLevel == "HIGH" || result.riskLevel == "CRITICAL") {
            alertDao.insert(
                AlertEntity(
                    alertId = UUID.randomUUID().toString(),
                    workerId = workerId,
                    bandId = bandId,
                    triggerDose = result.estimatedDosePpmHr,
                    riskLevel = result.riskLevel
                )
            )
        }

        result
    }

    suspend fun processSimulatedScan(
        bandId: String,
        workerId: String,
        deltaE: Double,
        shiftHours: Double = 8.0
    ): ExposureEstimator.EstimationResult = withContext(Dispatchers.IO) {

        val solver = estimator as? KineticColorEstimator ?: KineticColorEstimator()
        val result = solver.estimateFromDeltaE(deltaE, shiftHours)

        val now = System.currentTimeMillis()
        historyDao.insert(
            ExposureHistoryEntity(
                historyId = UUID.randomUUID().toString(),
                workerId = workerId,
                bandId = bandId,
                scanTime = now,
                estimatedDose = result.estimatedDosePpmHr,
                confidence = result.confidence,
                imagePath = "",
                riskLevel = result.riskLevel
            )
        )

        bandDao.updateDose(bandId, result.estimatedDosePpmHr, now)

        if (result.riskLevel == "HIGH" || result.riskLevel == "CRITICAL") {
            alertDao.insert(
                AlertEntity(
                    alertId = UUID.randomUUID().toString(),
                    workerId = workerId,
                    bandId = bandId,
                    triggerDose = result.estimatedDosePpmHr,
                    riskLevel = result.riskLevel
                )
            )
        }
        result
    }

    // ── Alert operations ─────────────────────────────────────────────────────────

    suspend fun acknowledgeAlert(alertId: String) = withContext(Dispatchers.IO) {
        alertDao.updateStatus(alertId, "ACKNOWLEDGED")
    }

    suspend fun resolveAlert(alertId: String) = withContext(Dispatchers.IO) {
        alertDao.updateStatus(alertId, "RESOLVED")
    }

    suspend fun acknowledgeAllForWorker(workerId: String) = withContext(Dispatchers.IO) {
        alertDao.acknowledgeForWorker(workerId)
    }
}
