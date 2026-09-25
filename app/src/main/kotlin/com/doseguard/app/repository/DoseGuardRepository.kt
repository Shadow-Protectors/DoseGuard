package com.doseguard.app.repository

import android.content.Context
import com.doseguard.app.database.AppDatabase
import com.doseguard.app.imageprocessing.ExposureEstimator
import com.doseguard.app.imageprocessing.KineticColorEstimator
import com.doseguard.app.model.AlertEntity
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

    sealed class BandValidity {
        object Valid : BandValidity()
        data class Invalid(val reason: String, val message: String) : BandValidity()
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

    // ── Band operations & Validity Gate ──────────────────────────────────────────

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

    suspend fun assignBandToWorker(bandId: String, workerId: String) = withContext(Dispatchers.IO) {
        bandDao.assignWorker(bandId, workerId)
    }

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
