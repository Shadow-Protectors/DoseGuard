package com.doseguard.app.navigation

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
 * DoseGuardNavGraph — wires all screens together.
 *
 * The ScanViewModel is scoped to the NavGraph (not individual composables) so that
 * CameraCaptureScreen and ScanResultScreen can share the same instance.
 *
 * The repository is passed directly to WorkerDetailScreen because it only needs
 * simple suspend reads and doesn't require a full ViewModel.
 */
@Composable
fun DoseGuardNavGraph(
    navController: NavHostController,
    repository: DoseGuardRepository
) {
    // ViewModel shared between Camera and Result screens
    val scanVm: ScanViewModel = viewModel()

    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route
    ) {

        // ── 1. Splash ─────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Screen.QrScan.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── 2. QR Scan ────────────────────────────────────────────────────────
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

        // ── 3. Worker Registration ────────────────────────────────────────────
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

        // ── 4. Worker Detail ──────────────────────────────────────────────────
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
                    navController.navigate(Screen.ExposureHistory.createRoute(workerId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 5. Camera Capture ─────────────────────────────────────────────────
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

        // ── 6. Scan Result ────────────────────────────────────────────────────
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
                    navController.navigate(Screen.ExposureHistory.createRoute(workerId)) {
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

        // ── 7. Exposure History ───────────────────────────────────────────────
        composable(
            route     = Screen.ExposureHistory.route,
            arguments = listOf(navArgument("workerId") { type = NavType.StringType })
        ) { backStack ->
            val workerId = backStack.arguments?.getString("workerId") ?: ""
            val vm: HistoryViewModel = viewModel()
            ExposureHistoryScreen(
                workerId = workerId,
                bandId   = "",   // history screen queries by workerId only
                vm       = vm,
                onBack   = { navController.popBackStack() }
            )
        }

        // ── 8. Alert ──────────────────────────────────────────────────────────
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
                    navController.navigate(Screen.ExposureHistory.createRoute(workerId))
                }
            )
        }

        // ── 9. Settings ───────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
