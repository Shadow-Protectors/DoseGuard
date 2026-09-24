package com.doseguard.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.doseguard.app.model.AlertEntity
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
        ExposureHistoryEntity::class,
        AlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workerDao(): WorkerDao
    abstract fun bandDao(): BandDao
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
                        createdAt = now - (7 * dayMs)
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
                        createdAt = now - (3 * dayMs)
                    )
                )
            }

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

            // Seed historical shift exposures for W-1001
            val sampleDays = listOf(
                Pair(now - (6 * dayMs), 0.42),
                Pair(now - (5 * dayMs), 0.55),
                Pair(now - (4 * dayMs), 0.85),
                Pair(now - (3 * dayMs), 1.20),
                Pair(now - (2 * dayMs), 1.65),
                Pair(now - (1 * dayMs), 2.40),
                Pair(now - (3600_000L), 3.20)
            )

            for ((time, dose) in sampleDays) {
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
