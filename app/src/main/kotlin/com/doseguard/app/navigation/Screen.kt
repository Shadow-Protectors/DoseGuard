package com.doseguard.app.navigation

/**
 * Sealed class defining all Navigation Compose routes in the app.
 * Using a sealed class (instead of string constants) gives compile-time safety.
 */
sealed class Screen(val route: String) {
    object Splash          : Screen("splash")
    object QrScan          : Screen("qr_scan")
    object WorkerRegistration : Screen("worker_registration/{bandId}/{qrData}") {
        fun createRoute(bandId: String, qrData: String) =
            "worker_registration/$bandId/${android.net.Uri.encode(qrData)}"
    }
    object WorkerDetail    : Screen("worker_detail/{workerId}/{bandId}") {
        fun createRoute(workerId: String, bandId: String) = "worker_detail/$workerId/$bandId"
    }
    object CameraCapture   : Screen("camera_capture/{bandId}/{workerId}") {
        fun createRoute(bandId: String, workerId: String) = "camera_capture/$bandId/$workerId"
    }
    object ScanResult      : Screen("scan_result/{bandId}/{workerId}") {
        fun createRoute(bandId: String, workerId: String) = "scan_result/$bandId/$workerId"
    }
    object ExposureHistory : Screen("exposure_history/{workerId}") {
        fun createRoute(workerId: String) = "exposure_history/$workerId"
    }
    object Alert           : Screen("alert/{workerId}/{bandId}") {
        fun createRoute(workerId: String, bandId: String) = "alert/$workerId/$bandId"
    }
    object Settings        : Screen("settings")
}
