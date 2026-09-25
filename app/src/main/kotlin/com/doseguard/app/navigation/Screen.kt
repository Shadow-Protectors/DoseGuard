package com.doseguard.app.navigation

/**
 * Sealed class defining all Navigation Compose routes in the app.
 * Using a sealed class (instead of string constants) gives compile-time safety.
 */
sealed class Screen(val route: String) {
    object Splash          : Screen("splash")
    object Dashboard       : Screen("dashboard")
    object QrScan          : Screen("qr_scan")
    object WorkerRegistration : Screen("worker_registration/{bandId}/{qrData}") {
        fun createRoute(bandId: String, qrData: String) =
            "worker_registration/$bandId/${android.net.Uri.encode(qrData)}"
    }
    object WorkerDetail    : Screen("worker_detail/{workerId}/{bandId}") {
        fun createRoute(workerId: String, bandId: String) = "worker_detail/$workerId/$bandId"
    }
    object BandInvalid     : Screen("band_invalid/{bandId}/{reason}/{workerId}") {
        fun createRoute(bandId: String, reason: String, workerId: String = "") =
            "band_invalid/$bandId/$reason/${if (workerId.isBlank()) "none" else workerId}"
    }
    object CameraCapture   : Screen("camera_capture/{bandId}/{workerId}") {
        fun createRoute(bandId: String, workerId: String) = "camera_capture/$bandId/$workerId"
    }
    object ScanResult      : Screen("scan_result/{bandId}/{workerId}") {
        fun createRoute(bandId: String, workerId: String) = "scan_result/$bandId/$workerId"
    }
    object ExposureHistory : Screen("exposure_history/{workerId}/{bandId}") {
        fun createRoute(workerId: String, bandId: String = "") =
            "exposure_history/$workerId/${if (bandId.isBlank()) "all" else bandId}"
    }
    object Alert           : Screen("alert/{workerId}/{bandId}") {
        fun createRoute(workerId: String, bandId: String) = "alert/$workerId/$bandId"
    }
    object Settings        : Screen("settings")
}
