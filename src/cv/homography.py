"""
SIH 26118 - Planar Homography & Perspective Rectification Engine

This module detects the 4 corner ArUco fiducial markers on the physical wristband
and applies a 3x3 homography transformation to convert arbitrary smartphone camera angles
into a canonical 600x400 orthorectified card image.
"""

import cv2
import numpy as np
from typing import Tuple, Optional, Dict, Any


def detect_aruco_corners(
    image: np.ndarray,
    aruco_dict_type: int = cv2.aruco.DICT_4X4_50
) -> Tuple[Optional[np.ndarray], Dict[int, np.ndarray]]:
    """
    Detects ArUco markers in the input image and applies sub-pixel corner refinement.

    Parameters:
    -----------
    image : np.ndarray
        Input RGB or BGR image frame from camera scan.
    aruco_dict_type : int
        ArUco dictionary specification (default DICT_4X4_50).

    Returns:
    --------
    Tuple[Optional[np.ndarray], Dict[int, np.ndarray]]
        - Ordered corner coordinates for IDs 0, 1, 2, 3 as (4, 2) array if all 4 found, else None.
        - Dictionary mapping marker_id -> corner coordinates.

    Complexity:
    -----------
    Time Complexity: O(N) where N is number of pixels in the input image.
    Space Complexity: O(K) where K is detected corner contours.
    """
    gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY) if len(image.shape) == 3 and image.shape[2] == 3 else image
    
    aruco_dict = cv2.aruco.getPredefinedDictionary(aruco_dict_type)
    detector_params = cv2.aruco.DetectorParameters()
    detector = cv2.aruco.ArucoDetector(aruco_dict, detector_params)

    corners, ids, rejected = detector.detectMarkers(gray)
    marker_map = {}

    if ids is not None and len(ids) > 0:
        # Refine corners with sub-pixel precision
        criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.01)
        for i, marker_id in enumerate(ids.flatten()):
            refined_corner = cv2.cornerSubPix(gray, corners[i], (5, 5), (-1, -1), criteria)
            marker_map[int(marker_id)] = refined_corner[0]

    # Check if all 4 target corner IDs (0: TL, 1: TR, 2: BR, 3: BL) are present
    required_ids = [0, 1, 2, 3]
    if all(rid in marker_map for rid in required_ids):
        # Extract the center point of each marker for robust homography mapping
        ordered_pts = []
        for rid in required_ids:
            pts = marker_map[rid]
            center = np.mean(pts, axis=0)
            ordered_pts.append(center)
        return np.array(ordered_pts, dtype=np.float32), marker_map

    return None, marker_map


def rectify_wristband_image(
    image: np.ndarray,
    target_width: int = 600,
    target_height: int = 400,
    aruco_dict_type: int = cv2.aruco.DICT_4X4_50
) -> Tuple[np.ndarray, bool, str]:
    """
    Computes 3x3 perspective homography matrix and warps the input camera frame
    into a canonical target_width x target_height orthorectified image.

    Target Canonical Corner Anchor Centers:
    - ID 0 (Top-Left): (55, 55)
    - ID 1 (Top-Right): (545, 55)
    - ID 2 (Bottom-Right): (545, 345)
    - ID 3 (Bottom-Left): (55, 345)

    Parameters:
    -----------
    image : np.ndarray
        Raw RGB camera image.
    target_width : int
        Width of orthorectified target image (default 600).
    target_height : int
        Height of orthorectified target image (default 400).

    Returns:
    --------
    Tuple[np.ndarray, bool, str]
        - Orthorectified RGB image (or raw image if homography fails).
        - Success flag (True if homography succeeded).
        - Status message explaining outcome.
    """
    detected_pts, marker_map = detect_aruco_corners(image, aruco_dict_type)

    if detected_pts is None:
        missing = [rid for rid in [0, 1, 2, 3] if rid not in marker_map]
        return image, False, f"Missing ArUco markers with IDs: {missing}. All 4 corner markers required."

    # Define target center points corresponding to marker_size=70 at corner padding=20
    # Center = 20 + 70/2 = 55 px
    dst_pts = np.array([
        [55.0, 55.0],                                 # ID 0: Top-Left center
        [target_width - 55.0, 55.0],                  # ID 1: Top-Right center
        [target_width - 55.0, target_height - 55.0],   # ID 2: Bottom-Right center
        [55.0, target_height - 55.0]                   # ID 3: Bottom-Left center
    ], dtype=np.float32)

    # Compute 3x3 Planar Homography Matrix H
    H, mask = cv2.findHomography(detected_pts, dst_pts, cv2.RANSAC, 5.0)

    if H is None:
        return image, False, "Planar homography computation failed due to degenerate corner points."

    # Warp perspective to obtain canonical orthorectified badge
    warped = cv2.warpPerspective(image, H, (target_width, target_height), flags=cv2.INTER_CUBIC)
    return warped, True, "Sub-pixel orthorectification successful."
