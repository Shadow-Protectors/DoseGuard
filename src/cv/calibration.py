"""
SIH 26118 - Chromatic Adaptation & Optical Delta-E Calibration Engine

Performs von Kries chromatic adaptation using the 18% neutral gray reference swatch,
converts normalized images to CIELAB (L*a*b*) color space, and computes Delta-E values
for both the active H2S Bismuth sensor strip and the Silver Nanoparticle expiry patch.
"""

import cv2
import numpy as np
from typing import Tuple, Dict, Any


def extract_median_roi_color(image: np.ndarray, bbox: Tuple[int, int, int, int], crop_margin_pct: float = 0.15) -> np.ndarray:
    """
    Extracts the median RGB color vector within an ROI bounding box.
    Applies an inset crop margin to eliminate edge diffusion or shadow artifacts.

    Parameters:
    -----------
    image : np.ndarray
        Orthorectified image (600x400x3).
    bbox : Tuple[int, int, int, int]
        Bounding box (x1, y1, x2, y2).
    crop_margin_pct : float
        Percentage margin to crop inward from borders (default 0.15 for central 70%).

    Returns:
    --------
    np.ndarray
        Median RGB color vector [R, G, B].
    """
    x1, y1, x2, y2 = bbox
    w = x2 - x1
    h = y2 - y1
    
    mx = int(w * crop_margin_pct)
    my = int(h * crop_margin_pct)

    roi = image[y1 + my : y2 - my, x1 + mx : x2 - mx]
    if roi.size == 0:
        roi = image[y1:y2, x1:x2]

    # Reshape to (N, 3) and calculate median across pixels for robust outlier rejection
    pixels = roi.reshape(-1, 3)
    median_color = np.median(pixels, axis=0)
    return median_color


def apply_von_kries_chromatic_adaptation(
    image: np.ndarray,
    gray_swatch_bbox: Tuple[int, int, int, int] = (110, 75, 230, 135),
    target_gray_val: float = 119.0
) -> Tuple[np.ndarray, np.ndarray]:
    """
    Applies von Kries diagonal scaling chromatic adaptation to remove ambient lighting casts.

    Calculation:
    R_norm = (target_gray / measured_gray_R) * R
    G_norm = (target_gray / measured_gray_G) * G
    B_norm = (target_gray / measured_gray_B) * B

    Parameters:
    -----------
    image : np.ndarray
        Orthorectified RGB image (600x400x3).
    gray_swatch_bbox : Tuple[int, int, int, int]
        Bounding box for the 18% neutral gray swatch.
    target_gray_val : float
        Target neutral sRGB luminance value (default 119.0).

    Returns:
    --------
    Tuple[np.ndarray, np.ndarray]
        - Chromatically adapted RGB image clipped to [0, 255].
        - Measured gray RGB color vector before correction.
    """
    measured_gray = extract_median_roi_color(image, gray_swatch_bbox)
    
    # Avoid divide-by-zero if sensor is completely black
    safe_gray = np.maximum(measured_gray, 1.0)
    scale_factors = target_gray_val / safe_gray

    # Apply diagonal gain scaling per color channel
    adapted_img = image.astype(np.float32) * scale_factors
    adapted_img = np.clip(adapted_img, 0, 255).astype(np.uint8)

    return adapted_img, measured_gray


def rgb_to_cielab(rgb_color: np.ndarray) -> np.ndarray:
    """
    Converts a single sRGB color vector [R, G, B] to CIELAB [L*, a*, b*].

    Parameters:
    -----------
    rgb_color : np.ndarray
        RGB vector in range [0, 255].

    Returns:
    --------
    np.ndarray
        CIELAB vector [L*, a*, b*].
    """
    rgb_pixel = np.uint8([[rgb_color]])
    lab_pixel = cv2.cvtColor(rgb_pixel, cv2.COLOR_RGB2LAB)
    
    # OpenCV LAB ranges: L in [0, 255], a in [0, 255], b in [0, 255]
    # Standard Lab: L* in [0, 100], a* in [-128, 127], b* in [-128, 127]
    lab_raw = lab_pixel[0, 0].astype(np.float32)
    L_star = lab_raw[0] * (100.0 / 255.0)
    a_star = lab_raw[1] - 128.0
    b_star = lab_raw[2] - 128.0

    return np.array([L_star, a_star, b_star], dtype=np.float32)


def compute_delta_e_ab(lab1: np.ndarray, lab2: np.ndarray) -> float:
    """
    Computes Euclidean Delta-E (CIE 1976) between two CIELAB color vectors.

    Delta-E = sqrt((L1* - L2*)^2 + (a1* - a2*)^2 + (b1* - b2*)^2)

    Complexity:
    -----------
    Time Complexity: O(1)
    Space Complexity: O(1)
    """
    diff = lab1 - lab2
    delta_e = float(np.sqrt(np.sum(diff ** 2)))
    return delta_e


def analyze_badge_optical_density(
    ortho_image: np.ndarray,
    baseline_unexposed_lab: np.ndarray = np.array([96.0, 0.0, 2.0], dtype=np.float32),
    expiry_threshold_delta_e: float = 18.0
) -> Dict[str, Any]:
    """
    Analyzes the orthorectified badge image to extract chromatic metrics:
    1. Performs von Kries chromatic adaptation using neutral gray.
    2. Extracts active H2S sensor well median color and converts to CIELAB.
    3. Calculates Delta-E against unexposed baseline.
    4. Evaluates expiry patch state.

    Bboxes (600x400 canonical layout):
    - Gray Swatch: (110, 75, 230, 135)
    - White Swatch: (250, 75, 370, 135)
    - Active Sensor Well: (110, 170, 370, 310)
    - Expiry Patch: (390, 170, 520, 310)

    Returns:
    --------
    Dict[str, Any]
        Structured dictionary containing Delta-E, LAB vectors, expiry status, and diagnostic flags.
    """
    # 1. Chromatic Adaptation
    adapted_img, measured_gray = apply_von_kries_chromatic_adaptation(ortho_image)

    # 2. Extract Active Sensor Well Color
    sensor_bbox = (110, 170, 370, 310)
    sensor_rgb = extract_median_roi_color(adapted_img, sensor_bbox, crop_margin_pct=0.15)
    sensor_lab = rgb_to_cielab(sensor_rgb)

    # 3. Calculate Delta-E for Active Sensor
    sensor_delta_e = compute_delta_e_ab(sensor_lab, baseline_unexposed_lab)

    # 4. Extract Expiry Patch Color
    expiry_bbox = (390, 170, 520, 310)
    expiry_rgb = extract_median_roi_color(adapted_img, expiry_bbox, crop_margin_pct=0.15)
    expiry_lab = rgb_to_cielab(expiry_rgb)
    
    # Baseline unexposed expiry patch (pristine light blue/gray)
    pristine_expiry_lab = np.array([90.0, -10.0, -5.0], dtype=np.float32)
    expiry_delta_e = compute_delta_e_ab(expiry_lab, pristine_expiry_lab)
    
    is_expired = expiry_delta_e > expiry_threshold_delta_e

    return {
        "sensor_rgb": sensor_rgb.tolist(),
        "sensor_lab": sensor_lab.tolist(),
        "sensor_delta_e": round(sensor_delta_e, 3),
        "expiry_rgb": expiry_rgb.tolist(),
        "expiry_lab": expiry_lab.tolist(),
        "expiry_delta_e": round(expiry_delta_e, 3),
        "is_expired": is_expired,
        "expiry_status": "EXPIRED" if is_expired else "VALID",
        "measured_gray_rgb": measured_gray.tolist()
    }
