package com.doseguard.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Band entity — represents a single H2S passive dosimeter wristband.
 * workerId links to WorkerEntity after assignment.
 * currentEstimatedDose accumulates over multiple scans.
 */
@Entity(tableName = "bands")
data class BandEntity(
    @PrimaryKey val bandId: String,           // From QR code, e.g. "BAND-001285" / "WB-1001"
    val workerId: String = "",                // Empty until assigned
    val batchNo: String = "BATCH-2026-A1",    // Manufacturer batch/lot number
    val qrData: String,                       // Raw QR payload string
    val issueDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + 30L * 24 * 3600 * 1000, // 30 days
    val bandStatus: String = "AVAILABLE",     // "AVAILABLE" | "ACTIVE" | "ASSIGNED" | "EXPIRED" | "SATURATED" | "RELEASED"
    val maximumDose: Double = 50.0,           // ppm·hr ceiling before mandatory replacement
    val currentEstimatedDose: Double = 0.0,   // Cumulative ppm·hr from all scans
    val lastScanTime: Long = 0L
)
