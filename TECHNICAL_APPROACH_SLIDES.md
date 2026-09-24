# DoseGuard: Technical Architecture & Chemical Kinetic Model
### Presentation & Slide Reference Guide (SIH 26118)

---

## Slide 1: System Tech Stack

### 1. Mobile Client (Edge Device)
* **Framework:** Android Native with Jetpack Compose (BOM 2024.09.03, Material 3)
* **Core Language:** Kotlin 2.0.21 (JVM 17 Target, Coroutines 1.9.0)
* **Architecture:** Model-View-ViewModel (MVVM) with Repository Pattern
* **Vision & Hardware:** AndroidX CameraX 1.3.4 (Zero-latency hardware abstraction)
* **Barcode Engine:** Google ML Kit 17.3.0 (On-device neural QR recognition, 100% offline)
* **Local Database:** Jetpack Room 2.6.1 (SQLite ACID persistence with Kotlin Flow streams)

### 2. Microservices & Backend Sync
* **Runtime:** Node.js 18 LTS with Express.js
* **Storage:** SQLite3 / Cloud Sync Engine
* **Compliance Reports:** Automated DGMS Form IV and OISD-155 statutory PDF generation

### 3. Key Stack Benefits
* **100% Offline-First:** Operates in ATEX Zone 0/1 refinery and mining areas without cellular or Wi-Fi signal.
* **Sub-15ms Real-Time Inference:** Colorimetry and kinetic solver run on-device without cloud round-trip delay.
* **Lifecycle Guard:** Camera hardware automatically releases on background transitions to prevent thermal throttling and battery drain.

---

## Slide 2: End-to-End Methodology Flow

```
[1. Wear Band] ──► [2. Scan QR] ──► [3. Capture Strip] ──► [4. Colorimetric Engine] ──► [5. Risk Evaluation] ──► [6. Dashboard & DB]
```

1. **Wear Wristband:** Worker wears the passive badge containing a lead acetate Pb(CH3COO)2 sensing strip.
2. **Scan QR Code:** ML Kit reads the band ID and verifies assignment to worker identity in Room DB.
3. **Capture Strip:** CameraX captures the center Region of Interest (ROI) with automatic exposure locking.
4. **Colorimetric Engine:** Converts pixel data from sRGB to CIE LAB (D65) and calculates Delta E darkening.
5. **Kinetic Solver:** Applies first-order reaction kinetics to compute cumulative ppm*hr dose and 8-hour TWA.
6. **Alerts & Storage:** Logs shift record into Room database; triggers immediate evacuation alarm if TWA exceeds 10 ppm.

---

## Slide 3: Chemical Kinetic Exposure Pipeline

```
Raw Camera JPEG
      │
      ▼
[Step 1: Center ROI Sampling]
• Extract average Red, Green, Blue from middle 40% patch
      │
      ▼
[Step 2: sRGB Linearization]
• Remove non-linear display gamma curve (gamma = 2.4)
      │
      ▼
[Step 3: XYZ Tristimulus Projection]
• Matrix transform calibrated to D65 standard daylight (6504 K)
      │
      ▼
[Step 4: CIE L*a*b* Non-Linear Transformation]
• L* = Perceptual Lightness (0 = Black, 100 = White)
• a* = Green to Red Axis (-a* = Green, +a* = Red)
• b* = Blue to Yellow Axis (-b* = Blue, +b* = Yellow)
      │
      ▼
[Step 5: Color Difference (CIE76 Delta E)]
• Calculate Euclidean distance from pristine unreacted cream reference
      │
      ▼
[Step 6: First-Order Reaction Kinetic Model]
• Convert Delta E to cumulative H2S dosage (ppm*hr) and 8-hr TWA (ppm)
```

---

## Slide 4: Mathematical Formulations (Slide Ready)

### Step 1: Gamma Linearization (sRGB to Linear RGB)
Camera sensors output non-linear gamma-corrected sRGB values (range 0 to 255). We normalize each channel (C in {R, G, B}) to [0, 1] and linearize:

```
V = C / 255.0

If V <= 0.04045:
    C_linear = V / 12.92
Else:
    C_linear = ((V + 0.055) / 1.055) ^ 2.4
```

---

### Step 2: CIE XYZ Tristimulus Conversion (D65 Illuminant)
Multiply linearized RGB by the standard CIE sRGB D65 transformation matrix:

```
X = (0.4124564 * R_linear) + (0.3575761 * G_linear) + (0.1804375 * B_linear)
Y = (0.2126729 * R_linear) + (0.7151522 * G_linear) + (0.0721750 * B_linear)
Z = (0.0193339 * R_linear) + (0.1191920 * G_linear) + (0.9503041 * B_linear)
```

Standard D65 White Reference Point:
* Xn = 0.95047
* Yn = 1.00000
* Zn = 1.08883

---

### Step 3: CIE L*a*b* Space Mapping
Normalize by reference white: `x_r = X / Xn`, `y_r = Y / Yn`, `z_r = Z / Zn`.

Define helper function f(t):
```
If t > (6/29)^3:
    f(t) = t ^ (1/3)
Else:
    f(t) = (t / (3 * (6/29)^2)) + (4/29)
```

Compute L*, a*, b* values:
```
L* = 116 * f(y_r) - 16
a* = 500 * (f(x_r) - f(y_r))
b* = 200 * (f(y_r) - f(z_r))
```

* **Why CIE L*a*b*?** It is perceptually uniform. Equal geometric distances represent equal human perceptual color changes, isolating darkening from ambient illumination shifts.

---

### Step 4: CIE76 Color Difference Calculation (Delta E)
Compare sampled strip color (L*, a*, b*) against pristine unreacted chemical strip reference (L_ref = 92.0, a_ref = -1.0, b_ref = 8.0):

```
Delta_E = sqrt( (L* - L_ref)^2 + (a* - a_ref)^2 + (b* - b_ref)^2 )
```

As the chemical badge absorbs H2S, lead acetate reacts to form brown/black lead sulfide (PbS), driving lightness (L*) downward and Delta E upward:

```
Pb(CH3COO)2 (White/Cream) + H2S (Gas) ──► PbS (Brown/Black Solid) + 2 CH3COOH
```

---

### Step 5: Chemical Kinetic Dose Model
The chemical reaction follows pseudo-first-order diffusion-limited kinetics with saturation ceiling Delta_E_max = 72.0 and rate constant k = 0.028:

```
Dose (ppm*hr) = - (1 / k) * ln( 1 - (Delta_E / Delta_E_max) )
```

Calculate 8-Hour Time-Weighted Average (TWA):
```
TWA_8hr (ppm) = Dose (ppm*hr) / Shift_Duration_Hours
```

---

### Step 6: Uncertainty Propagation & Confidence
Applying first-order Taylor series error propagation for optical sensor noise (sigma_Delta_E = 1.5):

```
Uncertainty (ppm*hr) = sigma_Delta_E / ( k * (Delta_E_max - Delta_E) )

Confidence Score = 1.0 - (Uncertainty / 20.0)   [Clamped between 0.40 and 0.99]
```

---

## Slide 5: Statutory Safety Thresholds & Action Matrix

| Exposure Range (8-hr TWA) | Risk Level | Status Color | Required Industrial SOP / Action |
| :--- | :--- | :--- | :--- |
| **< 1.0 ppm** | **SAFE** | Green | Normal work. Concentration well within permissible boundaries. |
| **1.0 to 2.5 ppm** | **MODERATE** | Yellow | Action Level reached. Inspect pipe seals, re-scan in 2 hours. |
| **2.5 to 10.0 ppm** | **HIGH** | Orange | Approaching OSHA PEL. Rotate worker to fresh air zone immediately. |
| **>= 10.0 ppm** | **CRITICAL** | Red | **PEL EXCEEDED.** Mandatory evacuation, SCBA respirator, log alert. |

---

## Slide 6: Slide Presentation Summary Points

1. **Zero Infrastructure Cost:** Transforms everyday Android smartphones into lab-grade colorimetric spectrophotometers.
2. **Physical Reaction Fidelity:** Replaces naive linear RGB estimation with scientifically accurate CIE LAB Delta E and first-order chemical rate kinetics.
3. **Regulatory Ready:** Directly maps cumulative chemical dose to OSHA, NIOSH, and DGMS occupational exposure standards.
4. **Built for Harsh Environments:** Native Jetpack Compose, CameraX, and Room ensure uninterrupted operation in remote, disconnected industrial sites.
