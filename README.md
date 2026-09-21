# DoseGuard: Smart Passive Colorimetric Dosimeter & Worker Safety Platform

DoseGuard (SIH 26118) is an end-to-end occupational safety and colorimetric dosimeter platform engineered for high-risk industrial environments such as refineries, chemical processing units, and mining operations. It provides automated exposure estimation for hazardous gases (such as Hydrogen Sulfide / H2S) through computer vision, sub-pixel fiducial marker homography, von Kries chromatic adaptation, and pseudo-first-order kinetic saturation modeling.

---

## System Architecture & Components

The repository comprises three integrated layers:

```
                                  +---------------------------------------+
                                  |     Physical Colorimetric Badge       |
                                  |  (Bismuth Subnitrate + ArUco Target)  |
                                  +-------------------+-------------------+
                                                      |
                                                      v
+---------------------------------------------------------------------------------------------------------+
|                                      Client Application Layer                                           |
|                                                                                                         |
|   +---------------------------------------------+     +---------------------------------------------+   |
|   |         Native Android Mobile App           |     |         Interactive Web Simulator           |   |
|   |                 (app/)                      |     |                 (app.py)                    |   |
|   |  - Kotlin, Jetpack MVVM, Material Design 3  |     |  - Streamlit multi-screen operator portal   |   |
|   |  - CameraX & ML Kit barcode scanning        |     |  - Direct OpenCV homography & solver engine |   |
|   |  - Room SQLite database (offline-first)     |     |  - Real-time Delta-E & TWA risk monitoring  |   |
|   |  - WorkManager background cloud sync        |     |  - DGMS Form IV PDF report generation       |   |
|   +----------------------+----------------------+     +----------------------+----------------------+   |
+--------------------------|---------------------------------------------------|--------------------------+
                           | HTTPS REST API (JWT Authenticated)                |
                           v                                                   v
+---------------------------------------------------------------------------------------------------------+
|                                      Backend Cloud Service                                              |
|                                         (backend_api/)                                                  |
|                                                                                                         |
|   - Node.js & Express RESTful API with Helmet, Rate Limiting, and CORS                                   |
|   - MongoDB data persistence via Mongoose schemas (Users, ShiftLogs)                                    |
|   - Statutory DGMS Form IV compliance audit logs and SHA-256 tamper verification                       |
+---------------------------------------------------------------------------------------------------------+
```

### 1. Native Android Application (`app/`)
- Developed in Kotlin using Android Jetpack architecture (MVVM, ViewBinding, Room).
- CameraX capture pipeline for exposure-controlled optical scanning.
- Offline-first Room database storing `ShiftLogEntity` and `WorkerEntity` records.
- Background synchronization via Android `WorkManager` with exponential backoff retry logic.

### 2. Core Computer Vision & Kinetic Engine (`src/` & `app.py`)
- **Fiducial Marker Homography (`src/cv/homography.py`):** Sub-pixel detection of 4 corner ArUco markers (DICT_4X4_50) and perspective rectification yielding canonical 600x400 orthorectified image tensors.
- **von Kries Lighting Adaptation (`src/cv/calibration.py`):** Normalizes illumination by computing white and 18% neutral gray reflectance swatches, compensating for tungsten or sodium vapor ambient casts.
- **CIELAB Color Shift Calculation:** Extracts $L^*a^*b^*$ coordinates and calculates Euclidean Delta-E ($\Delta E_{ab}^*$).
- **Pseudo-First-Order Kinetic Model (`src/cv/kinetics.py`):** Inverts reaction kinetics to compute cumulative dosage ($D$ in $\text{ppm}\cdot\text{hr}$) and 8-hour Time-Weighted Average (TWA):
  $$\text{TWA}_{8\text{hr}} = \frac{D}{8.0} \quad [\text{ppm}]$$
- **Statutory Reporting (`src/reports/dgms_exporter.py`):** Compiles Directorate General of Mines Safety (DGMS) Form IV and OISD 155 compliant PDF health registers.

### 3. Enterprise REST API (`backend_api/`)
- Express and Node.js REST API with JWT bearer token authentication.
- Secure password hashing using bcrypt.
- Shift log cloud ingestion, tamper-evident SHA-256 hash tracking, and administrative aggregation.

---

## Directory Structure

```
DoseGuard/
├── app/                        # Native Android application
│   ├── src/main/
│   │   ├── AndroidManifest.xml # Android app manifest and hardware permissions
│   │   ├── kotlin/             # Kotlin source code (CV solver, Room DB, WorkManager, UI)
│   │   └── res/                # Material Design 3 layouts, drawables, styles
│   └── build.gradle            # App-level Gradle build configuration
├── backend_api/                # Node.js & Express REST API
│   ├── src/
│   │   ├── config/             # MongoDB database connection
│   │   ├── controllers/        # Auth, dosimeter, and report route controllers
│   │   ├── middleware/         # JWT verification and express rate limiters
│   │   ├── models/             # Mongoose schemas (User, ShiftLog)
│   │   ├── routes/             # REST endpoints (/api/auth, /api/dosimeter, /api/reports)
│   │   └── app.js              # Express application assembly
│   ├── test/                   # API test suite (Node test runner)
│   ├── .env.example            # Environment configuration template
│   ├── package.json            # Node dependencies and npm scripts
│   └── server.js               # Service entrypoint
├── gradle/                     # Gradle wrapper binary and configuration
├── src/                        # Core Python algorithms and engines
│   ├── cv/                     # Homography, von Kries calibration, and kinetics solvers
│   ├── db/                     # SQLite persistence and cloud sync client
│   ├── hardware/               # Printable ArUco calibration card generator
│   └── reports/                # DGMS Form IV PDF exporter
├── tests/                      # Automated test suite for Python CV algorithms
├── app.py                      # Interactive Streamlit operator dashboard
├── build.gradle                # Root project Gradle build script
├── gradle.properties           # Portable JVM and AndroidX settings
├── gradlew / gradlew.bat       # Cross-platform Gradle wrapper executables
├── requirements.txt            # Python dependencies
├── settings.gradle             # Multi-module Gradle configuration
└── README.md                   # Repository documentation
```

---

## Getting Started

### 1. Python Computer Vision & Simulator

```bash
# 1. Install dependencies
pip install -r requirements.txt

# 2. Run automated tests
python -m pytest

# 3. Launch interactive simulator
streamlit run app.py
```

### 2. Backend REST API

```bash
cd backend_api

# 1. Install Node dependencies
npm install

# 2. Configure environment
cp .env.example .env

# 3. Run test suite
npm test

# 4. Start API server
npm start
```

### 3. Native Android Application

Open the root project directory in Android Studio (Jellyfish or newer) with Android SDK 34 and JDK 17 configured. Build and run using the included Gradle wrapper:

```bash
./gradlew assembleDebug
```

---

## Statutory Compliance & Standards

- **DGMS Form IV:** Compliance with statutory record-keeping mandated by the Directorate General of Mines Safety.
- **OISD Standard 155:** Occupational health monitoring requirements for personal protective equipment and gas exposure registries.
- **ACGIH Thresholds:** Integrated safety thresholds (TWA $< 1.0 \text{ ppm}$ Safe, $1.0 - 2.5 \text{ ppm}$ Moderate, $2.5 - 10.0 \text{ ppm}$ High, $\ge 10.0 \text{ ppm}$ Critical).

---

## License

This project is licensed under the MIT License.
