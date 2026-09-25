package com.doseguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.ui.components.QrScannerCamera
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.BandAssignmentViewModel

/**
 * Screen 2 — Scan Dosimeter Band QR Code.
 *
 * Full-screen scanner with green corner guides. Validates the band, surfaces
 * rejection reasons in a bottom sheet, and asks for explicit confirmation
 * before the Worker ↔ Band mapping is written to the database.
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
fun BandScanAssignScreen(
    worker: WorkerEntity?,
    vm: BandAssignmentViewModel,
    onAssigned: (workerId: String, bandId: String, assignedTime: Long) -> Unit,
    onBack: () -> Unit
) {
    val scanState by vm.bandScan.collectAsState()
    var showManualEntry by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf("") }

    LaunchedEffect(scanState) {
        val s = scanState
        if (s is BandAssignmentViewModel.BandScan.Success) {
            onAssigned(s.assignment.workerId, s.band.bandId, s.assignment.assignedTime)
        }
    }

    val scanningEnabled = scanState is BandAssignmentViewModel.BandScan.Scanning

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        QrScannerCamera(
            modifier = Modifier.fillMaxSize(),
            enabled = scanningEnabled,
            onCodeScanned = vm::onBandScanned,
            onPermissionDenied = {
                TextButton(onClick = { showManualEntry = true }) {
                    Text("Enter Band ID manually", color = Color.White)
                }
            }
        )

        // ── Top instruction bar ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Spacer(Modifier.width(2.dp))
                Text(
                    "Scan Dosimeter Band QR Code",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            worker?.let {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Person,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${it.employeeId} · ${it.name}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── Bottom instruction + manual fallback ─────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (scanState is BandAssignmentViewModel.BandScan.Validating) {
                CircularProgressIndicator(color = StatusSafe, strokeWidth = 3.dp)
                Spacer(Modifier.height(12.dp))
                Text("Validating band…", color = Color.White, fontSize = 14.sp)
            } else {
                Text(
                    "Place the QR code inside the frame.",
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showManualEntry = true }) {
                    Text("Enter Band ID manually", color = Color(0xFF7FD8FF))
                }
            }
        }

        // ── Rejection sheet ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = scanState is BandAssignmentViewModel.BandScan.Rejected,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val s = scanState as? BandAssignmentViewModel.BandScan.Rejected
            if (s != null) {
                RejectionSheet(
                    title = s.title,
                    message = s.message,
                    onScanAgain = { vm.resetBandScan() },
                    onCancel = onBack
                )
            }
        }
    }

    // ── Confirmation dialog ──────────────────────────────────────────────────
    val confirmState = scanState as? BandAssignmentViewModel.BandScan.Confirm
    if (confirmState != null && worker != null) {
        ConfirmAssignmentDialog(
            worker = worker,
            bandId = confirmState.band.bandId,
            onConfirm = vm::confirmAssignment,
            onDismiss = vm::cancelConfirmation
        )
    }

    if (scanState is BandAssignmentViewModel.BandScan.Saving) {
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp, color = NavyPrimary)
                    Spacer(Modifier.width(16.dp))
                    Text("Saving assignment…", color = TextPrimary)
                }
            }
        }
    }

    // ── Manual band entry ────────────────────────────────────────────────────
    if (showManualEntry) {
        AlertDialog(
            onDismissRequest = { showManualEntry = false },
            icon = { Icon(Icons.Filled.Keyboard, null, tint = NavyPrimary) },
            title = { Text("Enter Band ID", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    singleLine = true,
                    placeholder = { Text("BAND-001285") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManualEntry = false
                        if (manualInput.isNotBlank()) {
                            vm.resetBandScan()
                            vm.onBandScanned(manualInput.trim())
                            manualInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) { Text("Validate Band") }
            },
            dismissButton = {
                TextButton(onClick = { showManualEntry = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RejectionSheet(
    title: String,
    message: String,
    onScanAgain: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = CardWhite,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(StatusCriticalBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.WarningAmber, null, tint = StatusCritical)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(message, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
            Spacer(Modifier.height(22.dp))
            Row {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text("Cancel") }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onScanAgain,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.QrCodeScanner, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Again", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ConfirmAssignmentDialog(
    worker: WorkerEntity,
    bandId: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = CardWhite,
        icon = { Icon(Icons.Filled.AssignmentTurnedIn, null, tint = NavyPrimary) },
        title = {
            Text("Confirm Assignment", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column {
                LabelBlock("Worker", "${worker.employeeId}\n${worker.name}")
                Spacer(Modifier.height(14.dp))
                LabelBlock("Band", bandId)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Assign this band?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) { Text("Assign", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun LabelBlock(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceBg, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(label.uppercase(), fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
