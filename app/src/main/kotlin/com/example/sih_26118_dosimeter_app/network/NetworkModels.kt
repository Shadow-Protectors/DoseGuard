package com.example.sih_26118_dosimeter_app.network

import com.google.gson.annotations.SerializedName

// Cloud Sync Request & Response
data class SyncLogsRequest(
    @SerializedName("logs") val logs: List<ShiftLogDto>
)

data class SyncLogsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("syncedCount") val syncedCount: Int?,
    @SerializedName("timestamp") val timestamp: String?
)

data class ShiftLogDto(
    @SerializedName("logId") val logId: String,
    @SerializedName("workerId") val workerId: String,
    @SerializedName("workerName") val workerName: String,
    @SerializedName("department") val department: String,
    @SerializedName("bandId") val bandId: String,
    @SerializedName("shiftDate") val shiftDate: String,
    @SerializedName("startTime") val startTime: String = "08:00:00",
    @SerializedName("scanTime") val scanTime: String,
    @SerializedName("durationHours") val durationHours: Double,
    @SerializedName("expiryStatus") val expiryStatus: String,
    @SerializedName("rawDeltaE") val rawDeltaE: Double,
    @SerializedName("estimatedDosePpmHr") val estimatedDosePpmHr: Double,
    @SerializedName("uncertaintyPpmHr") val uncertaintyPpmHr: Double,
    @SerializedName("twa8hrPpm") val twa8hrPpm: Double,
    @SerializedName("riskLevel") val riskLevel: String,
    @SerializedName("actionRequired") val actionRequired: String,
    @SerializedName("imageHash") val imageHash: String,
    @SerializedName("syncStatus") val syncStatus: String = "SYNCED"
)

// Logs Query Response
data class GetLogsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("data") val data: List<ShiftLogDto>
)

// Dashboard Metrics Response
data class DashboardMetricsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("metrics") val metrics: DashboardMetricsDto?
)

data class DashboardMetricsDto(
    @SerializedName("totalScans") val totalScans: Int,
    @SerializedName("safeCount") val safeCount: Int,
    @SerializedName("cautionCount") val cautionCount: Int,
    @SerializedName("highCount") val highCount: Int,
    @SerializedName("criticalCount") val criticalCount: Int,
    @SerializedName("totalDosePpmHr") val totalDosePpmHr: Double,
    @SerializedName("avgTwaPpm") val avgTwaPpm: Double,
    @SerializedName("complianceRatePct") val complianceRatePct: Double
)

// Worker Registration Request & Response
data class RegisterWorkerRequest(
    @SerializedName("name") val name: String,
    @SerializedName("workerId") val workerId: String,
    @SerializedName("department") val department: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String = "Worker@123",
    @SerializedName("role") val role: String = "WORKER"
)

data class AuthResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("token") val token: String?
)
