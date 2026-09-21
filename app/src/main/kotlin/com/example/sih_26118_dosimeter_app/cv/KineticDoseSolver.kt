package com.example.sih_26118_dosimeter_app.cv

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Kinetic Exposure Saturation Solver & Error Propagation Engine
 *
 * Maps optical Delta-E color differences into cumulative dosage D (ppm*hr):
 * D = - (1 / k) * ln(1 - delta_e / delta_e_max)
 */
object KineticDoseSolver {

    data class DoseResult(
        val dosePpmHr: Double,
        val uncertaintyPpmHr: Double,
        val twa8hrPpm: Double,
        val riskLevel: String,
        val actionRequired: String
    )

    fun calculateDose(
        deltaE: Double,
        shiftHours: Double = 8.0,
        deltaEMax: Double = 75.0,
        kRate: Double = 0.025,
        sigmaDeltaE: Double = 1.2
    ): DoseResult {
        val safeDeltaE = min(max(0.0, deltaE), deltaEMax - 0.5)
        val ratio = safeDeltaE / deltaEMax
        val dosePpmHr = -(1.0 / kRate) * ln(1.0 - ratio)

        val denominator = kRate * (deltaEMax - safeDeltaE)
        val uncertainty = if (denominator > 0) sigmaDeltaE / denominator else 5.0

        val effectiveShift = max(0.1, shiftHours)
        val twa8hrPpm = dosePpmHr / effectiveShift

        val (riskLevel, action) = when {
            twa8hrPpm < 1.0 -> "SAFE" to "Normal operation. Exposure within permissible limits."
            twa8hrPpm < 2.5 -> "MODERATE" to "Action level reached. Inspect seals and re-check badge in 2 hours."
            twa8hrPpm < 10.0 -> "HIGH" to "Approaching Permissible Exposure Limit (PEL). Rotate worker to fresh air."
            else -> "CRITICAL" to "PERMISSIBLE EXPOSURE LIMIT EXCEEDED (> 10 ppm TWA). Evacuate worker immediately."
        }

        return DoseResult(
            dosePpmHr = Math.round(dosePpmHr * 100.0) / 100.0,
            uncertaintyPpmHr = Math.round(uncertainty * 100.0) / 100.0,
            twa8hrPpm = Math.round(twa8hrPpm * 100.0) / 100.0,
            riskLevel = riskLevel,
            actionRequired = action
        )
    }
}
