"""
SIH 26118 - Passive Colorimetric H2S Dosimeter Wristband Platform
Core Hardware Calibration Card Generator
"""

import numpy as np
import cv2
from PIL import Image, ImageDraw, ImageFont


def generate_calibration_card(
    width_px: int = 600,
    height_px: int = 400,
    aruco_dict_type: int = cv2.aruco.DICT_4X4_50
) -> np.ndarray:
    """
    Generates a canonical orthorectified printable calibration target image.

    The card contains:
    1. Four ArUco markers at the corners for planar homography rectification.
    2. 100% White reference swatch (L*max) for luminance baseline.
    3. 18% Neutral Gray reference swatch for von Kries chromatic adaptation.
    4. Active chemical substrate well (simulated pristine white Bismuth paper).
    5. Micro-sealed expiry patch well (simulated pristine Silver Nanoparticle patch).
    6. Anchor color reference swatches for sensor normalization.

    Parameters:
    -----------
    width_px : int
        Width of the target canonical card in pixels (default 600).
    height_px : int
        Height of the target canonical card in pixels (default 400).
    aruco_dict_type : int
        OpenCV ArUco dictionary identifier.

    Returns:
    --------
    np.ndarray
        RGB image of the generated calibration card (height_px, width_px, 3).

    Complexity:
    -----------
    Time Complexity: O(W * H) where W, H are target image dimensions.
    Space Complexity: O(W * H * 3) for the image buffer.
    """
    # Create white canvas background (RGB)
    canvas = np.ones((height_px, width_px, 3), dtype=np.uint8) * 245

    # Outer border line
    cv2.rectangle(canvas, (10, 10), (width_px - 10, height_px - 10), (50, 50, 50), 2)

    # Initialize ArUco dictionary and generator
    aruco_dict = cv2.aruco.getPredefinedDictionary(aruco_dict_type)
    marker_size = 70  # pixels

    # Define 4 corner marker positions (Top-Left, Top-Right, Bottom-Right, Bottom-Left)
    marker_coords = [
        (20, 20),                                    # ID 0: Top-Left
        (width_px - 20 - marker_size, 20),           # ID 1: Top-Right
        (width_px - 20 - marker_size, height_px - 20 - marker_size), # ID 2: Bottom-Right
        (20, height_px - 20 - marker_size)            # ID 3: Bottom-Left
    ]

    for marker_id, (x, y) in enumerate(marker_coords):
        marker_img = cv2.aruco.generateImageMarker(aruco_dict, marker_id, marker_size)
        # Convert 1-channel to 3-channel RGB
        marker_rgb = cv2.cvtColor(marker_img, cv2.COLOR_GRAY2RGB)
        canvas[y:y+marker_size, x:x+marker_size] = marker_rgb

    # Draw Title Header
    cv2.putText(
        canvas, "SIH 26118 H2S DOSIMETER", (110, 45),
        cv2.FONT_HERSHEY_SIMPLEX, 0.65, (20, 20, 20), 2, cv2.LINE_AA
    )

    # 1. Neutral Gray Swatch (18% Reflectance -> sRGB approx (119, 119, 119))
    # Used for von Kries Chromatic Adaptation
    gray_rect = (110, 75, 230, 135)
    cv2.rectangle(canvas, (gray_rect[0], gray_rect[1]), (gray_rect[2], gray_rect[3]), (119, 119, 119), -1)
    cv2.rectangle(canvas, (gray_rect[0], gray_rect[1]), (gray_rect[2], gray_rect[3]), (0, 0, 0), 1)
    cv2.putText(canvas, "18% GRAY", (120, 110), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (255, 255, 255), 1, cv2.LINE_AA)

    # 2. 100% White Reference Swatch (sRGB (255, 255, 255))
    white_rect = (250, 75, 370, 135)
    cv2.rectangle(canvas, (white_rect[0], white_rect[1]), (white_rect[2], white_rect[3]), (255, 255, 255), -1)
    cv2.rectangle(canvas, (white_rect[0], white_rect[1]), (white_rect[2], white_rect[3]), (0, 0, 0), 1)
    cv2.putText(canvas, "100% WHITE", (255, 110), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (0, 0, 0), 1, cv2.LINE_AA)

    # 3. Anchor Color References (Black, Red, Green, Blue swatches)
    anchor_colors = [
        ("BLACK", (0, 0, 0), (380, 75, 415, 135)),
        ("RED", (220, 30, 30), (420, 75, 455, 135)),
        ("GREEN", (30, 180, 30), (460, 75, 495, 135)),
    ]
    for label, color, (x1, y1, x2, y2) in anchor_colors:
        cv2.rectangle(canvas, (x1, y1), (x2, y2), color, -1)
        cv2.rectangle(canvas, (x1, y1), (x2, y2), (0, 0, 0), 1)

    # 4. Active Chemical Substrate Recess Well (Center-left)
    # This represents the Bismuth Subnitrate reaction paper strip window
    active_well = (110, 170, 370, 310)
    cv2.rectangle(canvas, (active_well[0], active_well[1]), (active_well[2], active_well[3]), (250, 250, 248), -1)
    cv2.rectangle(canvas, (active_well[0], active_well[1]), (active_well[2], active_well[3]), (100, 100, 100), 2)
    cv2.putText(canvas, "ACTIVE H2S SENSOR WELL", (130, 245), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (150, 150, 150), 1, cv2.LINE_AA)

    # 5. Micro-Sealed Expiry Patch Well (Center-right)
    expiry_well = (390, 170, 520, 310)
    cv2.rectangle(canvas, (expiry_well[0], expiry_well[1]), (expiry_well[2], expiry_well[3]), (245, 245, 240), -1)
    cv2.rectangle(canvas, (expiry_well[0], expiry_well[1]), (expiry_well[2], expiry_well[3]), (0, 150, 200), 2)
    cv2.putText(canvas, "EXPIRY", (430, 235), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 150, 200), 1, cv2.LINE_AA)
    cv2.putText(canvas, "PATCH", (435, 255), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 150, 200), 1, cv2.LINE_AA)

    # Footnote metadata
    cv2.putText(canvas, "FICKIAN DIFFUSION BARRIER (PTFE 0.2um) | LOT: 2026-BISMUTH-01", (100, 360),
                cv2.FONT_HERSHEY_SIMPLEX, 0.35, (80, 80, 80), 1, cv2.LINE_AA)

    return canvas
