package com.doseguard.app.navigation

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.doseguard.app.repository.DoseGuardRepository
import com.doseguard.app.ui.screens.*
import com.doseguard.app.viewmodel.*

/**
 * DoseGuardNavGraph — wires all screens together in the industrial dosimeter application.
 *
 * Scopes:
 * - scanVm: shared between CameraCaptureScreen and ScanResultScreen
 * - bandAssignmentVm: shared across WorkerIdentification, ScanBandQr, and BandAssignmentSuccess
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun DoseGuardNavGraph(
    navController: NavHostController,
    repository: DoseGuardRepository
) {
    // ViewModel shared between Camera and Result screens
    val scanVm: ScanViewModel = viewModel()
    
    // Shared ViewModel for Worker Identification and Band Assignment flow
    val bandAssignmentVm: BandAssignmentViewModel = viewModel()

    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route
    ) {

        // ── 1. Splash ─────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── 2. Dashboard ──────────────────────────────────────────────────────
        composable(Screen.Dashboard.route) {
            val dashboardVm: SupervisorDashboardViewModel = viewModel()
            SupervisorDashboardScreen(
                vm = dashboardVm,
                onAssignDosimeterBand = {
                    bandAssignmentVm.resetAll()
                    navController.navigate(Screen.WorkerIdentification.route)
                },
                onQuickScanExposure = {
                    navController.navigate(Screen.QrScan.route)
                },
                onWorkerSelected = { workerId, bandId ->
                    navController.navigate(Screen.WorkerDetail.createRoute(workerId, bandId))
                },
                onViewHistory = { workerId, bandId ->
                    navController.navigate(Screen.ExposureHistory.createRoute(workerId, bandId))
                },
                onOpenAlerts = { workerId, bandId ->
                    navController.navigate(Screen.Alert.createRoute(workerId, bandId))
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // ── 3. Worker Identification (Worker-First Assignment Step 1) ─────────
        composable(Screen.WorkerIdentification.route) {
            WorkerIdentificationScreen(
                vm = bandAssignmentVm,
                onContinueToScanBand = { workerId ->
                    navController.navigate(Screen.ScanBandQr.createRoute(workerId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 4. Scan Band QR & Validation (Worker-First Assignment Step 2) ──────
        composable(
            route = Screen.ScanBandQr.route,
            arguments = listOf(navArgument("workerId") { type = NavType.StringType })
        ) { backStack ->
            val workerId = backStack.arguments?.getString("workerId") ?: ""
            ScanBandQrScreen(
                workerId = workerId,
                vm = bandAssignmentVm,
                onAssignedSuccess = { wId, bandId ->
                    navController.navigate(Screen.BandAssignmentSuccess.createRoute(wId, bandId)) {
                        popUpTo(Screen.WorkerIdentification.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 5. Band Assignment Success (Worker-First Assignment Step 3) ───────
        composable(
            route = Screen.BandAssignmentSuccess.route,
            arguments = listOf(
                navArgument("workerId") { type = NavType.StringType },
                navArgument("bandId") { type = NavType.StringType }
            )
        ) { backStack ->
            val workerId = backStack.arguments?.getString("workerId") ?: ""
            val bandId = backStack.arguments?.getString("bandId") ?: ""
            BandAssignmentSuccessScreen(
                workerId = workerId,
                bandId = bandId,
                repository = repository,
                vm = bandAssignmentVm,
                onDone = {
                    bandAssignmentVm.resetAll()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onStartExposureScan = { wId, bId ->
                    navController.navigate(Screen.CameraCapture.createRoute(bId, wId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                },
                onAssignAnother = {
                    bandAssignmentVm.resetAll()
                    navController.navigate(Screen.WorkerIdentification.route) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }

        // ── 6. Legacy / Quick QR Scan ─────────────────────────────────────────
        composable(Screen.QrScan.route) {
            val vm: QrScanViewModel = viewModel()
            QrScanScreen(
                vm = vm,
                onBandAssigned = { bandId, workerId ->
                    navController.navigate(Screen.WorkerDetail.createRoute(workerId, bandId))
                },
                onNewBand = { bandId, qrData ->
                    navController.navigate(Screen.WorkerRegistration.createRoute(bandId, qrData))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 7. Worker Registration ────────────────────────────────────────────
        composable(
            route     = Screen.WorkerRegistration.route,
            arguments = listOf(
                navArgument("bandId") { type = NavType.StringType },
                navArgument("qrData") { type = NavType.StringType }
            )
        ) { backStack ->
            val bandId = backStack.arguments?.getString("bandId") ?: ""
            val qrData = android.net.Uri.decode(backStack.arguments?.getString("qrData") ?: "")
            val vm: WorkerRegistrationViewModel = viewModel()

            WorkerRegistrationScreen(
                bandId   = bandId,
                qrData   = qrData,
                vm       = vm,
                onRegistered = { workerId, bid ->
                    navController.navigate(Screen.WorkerDetail.createRoute(workerId, bid)) {
                        popUpTo(Screen.QrScan.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 8. Worker Detail ──────────────────────────────────────────────────
        composable(
            route     = Screen.WorkerDetail.route,
            arguments = listOf(
                navArgument("workerId") { type = NavType.StringType },
                navArgument("bandId")   { type = NavType.StringType }
            )
        ) { backStack ->
            val workerId = backStack.arguments?.getString("workerId") ?: ""
            val bandId   = backStack.arguments?.getString("bandId")   ?: ""

            // Load worker context into shared ScanViewModel
            LaunchedEffect(workerId, bandId) { scanVm.loadContext(workerId, bandId) }

            WorkerDetailScreen(
                workerId   = workerId,
                bandId     = bandId,
                repository = repository,
                onStartScan = {
                    navController.navigate(Screen.CameraCapture.createRoute(bandId, workerId))
                },
                onViewHistory = {
                    navController.navigate(Screen.ExposureHistory.createRoute(workerId, bandId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 9. Camera Capture ─────────────────────────────────────────────────
        composable(
            route     = Screen.CameraCapture.route,
            arguments = listOf(
                navArgument("bandId")   { type = NavType.StringType },
                navArgument("workerId") { type = NavType.StringType }
            )
        ) { backStack ->
            val bandId   = backStack.arguments?.getString("bandId")   ?: ""
            val workerId = backStack.arguments?.getString("workerId") ?: ""

            CameraCaptureScreen(
                bandId   = bandId,
                workerId = workerId,
                vm       = scanVm,
                onCaptured = {
                    navController.navigate(Screen.ScanResult.createRoute(bandId, workerId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 10. Scan Result ───────────────────────────────────────────────────
        composable(
            route     = Screen.ScanResult.route,
            arguments = listOf(
                navArgument("bandId")   { type = NavType.StringType },
                navArgument("workerId") { type = NavType.StringType }
            )
        ) { backStack ->
            val bandId   = backStack.arguments?.getString("bandId")   ?: ""
            val workerId = backStack.arguments?.getString("workerId") ?: ""

            ScanResultScreen(
                bandId       = bandId,
                workerId     = workerId,
                vm           = scanVm,
                onViewHistory = {
                    navController.navigate(Screen.ExposureHistory.createRoute(workerId, bandId)) {
                        popUpTo(Screen.WorkerDetail.createRoute(workerId, bandId))
                    }
                },
                onScanAgain = {
                    scanVm.reset()
                    navController.navigate(Screen.CameraCapture.createRoute(bandId, workerId)) {
                        popUpTo(Screen.ScanResult.createRoute(bandId, workerId)) { inclusive = true }
                    }
                },
                onShowAlert = {
                    navController.navigate(Screen.Alert.createRoute(workerId, bandId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 11. Exposure History ──────────────────────────────────────────────
        composable(
            route     = Screen.ExposureHistory.route,
            arguments = listOf(
                navArgument("workerId") { type = NavType.StringType },
                navArgument("bandId")   { type = NavType.StringType; defaultValue = "all" }
            )
        ) { backStack ->
            val workerId = backStack.arguments?.getString("workerId") ?: ""
            val bandId   = backStack.arguments?.getString("bandId")   ?: "all"
            val vm: HistoryViewModel = viewModel()
            ExposureHistoryScreen(
                workerId = workerId,
                bandId   = bandId,
                vm       = vm,
                onBack   = { navController.popBackStack() }
            )
        }

        // ── 12. Alert ─────────────────────────────────────────────────────────
        composable(
            route     = Screen.Alert.route,
            arguments = listOf(
                navArgument("workerId") { type = NavType.StringType },
                navArgument("bandId")   { type = NavType.StringType }
            )
        ) { backStack ->
            val workerId = backStack.arguments?.getString("workerId") ?: ""
            val bandId   = backStack.arguments?.getString("bandId")   ?: ""
            val vm: AlertViewModel = viewModel()

            AlertScreen(
                workerId      = workerId,
                bandId        = bandId,
                vm            = vm,
                onBack        = { navController.popBackStack() },
                onViewHistory = {
                    navController.navigate(Screen.ExposureHistory.createRoute(workerId, bandId))
                }
            )
        }

        // ── 13. Settings ──────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
