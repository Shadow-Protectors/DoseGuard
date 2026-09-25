package com.doseguard.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.doseguard.app.model.AlertEntity
import com.doseguard.app.model.BandAssignmentEntity
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.ExposureHistoryEntity
import com.doseguard.app.model.WorkerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * DoseGuard Room Database — offline-first local storage.
 * Includes pre-seeded demo records for seamless offline evaluation.
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
                    "doseguard_v3.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed sample demo data on database creation
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { seedDemoData(it) }
                            }
                        }
                    })
                    .build()
                    .also { 
                        INSTANCE = it
                        // Ensure demo data exists
                        CoroutineScope(Dispatchers.IO).launch {
                            seedDemoData(it)
                        }
                    }
            }
        }

        private suspend fun seedDemoData(db: AppDatabase) {
            val workerDao = db.workerDao()
            val bandDao = db.bandDao()
            val historyDao = db.exposureHistoryDao()
            val alertDao = db.alertDao()

            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            if (workerDao.getById("W-1001") == null) {
                workerDao.insert(
                    WorkerEntity(
                        workerId = "W-1001",
                        employeeId = "EMP-7821",
                        name = "Rajesh Kumar",
                        department = "Refinery Sweetening Unit",
                        designation = "Plant Operator",
                        shift = "Morning",
                        status = "ACTIVE",
                        createdAt = now - (30 * dayMs)
                    )
                )
            }

            if (workerDao.getById("W-1002") == null) {
                workerDao.insert(
                    WorkerEntity(
                        workerId = "W-1002",
                        employeeId = "EMP-9043",
                        name = "Priya Sharma",
                        department = "Catalytic Cracking Unit",
                        designation = "Safety Inspector",
                        shift = "Evening",
                        status = "ACTIVE",
                        createdAt = now - (15 * dayMs)
                    )
                )
            }

            // 1. Active & Valid Band
            if (bandDao.getById("WB-1001") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "WB-1001",
                        workerId = "W-1001",
                        qrData = "DG:BAND:WB-1001",
                        issueDate = now - (7 * dayMs),
                        expiryDate = now + (30 * dayMs),
                        bandStatus = "ASSIGNED",
                        maximumDose = 50.0,
                        currentEstimatedDose = 3.2,
                        lastScanTime = now - (2 * 3600_000L)
                    )
                )
            }

            // 2. EXPIRED Band (Shelf Life Past Expiry)
            if (bandDao.getById("WB-EXP-01") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "WB-EXP-01",
                        workerId = "W-1001",
                        qrData = "DG:BAND:WB-EXP-01",
                        issueDate = now - (40 * dayMs),
                        expiryDate = now - (2 * dayMs), // Expired 2 days ago
                        bandStatus = "EXPIRED",
                        maximumDose = 50.0,
                        currentEstimatedDose = 4.8,
                        lastScanTime = now - (3 * dayMs)
                    )
                )
            }

            // 3. SATURATED Band (Maximum 50.0 ppm·hr Dose Reached)
            if (bandDao.getById("WB-SAT-99") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "WB-SAT-99",
                        workerId = "W-1001",
                        qrData = "DG:BAND:WB-SAT-99",
                        issueDate = now - (10 * dayMs),
                        expiryDate = now + (20 * dayMs),
                        bandStatus = "SATURATED",
                        maximumDose = 50.0,
                        currentEstimatedDose = 50.0, // 100% capacity reached
                        lastScanTime = now - 1800_000L
                    )
                )
            }

            // 4. Critical Active Band
            if (bandDao.getById("WB-CRIT-99") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "WB-CRIT-99",
                        workerId = "W-1001",
                        qrData = "DG:BAND:WB-CRIT-99",
                        issueDate = now - (5 * dayMs),
                        expiryDate = now + (15 * dayMs),
                        bandStatus = "ASSIGNED",
                        maximumDose = 50.0,
                        currentEstimatedDose = 42.5,
                        lastScanTime = now - 1800_000L
                    )
                )
            }

            // Industry worker directory — searchable by Employee ID in Band Assignment
            val directory = listOf(
                WorkerEntity("W-1003", "EMP02345", "John Mathew", "Gas Processing", "Field Technician", "Morning", "ACTIVE", now - (60 * dayMs)),
                WorkerEntity("W-1004", "EMP02346", "Anita Desai", "Sour Water Stripper", "Process Engineer", "Evening", "ACTIVE", now - (45 * dayMs)),
                WorkerEntity("W-1005", "EMP02347", "Mohammed Farid", "Amine Treating Unit", "Maintenance Fitter", "Night", "ACTIVE", now - (20 * dayMs)),
                WorkerEntity("W-1006", "EMP02348", "Sara Thomas", "HSE Department", "Safety Officer", "Morning", "ACTIVE", now - (10 * dayMs))
            )
            for (w in directory) {
                if (workerDao.getById(w.workerId) == null) workerDao.insert(w)
            }

            // Available (unassigned) bands ready to be issued
            val availableBands = listOf("BAND-001285", "BAND-001286", "BAND-001287", "BAND-001288")
            for (id in availableBands) {
                if (bandDao.getById(id) == null) {
                    bandDao.insert(
                        BandEntity(
                            bandId = id,
                            workerId = "",
                            qrData = "DG:BAND:$id",
                            issueDate = now,
                            expiryDate = now + (45 * dayMs),
                            bandStatus = "UNASSIGNED"
                        )
                    )
                }
            }

            // An already-assigned band and an expired band, for validation demos
            if (bandDao.getById("BAND-001290") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "BAND-001290",
                        workerId = "W-1004",
                        qrData = "DG:BAND:BAND-001290",
                        issueDate = now - (3 * dayMs),
                        expiryDate = now + (27 * dayMs),
                        bandStatus = "ACTIVE"
                    )
                )
            }
            if (bandDao.getById("BAND-001291") == null) {
                bandDao.insert(
                    BandEntity(
                        bandId = "BAND-001291",
                        workerId = "",
                        qrData = "DG:BAND:BAND-001291",
                        issueDate = now - (60 * dayMs),
                        expiryDate = now - (5 * dayMs),
                        bandStatus = "EXPIRED"
                    )
                )
            }

            // 30 Days of Historical Shift Exposure Records for W-1001
            val sampleHistoryDays = listOf(
                Pair(now - (28 * dayMs), 0.30),
                Pair(now - (25 * dayMs), 0.35),
                Pair(now - (22 * dayMs), 0.40),
                Pair(now - (19 * dayMs), 0.45),
                Pair(now - (16 * dayMs), 0.50),
                Pair(now - (13 * dayMs), 0.65),
                Pair(now - (10 * dayMs), 0.80),
                Pair(now - (7 * dayMs), 0.95),
                Pair(now - (6 * dayMs), 1.10),
                Pair(now - (5 * dayMs), 1.45),
                Pair(now - (4 * dayMs), 1.85),
                Pair(now - (3 * dayMs), 2.20),
                Pair(now - (2 * dayMs), 2.65),
                Pair(now - (1 * dayMs), 2.90),
                Pair(now - (3600_000L), 3.20)
            )

            for ((time, dose) in sampleHistoryDays) {
                val risk = when {
                    dose < 1.0 -> "SAFE"
                    dose < 2.5 -> "MODERATE"
                    dose < 10.0 -> "HIGH"
                    else -> "CRITICAL"
                }
                val entity = ExposureHistoryEntity(
                    historyId = UUID.randomUUID().toString(),
                    workerId = "W-1001",
                    bandId = "WB-1001",
                    scanTime = time,
                    estimatedDose = dose,
                    confidence = 0.94,
                    imagePath = "",
                    riskLevel = risk,
                    temperature = 28.5,
                    humidity = 58.0
                )
                historyDao.insert(entity)
            }

            // Seed an active alert for W-1001
            alertDao.insert(
                AlertEntity(
                    alertId = "ALERT-001",
                    workerId = "W-1001",
                    bandId = "WB-CRIT-99",
                    triggerDose = 11.8,
                    riskLevel = "CRITICAL",
                    status = "ACTIVE",
                    createdAt = now - 1800_000L
                )
            )
        }
    }
}
