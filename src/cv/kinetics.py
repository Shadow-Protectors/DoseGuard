"""
SIH 26118 - Pseudo-First-Order Kinetic Dose Mapping & Uncertainty Model

Maps optical Delta-E color differences into cumulative gas exposure dosage D (ppm*hr),
calculates 8-hour Time Weighted Average (TWA in ppm), computes propagated measurement
uncertainties, and determines safety action levels based on OSHA/NIOSH and DGMS standards.
"""

import numpy as np
from typing import Dict, Any, Tuple


def delta_e_to_cumulative_dose(
    delta_e: float,
    delta_e_max: float = 75.0,
    k_rate: float = 0.025,
    sigma_delta_e: float = 1.2
) -> Tuple[float, float]:
    """
    Maps optical Delta-E to cumulative dose D (ppm*hr) using pseudo-first-order kinetic saturation:

    D = - (1 / k) * ln(1 - delta_e / delta_e_max)

    Uncertainty propagation:
    sigma_D = sigma_delta_e / (k * (delta_e_max - delta_e))

    Parameters:
    -----------
    delta_e : float
        Measured optical color difference from unexposed baseline.
    delta_e_max : float
        Asymptotic maximum saturation Delta-E (default 75.0).
    k_rate : float
        Reaction rate constant in (ppm*hr)^-1 (default 0.025).
    sigma_delta_e : float
        Optical measurement noise standard deviation (default 1.2).

    Returns:
    --------
    Tuple[float, float]
        - Estimated cumulative dose D in ppm*hr.
        - Measurement uncertainty sigma_D in ppm*hr.

    Complexity:
    -----------
    Time Complexity: O(1)
    Space Complexity: O(1)
    """
    # Clamp delta_e to avoid logarithm of non-positive values
    safe_delta_e = min(max(0.0, delta_e), delta_e_max - 0.5)

    ratio = safe_delta_e / delta_e_max
    dose_ppm_hr = - (1.0 / k_rate) * np.log(1.0 - ratio)

    # Calculate derivative dD/d(Delta_E) for error propagation
    denominator = k_rate * (delta_e_max - safe_delta_e)
    sigma_dose = sigma_delta_e / denominator if denominator > 0 else 5.0

    return float(dose_ppm_hr), float(sigma_dose)


def evaluate_shift_exposure(
    delta_e: float,
    shift_hours: float = 8.0,
    is_expired: bool = False
) -> Dict[str, Any]:
    """
    Evaluates complete shift exposure health metrics.

    Parameters:
    -----------
    delta_e : float
        Measured optical Delta-E.
    shift_hours : float
        Total duration of shift in hours (default 8.0).
    is_expired : bool
        Flag indicating if the badge expiry patch failed.

    Returns:
    --------
    Dict[str, Any]
        Dictionary with cumulative dose, TWA, uncertainty, risk status, and actionable advice.
    """
    if is_expired:
        return {
            "status": "INVALID",
            "risk_level": "EXPIRED_BADGE",
            "message": "Badge expiry patch indicates shelf-life degradation. Discard badge and scan a fresh dosimeter.",
            "dose_ppm_hr": 0.0,
            "uncertainty_ppm_hr": 0.0,
            "twa_8hr_ppm": 0.0,
            "action_required": "Replace Badge Immediately"
        }

    dose_ppm_hr, uncertainty = delta_e_to_cumulative_dose(delta_e)
    
    # 8-hour TWA normalized concentration
    effective_shift = max(0.1, shift_hours)
    twa_8hr_ppm = dose_ppm_hr / effective_shift

    # Determine risk action level
    if twa_8hr_ppm < 1.0:
        risk_level = "SAFE"
        action = "Normal operation. Exposure within permissible occupational health limits."
        color_code = "GREEN"
    elif twa_8hr_ppm < 2.5:
        risk_level = "CAUTION"
        action = "Action level reached. Inspect ventilation seals and re-check badge in 2 hours."
        color_code = "YELLOW"
    elif twa_8hr_ppm < 10.0:
        risk_level = "HIGH"
        action = "Approaching Permissible Exposure Limit (PEL). Rotate worker to fresh air zone."
        color_code = "ORANGE"
    else:
        risk_level = "CRITICAL"
        action = "PERMISSIBLE EXPOSURE LIMIT EXCEEDED (> 10 ppm TWA). Immediate evacuation & medical triage required."
        color_code = "RED"

    return {
        "status": "VALID",
        "dose_ppm_hr": round(dose_ppm_hr, 2),
        "uncertainty_ppm_hr": round(uncertainty, 2),
        "twa_8hr_ppm": round(twa_8hr_ppm, 2),
        "shift_hours": shift_hours,
        "risk_level": risk_level,
        "color_code": color_code,
        "action_required": action
    }
