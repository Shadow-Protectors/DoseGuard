"""
SIH 26118 - Comprehensive Automated Test Suite

Tests hardware calibration card generation, homography perspective rectification,
von Kries chromatic adaptation, kinetic dose mapping, SQLite database transactions,
cloud sync triggers, and DGMS Form IV PDF exporter.
"""

import pytest
import numpy as np
import cv2
import os
from src.hardware.calibration_card import generate_calibration_card
from src.cv.homography import detect_aruco_corners, rectify_wristband_image
from src.cv.calibration import apply_von_kries_chromatic_adaptation, analyze_badge_optical_density, compute_delta_e_ab
from src.cv.kinetics import delta_e_to_cumulative_dose, evaluate_shift_exposure
from src.db.database import DosimeterDatabase
from src.reports.dgms_exporter import generate_dgms_form_iv_pdf


def test_calibration_card_generation():
    """Validates printable calibration card canvas dimensions and ArUco markers."""
    card = generate_calibration_card(600, 400)
    assert card.shape == (400, 600, 3)
    
    # Test ArUco marker detection on generated target
    detected_pts, marker_map = detect_aruco_corners(card)
    assert detected_pts is not None
    assert len(marker_map) == 4
    for mid in [0, 1, 2, 3]:
        assert mid in marker_map


def test_homography_perspective_rectification():
    """Validates planar homography warping on synthetic camera scan."""
    canonical_card = generate_calibration_card(600, 400)
    
    # Synthetic perspective transformation (simulate 20-degree camera tilt)
    src_pts = np.float32([[55, 55], [545, 55], [545, 345], [55, 345]])
    tilt_pts = np.float32([[80, 70], [520, 45], [550, 370], [60, 360]])
    
    H_tilt, _ = cv2.findHomography(src_pts, tilt_pts)
    tilted_img = cv2.warpPerspective(canonical_card, H_tilt, (700, 500))

    # Apply homography rectification
    rectified, success, msg = rectify_wristband_image(tilted_img, 600, 400)
    assert success is True
    assert rectified.shape == (400, 600, 3)


def test_chromatic_adaptation_and_delta_e():
    """Validates von Kries lighting normalization and CIELAB Delta-E calculation."""
    card = generate_calibration_card(600, 400)
    
    # Simulate a warm sodium-vapor color cast (elevate R & G, reduce B)
    warmed_card = card.astype(np.float32)
    warmed_card[:, :, 0] = np.clip(warmed_card[:, :, 0] * 1.25, 0, 255) # R channel
    warmed_card[:, :, 2] = np.clip(warmed_card[:, :, 2] * 0.70, 0, 255) # B channel
    warmed_card = warmed_card.astype(np.uint8)

    results = analyze_badge_optical_density(warmed_card)
    assert "sensor_delta_e" in results
    assert "expiry_status" in results
    assert results["expiry_status"] in ["VALID", "EXPIRED"]


def test_kinetic_dose_mapping():
    """Validates pseudo-first-order kinetic saturation and hazard action levels."""
    # Test low exposure
    dose_low, sigma_low = delta_e_to_cumulative_dose(10.0)
    eval_low = evaluate_shift_exposure(10.0, shift_hours=8.0)
    assert eval_low["risk_level"] == "SAFE"
    assert eval_low["dose_ppm_hr"] > 0.0

    # Test high exposure (nearing PEL)
    eval_high = evaluate_shift_exposure(45.0, shift_hours=8.0)
    assert eval_high["risk_level"] in ["HIGH", "CRITICAL"]


def test_database_and_cloud_sync(tmp_path):
    """Validates SQLite transactions, SHA-256 image hashing, and cloud sync."""
    db_file = tmp_path / "test_dosimeter.db"
    db = DosimeterDatabase(str(db_file))

    workers = db.get_all_workers()
    assert len(workers) >= 4

    # Log a dummy shift scan
    sample_bytes = b"SIMULATED_BADGE_IMAGE_BYTES_12345"
    log_res = db.log_shift_scan(
        log_id="LOG-TEST-001",
        worker_id=workers[0]["worker_id"],
        band_id="BAND-9988",
        shift_date="2026-09-10",
        start_time="08:00:00",
        scan_time="16:00:00",
        duration_hours=8.0,
        expiry_status="VALID",
        raw_delta_e=24.5,
        dose_ppm_hr=12.4,
        uncertainty=0.8,
        twa_8hr_ppm=1.55,
        risk_level="CAUTION",
        action_required="Inspect seals",
        image_bytes=sample_bytes
    )

    assert log_res["sync_status"] == "PENDING"
    assert len(log_res["image_hash"]) == 64  # SHA-256 length

    # Execute Cloud Sync
    sync_res = db.sync_to_cloud()
    assert sync_res["synced_count"] >= 1
    assert sync_res["status"] == "SUCCESS"


def test_dgms_pdf_generation():
    """Validates statutory DGMS Form IV PDF document creation."""
    dummy_log = {
        "log_id": "LOG-TEST-001",
        "band_id": "BAND-9988",
        "shift_date": "2026-09-10",
        "scan_time": "16:00:00",
        "duration_hours": 8.0,
        "expiry_status": "VALID",
        "raw_delta_e": 24.5,
        "estimated_dose_ppm_hr": 12.4,
        "uncertainty_ppm_hr": 0.8,
        "twa_8hr_ppm": 1.55,
        "risk_level": "CAUTION",
        "action_required": "Inspect seals",
        "image_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        "sync_status": "SYNCED"
    }
    dummy_worker = {
        "worker_id": "W-1001",
        "name": "Rajesh Kumar",
        "department": "Refinery Sweetening Unit",
        "role": "Plant Operator"
    }

    pdf_bytes = generate_dgms_form_iv_pdf(dummy_log, dummy_worker)
    assert len(pdf_bytes) > 500
    assert pdf_bytes.startswith(b"%PDF")
