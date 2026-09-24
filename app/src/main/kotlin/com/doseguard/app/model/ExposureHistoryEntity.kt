package com.doseguard.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ExposureHistoryEntity — one record per scan event.
 * imagePath stores the local file URI of the captured strip image.
 * confidence is a 0–1 value from the exposure estimator.
 */
@Entity(tableName = "exposure_history")
data class ExposureHistoryEntity(
    @PrimaryKey val historyId: String,       // UUID
    val workerId: String,
    val bandId: String,
    val scanTime: Long = System.currentTimeMillis(),
    val estimatedDose: Double,               // ppm·hr for this scan session
    val confidence: Double,                  // 0.0 – 1.0
    val imagePath: String = "",              // Local file path of captured image
    val riskLevel: String,                   // "SAFE" | "MODERATE" | "HIGH" | "CRITICAL"
    val temperature: Double = 25.0,          // °C — from sensor or default
    val humidity: Double = 60.0              // % RH — from sensor or default
)
