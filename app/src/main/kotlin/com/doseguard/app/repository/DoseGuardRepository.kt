package com.doseguard.app.repository

import android.content.Context
import com.doseguard.app.database.AppDatabase
import com.doseguard.app.imageprocessing.ExposureEstimator
import com.doseguard.app.imageprocessing.KineticColorEstimator
import com.doseguard.app.model.AlertEntity
import com.doseguard.app.model.BandAssignmentEntity
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.ExposureHistoryEntity
import com.doseguard.app.model.WorkerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * DoseGuardRepository — single source of truth for all app data.
 *
 * Mediates between ViewModels and Room DAOs.
 * Fixes cumulative double counting by updating cumulative dose directly from latest optical reading.
 */
class DoseGuardRepository(
    context: Context,
    private val estimator: ExposureEstimator = KineticColorEstimator()
) {

    private val db = AppDatabase.getInstance(context)
    private val workerDao = db.workerDao()
    private val bandDao = db.bandDao()
    private val historyDao = db.exposureHistoryDao()
    private val alertDao = db.alertDao()
    private val assignmentDao = db.bandAssignmentDao()

    sealed class BandValidity {
        object Valid : BandValidity()
        data class Invalid(val reason: String, val message: String) : BandValidity()
    }

    /** Result of validating a scanned band before it can be issued to a worker. */
    sealed class AssignmentCheck {
        /** Band exists, is unassigned, in date, and ready to issue. */
        data class Available(val band: BandEntity) : AssignmentCheck()
        /** Band is already worn by someone (holder is null if the worker record is missing). */
        data class AlreadyAssigned(val band: BandEntity, val holder: WorkerEntity?) : AssignmentCheck()
        data class Expired(val band: BandEntity) : AssignmentCheck()
        data class Saturated(val band: BandEntity) : AssignmentCheck()
        data class Retired(val band: BandEntity) : AssignmentCheck()
        data class NotFound(val bandId: String) : AssignmentCheck()
    }

    // ── Reactive flows for UI ────────────────────────────────────────────────────

    val allWorkersFlow: Flow<List<WorkerEntity>> = workerDao.getAllFlow()
    val allBandsFlow: Flow<List<BandEntity>> = bandDao.getAllFlow()
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

    suspend fun getAllWorkers(): List<WorkerEntity> = withContext(Dispatchers.IO) {
        workerDao.getAll()
    }

    /**
     * Industry-database lookup used by the Band Assignment flow.
     * Accepts an internal worker id, an employee number, or a raw employee-card
     * QR / barcode payload such as "DG:EMP:EMP02345" or "…/worker/EMP02345".
     */
    suspend fun findWorker(identifier: String): WorkerEntity? = withContext(Dispatchers.IO) {
        val cleaned = parseWorkerIdentifier(identifier)
        if (cleaned.isBlank()) null else workerDao.findByIdentifier(cleaned)
    }

    fun parseWorkerIdentifier(raw: String): String {
        val t = raw.trim()
        val u = t.uppercase()
        val prefixes = listOf("DG:EMP:", "DG:WORKER:", "EMPID:", "/WORKER/")
        for (p in prefixes) {
            val i = u.lastIndexOf(p)
            if (i >= 0) return t.substring(i + p.length).trim()
        }
        return t
    }

    fun parseBandIdentifier(raw: String): String {
        val t = raw.trim()
        val u = t.uppercase()
        val prefixes = listOf("DG:BAND:", "/BAND/")
        for (p in prefixes) {
            val i = u.lastIndexOf(p)
            if (i >= 0) return t.substring(i + p.length).trim()
        }
        return t
    }

    // ── Band operations & Validity Gate ──────────────────────────────────────────

    suspend fun getBandById(bandId: String): BandEntity? = withContext(Dispatchers.IO) {
        bandDao.getById(bandId)
    }

    suspend fun registerNewBand(bandId: String, qrData: String): BandEntity = withContext(Dispatchers.IO) {
        val band = BandEntity(bandId = bandId, qrData = qrData)
        bandDao.insert(band)
        band
    }

    suspend fun assignBandToWorker(bandId: String, workerId: String) = withContext(Dispatchers.IO) {
        bandDao.assignWorker(bandId, workerId)
    }

    // ── Band Assignment flow (Worker → Band mapping) ─────────────────────────────

    /**
     * Validate a scanned band QR payload before it is issued to a worker.
     * Checks existence, current assignment, shelf-life expiry and saturation.
     */
    suspend fun checkBandForAssignment(rawQr: String): AssignmentCheck = withContext(Dispatchers.IO) {
        val bandId = parseBandIdentifier(rawQr)
        val band = bandDao.getById(bandId) ?: return@withContext AssignmentCheck.NotFound(bandId)
        val now = System.currentTimeMillis()

        if (band.bandStatus == "REPLACED") return@withContext AssignmentCheck.Retired(band)

        if (band.bandStatus == "EXPIRED" || now > band.expiryDate) {
            bandDao.updateStatus(band.bandId, "EXPIRED")
            return@withContext AssignmentCheck.Expired(band.copy(bandStatus = "EXPIRED"))
        }

        if (band.bandStatus == "SATURATED" || band.currentEstimatedDose >= band.maximumDose) {
            bandDao.updateStatus(band.bandId, "SATURATED")
            return@withContext AssignmentCheck.Saturated(band.copy(bandStatus = "SATURATED"))
        }

        if (band.workerId.isNotBlank()) {
            val holder = workerDao.getById(band.workerId)
            return@withContext AssignmentCheck.AlreadyAssigned(band, holder)
        }

        AssignmentCheck.Available(band)
    }

    /**
     * Commit the Worker ↔ Band mapping.
     *
     *  bands            → workerId = worker, bandStatus = ACTIVE, issueDate = now
     *  band_assignments → new row (workerId, bandId, assignedTime)
     *
     * Any previously open assignment for either side is released first so a worker
     * only ever wears one live band.
     */
    suspend fun assignBand(workerId: String, bandId: String, assignedBy: String = "OPERATOR"): BandAssignmentEntity =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()

            assignmentDao.releaseForWorker(workerId, now)
            assignmentDao.releaseForBand(bandId, now)

            bandDao.assignWorkerWithIssueDate(bandId, workerId, now)

            val record = BandAssignmentEntity(
                assignmentId = UUID.randomUUID().toString(),
                workerId = workerId,
                bandId = bandId,
                assignedTime = now,
                assignedBy = assignedBy,
                status = "ACTIVE"
            )
            assignmentDao.insert(record)
            record
        }

    suspend fun getActiveAssignmentForWorker(workerId: String): BandAssignmentEntity? =
        withContext(Dispatchers.IO) { assignmentDao.getActiveForWorker(workerId) }

    fun getAssignmentsForWorkerFlow(workerId: String): Flow<List<BandAssignmentEntity>> =
        assignmentDao.getByWorkerFlow(workerId)

    val allAssignmentsFlow: Flow<List<BandAssignmentEntity>> = assignmentDao.getAllFlow()

    /**
     * Replacement-Band Flow:
     * Retires the old expired/saturated band and links the new band to the existing worker.
     * Preserves continuous health history without creating duplicate worker rows.
     */
    suspend fun replaceBandForWorker(oldBandId: String, newBandId: String, workerId: String) = withContext(Dispatchers.IO) {
        if (oldBandId.isNotBlank()) {
            bandDao.markBandReplaced(oldBandId)
        }
        bandDao.assignWorker(newBandId, workerId)
    }

    /**
     * Check if a band is still safe and valid for scanning.
     * Detects EXPIRED shelf-life, SATURATED optical limit, or REPLACED retirement.
     */
    suspend fun evaluateBandValidity(band: BandEntity): BandValidity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        if (band.bandStatus == "REPLACED") {
            return@withContext BandValidity.Invalid(
                reason = "REPLACED",
                message = "This dosimeter wristband has been retired and replaced with a new unit."
            )
        }

        if (band.bandStatus == "EXPIRED" || now > band.expiryDate) {
            bandDao.updateStatus(band.bandId, "EXPIRED")
            return@withContext BandValidity.Invalid(
                reason = "EXPIRED",
                message = "Dosimeter chemical shelf-life expired. Sensor strip chemistry is no longer calibrated."
            )
        }

        if (band.bandStatus == "SATURATED" || band.currentEstimatedDose >= band.maximumDose) {
            bandDao.updateStatus(band.bandId, "SATURATED")
            return@withContext BandValidity.Invalid(
                reason = "SATURATED",
                message = "Maximum dose capacity (${band.maximumDose} ppm·hr) reached. Sensor strip is fully saturated."
            )
        }

        BandValidity.Valid
    }

    fun getHistoryByWorkerFlow(workerId: String): Flow<List<ExposureHistoryEntity>> =
        historyDao.getByWorkerFlow(workerId)

    fun getHistoryByBandFlow(bandId: String): Flow<List<ExposureHistoryEntity>> =
        historyDao.getByBandFlow(bandId)

    fun getRecentHistoryFlow(workerId: String, since: Long): Flow<List<ExposureHistoryEntity>> =
        historyDao.getRecentByWorkerFlow(workerId, since)

    // ── Core scan & save (Direct Cumulative Dose Update) ─────────────────────────

    /**
     * Process a captured image and save the result.
     * Updates cumulative dose directly from the physical strip reading (prevents double-counting).
     */
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

        // Set latest cumulative dose directly — fixes double count bug
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

    /**
     * Simulated scan — skips real image processing and uses a synthetic ΔE value.
     */
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

        // Set latest cumulative dose directly — fixes double count bug
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
