package com.doseguard.app.imageprocessing

/**
 * ExposureEstimator — pluggable interface for H2S strip color analysis.
 *
 * The current implementation (KineticColorEstimator) uses rule-based LAB/brightness math.
 * To integrate a TFLite model:
 *   1. Create a class TFLiteEstimator : ExposureEstimator
 *   2. Load the .tflite asset in init{}
 *   3. Override estimate() to run inference
 *   4. Inject TFLiteEstimator instead of KineticColorEstimator in DoseGuardRepository
 *
 * [AI_INTEGRATION_POINT] — swap implementation here when real model is ready.
 */
interface ExposureEstimator {

    /**
     * Analyze a captured strip image and estimate H2S exposure.
     *
     * @param imageBytes Raw JPEG bytes of the cropped strip region.
     * @param shiftHours Hours elapsed since band was last fresh (used for TWA calc).
     * @return [EstimationResult] with dose, confidence, and risk classification.
     */
    fun estimate(imageBytes: ByteArray, shiftHours: Double = 8.0): EstimationResult

    data class EstimationResult(
        val estimatedDosePpmHr: Double,   // Cumulative dose for this scan (ppm·hr)
        val twa8hrPpm: Double,            // Time-weighted average over 8h (ppm)
        val uncertaintyPpmHr: Double,     // ±uncertainty from model/formula
        val riskLevel: String,            // "SAFE" | "MODERATE" | "HIGH" | "CRITICAL"
        val actionRequired: String,       // Human-readable guidance text
        val confidence: Double,           // 0.0–1.0 model confidence
        val rawDeltaE: Double,            // ΔE color difference used for estimation
        val debugInfo: String = ""        // Diagnostic string for demo display
    )
}
