package com.doseguard.app

import com.doseguard.app.imageprocessing.KineticColorEstimator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the Kinetic Colorimetry and H2S Exposure Estimation model.
 */
class KineticColorEstimatorTest {

    private lateinit var estimator: KineticColorEstimator

    @Before
    fun setUp() {
        estimator = KineticColorEstimator()
    }

    @Test
    fun testBaselineSafeExposure() {
        // Delta E of 2.0 represents baseline unexposed / negligible exposure
        val result = estimator.estimateFromDeltaE(2.0, 8.0)
        assertEquals("Baseline strip should be classified as SAFE", "SAFE", result.riskLevel)
        assertTrue("8-hr TWA should be <= 1.0 ppm for safe baseline", result.twa8hrPpm <= 1.0)
        assertTrue("Estimated dose should be positive and small", result.estimatedDosePpmHr >= 0.0)
    }

    @Test
    fun testModerateExposure() {
        // Delta E of 25.0 corresponds to ~14.9 ppm*hr (TWA ~1.86 ppm), which is within Action Level (1.0-5.0 ppm)
        val result = estimator.estimateFromDeltaE(25.0, 8.0)
        assertEquals("Delta E of 25.0 should be MODERATE", "MODERATE", result.riskLevel)
        assertTrue("TWA should be between 1.0 and 5.0 ppm", result.twa8hrPpm in 1.0..5.0)
    }

    @Test
    fun testHighExposure() {
        // Delta E of 53.0 corresponds to ~47.5 ppm*hr (TWA ~5.94 ppm), approaching OSHA PEL (5.0-10.0 ppm)
        val result = estimator.estimateFromDeltaE(53.0, 8.0)
        assertEquals("Delta E of 53.0 should be HIGH", "HIGH", result.riskLevel)
        assertTrue("TWA should be between 5.0 and 10.0 ppm", result.twa8hrPpm in 5.0..10.0)
    }

    @Test
    fun testCriticalExposure() {
        // Delta E of 66.0 corresponds to ~88.8 ppm*hr (TWA ~11.1 ppm), exceeding statutory ceiling (>10.0 ppm)
        val result = estimator.estimateFromDeltaE(66.0, 8.0)
        assertEquals("Delta E of 66.0 should be CRITICAL", "CRITICAL", result.riskLevel)
        assertTrue("TWA should exceed 10.0 ppm", result.twa8hrPpm > 10.0)
    }

    @Test
    fun testConfidenceDecreasesNearSaturation() {
        val lowExposure = estimator.estimateFromDeltaE(5.0, 8.0)
        val saturatedExposure = estimator.estimateFromDeltaE(68.0, 8.0)
        assertTrue("Confidence should be high for clear linear regime", lowExposure.confidence >= 0.85)
        assertTrue("Confidence should decrease near saturation limit", saturatedExposure.confidence < lowExposure.confidence)
    }
}
