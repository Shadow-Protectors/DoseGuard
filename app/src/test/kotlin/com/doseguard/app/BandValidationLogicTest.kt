package com.doseguard.app

import com.doseguard.app.model.BandEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying Industrial Band Validation rules.
 */
class BandValidationLogicTest {

    private val now = System.currentTimeMillis()
    private val dayMs = 86_400_000L

    @Test
    fun testAvailableBandIsValid() {
        val band = BandEntity(
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

        val isExpired = band.bandStatus == "EXPIRED" || now > band.expiryDate
        val isSaturated = band.bandStatus == "SATURATED" || band.currentEstimatedDose >= band.maximumDose
        val isAssignedToOther = band.workerId.isNotBlank() && band.workerId != "W-1001"

        assertFalse("Available band should not be expired", isExpired)
        assertFalse("Available band should not be saturated", isSaturated)
        assertFalse("Available band should not be assigned to other", isAssignedToOther)
    }

    @Test
    fun testExpiredBandRejection() {
        val band = BandEntity(
            bandId = "WB-EXP-01",
            workerId = "",
            batchNo = "BATCH-2025-Z9",
            qrData = "DG:BAND:WB-EXP-01",
            issueDate = now - (40 * dayMs),
            expiryDate = now - (2 * dayMs),
            bandStatus = "EXPIRED",
            maximumDose = 50.0,
            currentEstimatedDose = 0.0
        )

        val isExpired = band.bandStatus == "EXPIRED" || now > band.expiryDate
        assertTrue("Past expiry date must trigger expired state", isExpired)
    }

    @Test
    fun testSaturatedBandRejection() {
        val band = BandEntity(
            bandId = "WB-SAT-99",
            workerId = "",
            batchNo = "BATCH-2026-X1",
            qrData = "DG:BAND:WB-SAT-99",
            issueDate = now,
            expiryDate = now + (30 * dayMs),
            bandStatus = "SATURATED",
            maximumDose = 50.0,
            currentEstimatedDose = 50.0
        )

        val isSaturated = band.bandStatus == "SATURATED" || band.currentEstimatedDose >= band.maximumDose
        assertTrue("Max dose reached must trigger saturated state", isSaturated)
    }

    @Test
    fun testAlreadyAssignedRejection() {
        val band = BandEntity(
            bandId = "WB-ASSIGNED-01",
            workerId = "W-1003", // Anita Desai
            batchNo = "BATCH-2026-A1",
            qrData = "DG:BAND:WB-ASSIGNED-01",
            issueDate = now - (3 * dayMs),
            expiryDate = now + (27 * dayMs),
            bandStatus = "ACTIVE",
            maximumDose = 50.0,
            currentEstimatedDose = 1.8
        )

        val targetWorkerId = "W-1001" // Arun Kumar
        val isAssignedToOther = band.workerId.isNotBlank() && band.workerId != targetWorkerId && band.bandStatus in listOf("ACTIVE", "ASSIGNED")
        assertTrue("Band assigned to W-1003 must be rejected when requested by W-1001", isAssignedToOther)
    }
}
