package com.doseguard.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AlertEntity — created when a scan result exceeds the exposure threshold.
 * status transitions: "ACTIVE" → "ACKNOWLEDGED" → "RESOLVED"
 */
@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val alertId: String,         // UUID
    val workerId: String,
    val bandId: String,
    val triggerDose: Double,                 // The ppm·hr value that triggered the alert
    val riskLevel: String,                   // "HIGH" | "CRITICAL"
    val status: String = "ACTIVE",           // "ACTIVE" | "ACKNOWLEDGED" | "RESOLVED"
    val createdAt: Long = System.currentTimeMillis()
)
