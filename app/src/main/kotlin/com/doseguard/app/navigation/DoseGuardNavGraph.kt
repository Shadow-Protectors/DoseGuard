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
 * DoseGuardNavGraph — wires all screens together including validity gating,
 * replacement band assignment, and continuous exposure history.
 */
@Composable
fun DoseGuardNavGraph(
    navController: NavHostController,
    repository: DoseGuardRepository
) {
    val scanVm: ScanViewModel = viewModel()
    // Shared across the three band-assignment screens so the identified worker,
    // the validated band and the confirmation state survive navigation.
    val assignVm: BandAssignmentViewModel = viewModel()

    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route
    ) {

        // ── 1. Splash ─────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Screen.BandAssignment.route) {
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
                onBandInvalid = { bandId, reason, workerId ->
                    navController.navigate(Screen.BandInvalid.createRoute(bandId, reason, workerId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 2b. Band Assignment — Screen 1: Worker Identification ────────────
        composable(Screen.BandAssignment.route) {
            LaunchedEffect(Unit) { assignVm.resetBandScan() }
            BandAssignmentScreen(
                vm = assignVm,
                onAssignBand = { workerId ->
                    assignVm.resetBandScan()
                    navController.navigate(Screen.BandScanAssign.createRoute(workerId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 2c. Band Assignment — Screen 2: Scan Band QR ──────────────────────
        composable(
            route = Screen.BandScanAssign.route,
            arguments = listOf(navArgument("workerId") { type = NavType.StringType })
        ) { backStack ->
            val workerId = backStack.arguments?.getString("workerId") ?: ""
            val lookup by assignVm.workerLookup.collectAsState()
            LaunchedEffect(workerId) { assignVm.ensureWorker(workerId) }

            BandScanAssignScreen(
                worker = (lookup as? BandAssignmentViewModel.WorkerLookup.Found)?.worker,
                vm     = assignVm,
                onAssigned = { assignedWorkerId, bandId, assignedTime ->
                    navController.navigate(
                        Screen.AssignmentSuccess.createRoute(assignedWorkerId, bandId, assignedTime)
                    ) {
                        popUpTo(Screen.BandScanAssign.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 2d. Band Assignment — Screen 3: Success ───────────────────────────
        composable(
            route = Screen.AssignmentSuccess.route,
            arguments = listOf(
                navArgument("workerId")     { type = NavType.StringType },
                navArgument("bandId")       { type = NavType.StringType },
                navArgument("assignedTime") { type = NavType.LongType }
            )
        ) { backStack ->
            val workerId     = backStack.arguments?.getString("workerId") ?: ""
            val bandId       = backStack.arguments?.getString("bandId") ?: ""
            val assignedTime = backStack.arguments?.getLong("assignedTime") ?: System.currentTimeMillis()
            val lookup by assignVm.workerLookup.collectAsState()
            LaunchedEffect(workerId) { assignVm.ensureWorker(workerId) }
            val worker = (lookup as? BandAssignmentViewModel.WorkerLookup.Found)?.worker

            AssignmentSuccessScreen(
                employeeId   = worker?.employeeId ?: workerId,
                workerName   = worker?.name ?: "",
                bandId       = bandId,
                assignedTime = assignedTime,
                onDone = {
                    navController.navigate(Screen.BandAssignment.route) {
                        popUpTo(Screen.BandAssignment.route) { inclusive = true }
                    }
                    assignVm.reset()
                },
                onStartScan = {
                    navController.navigate(Screen.WorkerDetail.createRoute(workerId, bandId)) {
                        popUpTo(Screen.BandAssignment.route)
                    }
                    assignVm.resetBandScan()
                }
            )
        }

        // ── 3. Band Invalidation Gate (Blocked Camera / Replacement) ──────────
        composable(
            route = Screen.BandInvalid.route,
            arguments = listOf(
                navArgument("bandId")   { type = NavType.StringType },
                navArgument("reason")   { type = NavType.StringType },
                navArgument("workerId") { type = NavType.StringType }
            )
        ) { backStack ->
            val bandId   = backStack.arguments?.getString("bandId")   ?: ""
            val reason   = backStack.arguments?.getString("reason")   ?: ""
            val workerId = backStack.arguments?.getString("workerId") ?: ""

            BandInvalidScreen(
                bandId             = bandId,
                reason             = reason,
                workerId           = workerId,
                repository         = repository,
                onIssueReplacement = { wId ->
                    navController.navigate(Screen.QrScan.route) {
                        popUpTo(Screen.QrScan.route) { inclusive = true }
                    }
                },
                onViewHistory = { wId ->
                    navController.navigate(Screen.ExposureHistory.createRoute(wId, bandId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── 4. Worker Registration / Replacement ─────────────────────────────
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

        // ── 5. Worker Detail ──────────────────────────────────────────────────
        composable(
            route     = Screen.WorkerDetail.route,
            arguments = listOf(
                navArgument("workerId") { type = NavType.StringType },
                navArgument("bandId")   { type = NavType.StringType }
            )
        ) { backStack ->
            val workerId = backStack.arguments?.getString("workerId") ?: ""
            val bandId   = backStack.arguments?.getString("bandId")   ?: ""

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

        // ── 6. Camera Capture ─────────────────────────────────────────────────
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

        // ── 7. Scan Result ────────────────────────────────────────────────────
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

        // ── 8. Exposure History (With 7-Day / 30-Day Filtering) ───────────────
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

        // ── 9. Alert ──────────────────────────────────────────────────────────
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

        // ── 10. Settings ──────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
