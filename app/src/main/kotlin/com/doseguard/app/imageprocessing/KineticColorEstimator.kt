package com.doseguard.app.imageprocessing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlin.math.*

/**
 * KineticColorEstimator — rule-based H2S exposure estimator.
 *
 * Calibrated Colorimetry & Physical Chemistry:
 * 1. Decode JPEG bytes → Bitmap
 * 2. Sample average RGB from the center strip region (middle 40% of image)
 * 3. Convert sRGB → CIE LAB (using D65 standard illuminant)
 * 4. Compute chemical darkening & metal sulfide precipitation index (ΔE_eff):
 *    - Unexposed strip is naturally pale cream / light yellow (L* ≈ 92, a* ≈ -1, b* ≈ 12).
 *    - Exposure to H2S gas causes formation of dark brown-black metal sulfide (PbS / Ag2S).
 *    - The primary optical indicator is DARKENING (ΔL loss). Slight yellow/cream tint (high b*)
 *      is the baseline unexposed chemical carrier and indicates SAFE exposure (≤1.0 ppm TWA).
 * 5. Apply first-order kinetic saturation model: D = -(1/k) * ln(1 - ΔE/ΔE_max)
 * 6. Classify risk against statutory OSHA / DGMS permissible exposure limits (PEL):
 *    - Safe: ≤1.0 ppm 8-hr TWA (DGMS safe baseline)
 *    - Moderate: 1.0–5.0 ppm 8-hr TWA (Action level)
 *    - High: 5.0–10.0 ppm 8-hr TWA (Approaching OSHA PEL ceiling)
 *    - Critical: >10.0 ppm 8-hr TWA (Statutory limit exceeded; Evacuation SOP)
 *
 * [AI_INTEGRATION_POINT] — replace this class with TFLiteEstimator
 *   when CNN model is trained on calibrated strip images.
 */
class KineticColorEstimator : ExposureEstimator {

    // Pristine H2S lead acetate strip reference in LAB (D65 illuminant)
    private val REF_L = 92.0
    private val REF_A = -1.0
    private val REF_B = 12.0

    // Kinetic model constants (calibrated from bench experimental darkening curves)
    private val DELTA_E_MAX = 72.0
    private val K_RATE = 0.028          // Reaction rate constant
    private val SIGMA_DELTA_E = 1.2     // Instrument noise std-dev

    override fun estimate(imageBytes: ByteArray, shiftHours: Double): ExposureEstimator.EstimationResult {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return fallbackResult("Bitmap decode failed")

        // Sample average color from center strip region (40% horizontal, 30–70% vertical)
        val avgRgb = sampleCenterRegion(bitmap)
        val (sL, sA, sB) = rgbToLab(avgRgb.first, avgRgb.second, avgRgb.third)

        // Physical Sulfide Precipitation & Darkening Index:
        // Loss of luminance (darkening is the primary signal of metal sulfide precipitate)
        val deltaL = max(0.0, REF_L - sL)
        // Red-brown chromophore shift (a* increase towards reddish-brown)
        val deltaA = max(0.0, sA - REF_A)
        // Only loss of yellow (b* dropping below baseline as it turns gray/black) indicates sulfide.
        // Elevated b* (pure yellow) represents clean carrier dye, NOT sulfide darkening.
        val deltaB = if (sB < REF_B) (REF_B - sB) else 0.0

        val effectiveDeltaE = sqrt(deltaL.pow(2) * 1.3 + deltaA.pow(2) * 1.0 + deltaB.pow(2) * 0.4)

        return computeFromDeltaE(effectiveDeltaE, shiftHours)
    }

    /**
     * Direct entry point for simulated/demo scans — takes a deltaE directly.
     * Used by the demo preset chips on the scan screen.
     */
    fun estimateFromDeltaE(deltaE: Double, shiftHours: Double = 8.0): ExposureEstimator.EstimationResult {
        return computeFromDeltaE(deltaE, shiftHours)
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private fun computeFromDeltaE(rawDeltaE: Double, shiftHours: Double): ExposureEstimator.EstimationResult {
        val safeDeltaE = rawDeltaE.coerceIn(0.0, DELTA_E_MAX - 0.5)
        val ratio = safeDeltaE / DELTA_E_MAX

        // First-order kinetic saturation formula: D = -(1/k) * ln(1 - ratio)
        val dosePpmHr = -(1.0 / K_RATE) * ln(1.0 - ratio)

        // Gaussian error propagation
        val denom = K_RATE * (DELTA_E_MAX - safeDeltaE)
        val uncertainty = if (denom > 0) SIGMA_DELTA_E / denom else 4.0

        val effectiveShift = shiftHours.coerceAtLeast(0.1)
        val twa8hr = dosePpmHr / effectiveShift

        // Confidence inversely proportional to uncertainty (clamped 0.5–0.99)
        val confidence = (1.0 - (uncertainty / 25.0)).coerceIn(0.5, 0.99)

        val (risk, action) = classifyRisk(twa8hr)

        val debug = "ΔE=%.2f | dose=%.2f ppm·hr | TWA=%.2f ppm | ±%.2f".format(
            safeDeltaE, dosePpmHr.round2(), twa8hr.round2(), uncertainty.round2()
        )

        return ExposureEstimator.EstimationResult(
            estimatedDosePpmHr = dosePpmHr.round2(),
            twa8hrPpm = twa8hr.round2(),
            uncertaintyPpmHr = uncertainty.round2(),
            riskLevel = risk,
            actionRequired = action,
            confidence = confidence.round2(),
            rawDeltaE = rawDeltaE.round2(),
            debugInfo = debug
        )
    }

    /** Sample average RGB from center 40% width × 40% height crop. */
    private fun sampleCenterRegion(bitmap: Bitmap): Triple<Int, Int, Int> {
        val w = bitmap.width
        val h = bitmap.height
        val xStart = (w * 0.30).toInt()
        val xEnd = (w * 0.70).toInt()
        val yStart = (h * 0.30).toInt()
        val yEnd = (h * 0.70).toInt()

        var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0
        // Sub-sample every 4 pixels to avoid ANR on large bitmaps
        val step = 4
        for (x in xStart until xEnd step step) {
            for (y in yStart until yEnd step step) {
                val px = bitmap.getPixel(x, y)
                rSum += Color.red(px)
                gSum += Color.green(px)
                bSum += Color.blue(px)
                count++
            }
        }
        if (count == 0) return Triple(239, 235, 233)  // pristine cream default
        return Triple((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
    }

    /** sRGB [0,255] → CIE LAB D65 (two-step: sRGB → XYZ → LAB). */
    private fun rgbToLab(r: Int, g: Int, b: Int): Triple<Double, Double, Double> {
        fun linearize(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        val lr = linearize(r); val lg = linearize(g); val lb = linearize(b)

        // sRGB → XYZ (D65 standard illuminant)
        val x = lr * 0.4124564 + lg * 0.3575761 + lb * 0.1804375
        val y = lr * 0.2126729 + lg * 0.7151522 + lb * 0.0721750
        val z = lr * 0.0193339 + lg * 0.1191920 + lb * 0.9503041

        // XYZ → LAB
        fun f(t: Double): Double {
            val delta = 6.0 / 29.0
            return if (t > delta.pow(3)) t.pow(1.0 / 3.0) else t / (3 * delta.pow(2)) + 4.0 / 29.0
        }
        val fx = f(x / 0.95047)
        val fy = f(y / 1.00000)
        val fz = f(z / 1.08883)

        val L = 116 * fy - 16
        val A = 500 * (fx - fy)
        val B = 200 * (fy - fz)
        return Triple(L, A, B)
    }

    /**
     * Statutory Real-Time Exposure Limits:
     * - SAFE: ≤1.0 ppm 8-hr TWA (DGMS safe baseline standard)
     * - MODERATE: 1.0–5.0 ppm 8-hr TWA (Action Level)
     * - HIGH: 5.0–10.0 ppm 8-hr TWA (Approaching OSHA 8-hr PEL)
     * - CRITICAL: >10.0 ppm 8-hr TWA (OSHA/DGMS PEL Exceeded; Evacuate)
     */
    private fun classifyRisk(twa8hr: Double): Pair<String, String> = when {
        twa8hr <= 1.0  -> "SAFE"     to "Normal operation. Exposure within permissible baseline (≤1.0 ppm 8-hr TWA)."
        twa8hr <= 5.0  -> "MODERATE" to "Action Level reached (1.0–5.0 ppm TWA). Inspect seals and monitor worker."
        twa8hr <= 10.0 -> "HIGH"     to "Approaching OSHA/DGMS statutory limit (5.0–10.0 ppm TWA). Rotate worker to fresh air zone."
        else          -> "CRITICAL" to "OSHA/DGMS PEL EXCEEDED (>10.0 ppm TWA). EVACUATE WORKER IMMEDIATELY. Initiate medical triage SOP."
    }

    private fun fallbackResult(reason: String) = ExposureEstimator.EstimationResult(
        estimatedDosePpmHr = 0.0, twa8hrPpm = 0.0, uncertaintyPpmHr = 0.0,
        riskLevel = "UNKNOWN", actionRequired = "Image analysis failed: $reason",
        confidence = 0.0, rawDeltaE = 0.0
    )

    private fun Double.round2() = (this * 100.0).toLong() / 100.0
}
