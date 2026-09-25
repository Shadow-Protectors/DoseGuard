package com.doseguard.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.doseguard.app.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * DoseGuard Room Database — offline-first local storage.
 * Includes workers, bands, band_assignments, exposure_history, and alerts.
 */
@Database(
    entities = [
        WorkerEntity::class,
        BandEntity::class,
        BandAssignmentEntity::class,
        ExposureHistoryEntity::class,
        AlertEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workerDao(): WorkerDao
    abstract fun bandDao(): BandDao
    abstract fun bandAssignmentDao(): BandAssignmentDao
    abstract fun exposureHistoryDao(): ExposureHistoryDao
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "doseguard_v2.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { db ->
                        INSTANCE = db
                        CoroutineScope(Dispatchers.IO).launch {
                            seedDemoData(db)
                        }
                    }
            }
        }

        private suspend fun seedDemoData(db: AppDatabase) {
            val workerDao = db.workerDao()
            val bandDao = db.bandDao()
            val assignmentDao = db.bandAssignmentDao()
            val historyDao = db.exposureHistoryDao()
            val alertDao = db.alertDao()

            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            // ── Pre-enrolled Industry Workers ─────────────────────────────────
            val workers = listOf(
                WorkerEntity("W-1001", "EMP-1052", "Arun Kumar", "Gas Processing", "Process Operator", "Morning", "ACTIVE", now - (30 * dayMs)),
                WorkerEntity("W-1002", "EMP02345", "John Mathew", "Gas Processing", "Field Technician", "Morning", "ACTIVE", now - (25 * dayMs)),
                WorkerEntity("W-1003", "EMP02346", "Anita Desai", "Desulfurization Unit", "Safety Inspector", "Evening", "ACTIVE", now - (20 * dayMs)),
                WorkerEntity("W-1004", "EMP-7821", "Rajesh Kumar", "Refinery Sweetening Unit", "Plant Operator", "Morning", "ACTIVE", now - (15 * dayMs)),
                WorkerEntity("W-1005", "EMP-9043", "Priya Sharma", "Catalytic Cracking Unit", "Shift Supervisor", "Night", "ACTIVE", now - (10 * dayMs))
            )
            workers.forEach { if (workerDao.getById(it.workerId) == null) workerDao.insert(it) }

            // ── Inventory Dosimeter Bands ─────────────────────────────────────
            // 1. Available Fresh Bands
            if (bandDao.getById("BAND-001285") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "BAND-001285",
                        workerId = "",
                        batchNo = "BATCH-2026-A1",
                        qrData = "DG:BAND:BAND-001285",
                        issueDate = now,
                        expiryDate = now + (30 * dayMs),
                        bandStatus = "AVAILABLE",
                        maximumDose = 50.0,
                        currentEstimatedDose = 0.0
                    )
                )
            }
            if (bandDao.getById("WB-2005") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "WB-2005",
                        workerId = "",
                        batchNo = "BATCH-2026-A2",
                        qrData = "DG:BAND:WB-2005",
                        issueDate = now,
                        expiryDate = now + (30 * dayMs),
                        bandStatus = "AVAILABLE",
                        maximumDose = 50.0,
                        currentEstimatedDose = 0.0
                    )
                )
            }

            // 2. Active Band Assigned to Rajesh Kumar (W-1004)
            if (bandDao.getById("WB-1001") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "WB-1001",
                        workerId = "W-1004",
                        batchNo = "BATCH-2026-A1",
                        qrData = "DG:BAND:WB-1001",
                        issueDate = now - (7 * dayMs),
                        expiryDate = now + (23 * dayMs),
                        bandStatus = "ACTIVE",
                        maximumDose = 50.0,
                        currentEstimatedDose = 3.2,
                        lastScanTime = now - (2 * 3600_000L)
                    )
                )
                assignmentDao.insert(
                    BandAssignmentEntity("ASG-1001", "W-1004", "WB-1001", now - (7 * dayMs), "ACTIVE")
                )
            }

            // 3. Already Assigned Band (Anita Desai - W-1003)
            if (bandDao.getById("WB-ASSIGNED-01") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "WB-ASSIGNED-01",
                        workerId = "W-1003",
                        batchNo = "BATCH-2026-A1",
                        qrData = "DG:BAND:WB-ASSIGNED-01",
                        issueDate = now - (3 * dayMs),
                        expiryDate = now + (27 * dayMs),
                        bandStatus = "ACTIVE",
                        maximumDose = 50.0,
                        currentEstimatedDose = 1.8,
                        lastScanTime = now - (4 * 3600_000L)
                    )
                )
                assignmentDao.insert(
                    BandAssignmentEntity("ASG-1003", "W-1003", "WB-ASSIGNED-01", now - (3 * dayMs), "ACTIVE")
                )
            }

            // 4. EXPIRED Band (Chemical Shelf-Life Past Expiry)
            if (bandDao.getById("WB-EXP-01") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "WB-EXP-01",
                        workerId = "",
                        batchNo = "BATCH-2025-Z9",
                        qrData = "DG:BAND:WB-EXP-01",
                        issueDate = now - (40 * dayMs),
                        expiryDate = now - (2 * dayMs), // Expired 2 days ago
                        bandStatus = "EXPIRED",
                        maximumDose = 50.0,
                        currentEstimatedDose = 0.0
                    )
                )
            }

            // 5. SATURATED Band (Maximum 50.0 ppm·hr Dose Reached)
            if (bandDao.getById("WB-SAT-99") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "WB-SAT-99",
                        workerId = "",
                        batchNo = "BATCH-2026-A1",
                        qrData = "DG:BAND:WB-SAT-99",
                        issueDate = now - (10 * dayMs),
                        expiryDate = now + (20 * dayMs),
                        bandStatus = "SATURATED",
                        maximumDose = 50.0,
                        currentEstimatedDose = 50.0,
                        lastScanTime = now - 1800_000L
                    )
                )
            }

            // 30 Days of Historical Shift Exposure Records for Rajesh Kumar (W-1004)
            val sampleHistoryDays = listOf(
                Pair(now - (28 * dayMs), 0.30),
                Pair(now - (25 * dayMs), 0.35),
                Pair(now - (22 * dayMs), 0.40),
                Pair(now - (18 * dayMs), 0.25),
                Pair(now - (14 * dayMs), 0.50),
                Pair(now - (10 * dayMs), 0.45),
                Pair(now - (7 * dayMs),  0.30),
                Pair(now - (5 * dayMs),  0.60),
                Pair(now - (3 * dayMs),  0.40),
                Pair(now - (1 * dayMs),  0.35)
            )
            sampleHistoryDays.forEachIndexed { i, (time, dose) ->
                val hId = "HIST-$i"
                historyDao.insert(
                    ExposureHistoryEntity(
                        historyId = hId,
                        workerId = "W-1004",
                        bandId = "WB-1001",
                        scanTime = time,
                        estimatedDose = dose,
                        confidence = 0.95,
                        imagePath = "",
                        riskLevel = "SAFE",
                        temperature = 26.0,
                        humidity = 58.0
                    )
                )
            }
        }
    }
}
