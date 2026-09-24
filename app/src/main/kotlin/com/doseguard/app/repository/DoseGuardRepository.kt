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
 * The exposureEstimator is injected via constructor so it can be swapped for testing
 * or for the final TFLite-backed implementation.
 *
 * [AI_INTEGRATION_POINT] — pass TFLiteEstimator() here instead of KineticColorEstimator()
 *   when the real model is available.
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

    // ── Band operations ──────────────────────────────────────────────────────────

    /**
     * Look up a band by its QR-derived ID.
     * Returns null if the band has never been scanned before.
     */
    suspend fun getBandById(bandId: String): BandEntity? = withContext(Dispatchers.IO) {
        bandDao.getById(bandId)
    }

    /**
     * Register a brand-new band from a first-time QR scan.
     * bandStatus starts as UNASSIGNED until a worker is linked.
     */
    suspend fun registerNewBand(bandId: String, qrData: String): BandEntity = withContext(Dispatchers.IO) {
        val band = BandEntity(bandId = bandId, qrData = qrData)
        bandDao.insert(band)
        band
    }

    /** Link a band to a worker (called after registration form is submitted). */
    suspend fun assignBandToWorker(bandId: String, workerId: String) = withContext(Dispatchers.IO) {
        bandDao.assignWorker(bandId, workerId)
    }

    fun getHistoryByWorkerFlow(workerId: String): Flow<List<ExposureHistoryEntity>> =
        historyDao.getByWorkerFlow(workerId)

    fun getRecentHistoryFlow(workerId: String, since: Long): Flow<List<ExposureHistoryEntity>> =
        historyDao.getRecentByWorkerFlow(workerId, since)

    // ── Core scan & save ─────────────────────────────────────────────────────────

    /**
     * Process a captured image and save the result.
     *
     * Steps:
     * 1. Run ExposureEstimator on the image bytes
     * 2. Persist ExposureHistoryEntity
     * 3. Accumulate dose on BandEntity
     * 4. Create AlertEntity if above threshold
     *
     * @return The EstimationResult so the caller (ViewModel) can drive UI state.
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

        val historyId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        historyDao.insert(
            ExposureHistoryEntity(
                historyId = historyId,
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

        // Accumulate on the band
        bandDao.addDose(bandId, result.estimatedDosePpmHr, now)

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
     * Used by demo preset chips (Safe / Caution / Critical buttons).
     */
    suspend fun processSimulatedScan(
        bandId: String,
        workerId: String,
        deltaE: Double,
        shiftHours: Double = 8.0
    ): ExposureEstimator.EstimationResult = withContext(Dispatchers.IO) {

        // [AI_INTEGRATION_POINT] — this branch will be removed once real camera flow is complete
        val solver = estimator as? KineticColorEstimator
            ?: KineticColorEstimator()
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
        bandDao.addDose(bandId, result.estimatedDosePpmHr, now)

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
