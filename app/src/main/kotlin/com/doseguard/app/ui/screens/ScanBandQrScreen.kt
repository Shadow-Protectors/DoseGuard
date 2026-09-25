package com.doseguard.app.ui.screens

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.BandAssignmentViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.*

@androidx.camera.core.ExperimentalGetImage
@Composable
fun ScanBandQrScreen(
    workerId: String,
    vm: BandAssignmentViewModel,
    onAssignedSuccess: (workerId: String, bandId: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by vm.uiState.collectAsState()

    var showManualDialog by remember { mutableStateOf(false) }
    var manualBandInput by remember { mutableStateOf("") }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    // Reticle animation
    val infiniteTransition = rememberInfiniteTransition(label = "reticle_anim")
    val cornerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        label = "alpha",
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )

    // Handle success transition
    LaunchedEffect(uiState) {
        if (uiState is BandAssignmentViewModel.UiState.Assigned) {
            val s = uiState as BandAssignmentViewModel.UiState.Assigned
            onAssignedSuccess(s.worker.workerId, s.band.bandId)
        }
    }

    // Extract current worker context
    val currentWorker: WorkerEntity? = when (val s = uiState) {
        is BandAssignmentViewModel.UiState.ScanningBand -> s.worker
        is BandAssignmentViewModel.UiState.BandRejected -> s.worker
        is BandAssignmentViewModel.UiState.ConfirmPending -> s.worker
        is BandAssignmentViewModel.UiState.Assigning -> s.worker
        is BandAssignmentViewModel.UiState.Assigned -> s.worker
        is BandAssignmentViewModel.UiState.WorkerFound -> s.worker
        else -> null
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── 1. CameraX Preview with ML Kit Analyzer ──────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val barcodeScanner = BarcodeScanning.getClient()
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        var lastScannedTime = 0L

                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val now = System.currentTimeMillis()
                                        if (barcodes.isNotEmpty() && (now - lastScannedTime > 1500)) {
                                            val raw = barcodes.first().rawValue
                                            if (!raw.isNullOrBlank()) {
                                                lastScannedTime = now
                                                vm.onBandDecoded(raw)
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ScanBandQr", "Barcode scan failed", e)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = cam.cameraControl
                        } catch (e: Exception) {
                            Log.e("ScanBandQr", "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }
        )

        // ── 2. Dimmed Reticle Cutout Overlay ──────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val boxSize = canvasWidth * 0.72f
            val left = (canvasWidth - boxSize) / 2f
            val top = (canvasHeight - boxSize) / 2.3f

            // Dim outer area
            drawRect(
                color = Color.Black.copy(alpha = 0.65f),
                size = size
            )

            // Cut out center square
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // Green Reticle Corners (#10B981)
            val cornerLen = 32.dp.toPx()
            val strokeW = 4.5.dp.toPx()
            val cornerColor = StatusSafe.copy(alpha = cornerAlpha)

            // Top-Left
            drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
            drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)
            // Top-Right
            drawLine(cornerColor, Offset(left + boxSize, top), Offset(left + boxSize - cornerLen, top), strokeW)
            drawLine(cornerColor, Offset(left + boxSize, top), Offset(left + boxSize, top + cornerLen), strokeW)
            // Bottom-Left
            drawLine(cornerColor, Offset(left, top + boxSize), Offset(left + cornerLen, top + boxSize), strokeW)
            drawLine(cornerColor, Offset(left, top + boxSize), Offset(left, top + boxSize - cornerLen), strokeW)
            // Bottom-Right
            drawLine(cornerColor, Offset(left + boxSize, top + boxSize), Offset(left + boxSize - cornerLen, top + boxSize), strokeW)
            drawLine(cornerColor, Offset(left + boxSize, top + boxSize), Offset(left + boxSize, top + boxSize - cornerLen), strokeW)
        }

        // ── 3. Top Controls & Worker Context Header ───────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // Worker Context Pill
                if (currentWorker != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, StatusSafe)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = StatusSafe, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentWorker.name} (${currentWorker.employeeId})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Torch Toggle
                IconButton(
                    onClick = {
                        torchEnabled = !torchEnabled
                        cameraControl?.enableTorch(torchEnabled)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (torchEnabled) StatusModerate else Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flashlight",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = "Scan Dosimeter Band QR Code",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Align band QR inside the green frame",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── 4. Bottom Control Panel & Evaluator Chips ─────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(14.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1-TAP DEMO SIMULATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(
                        onClick = { showManualDialog = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enter Band ID", fontSize = 12.sp, color = NavyPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                // Preset 1: Fresh Available Band (Passes Validation -> Confirmation)
                Button(
                    onClick = { vm.onBandDecoded("DG:BAND:BAND-001285") },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSafe),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Available Band (BAND-001285)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text("Assign ➔", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                    }
                }

                // Preset 2: Already Assigned Band (Triggers Error Sheet)
                Button(
                    onClick = { vm.onBandDecoded("DG:BAND:WB-ASSIGNED-01") },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Block, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Already Assigned (WB-ASSIGNED-01)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text("Error Test ➔", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                    }
                }

                // Preset 3: Expired Band (Triggers Expiry Error Sheet)
                Button(
                    onClick = { vm.onBandDecoded("DG:BAND:WB-EXP-01") },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusModerate),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Expired Band (WB-EXP-01)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text("Expiry Test ➔", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }
        }
    }

    // ── 5. Validation Error Bottom Sheet Modal ────────────────────────────────
    if (uiState is BandAssignmentViewModel.UiState.BandRejected) {
        val rejectedState = uiState as BandAssignmentViewModel.UiState.BandRejected
        AlertDialog(
            onDismissRequest = { vm.retryScanBand() },
            containerColor = CardWhite,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = StatusCriticalBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Band Validation Failed", color = StatusCritical, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = rejectedState.details,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "Dosimeter badges must be active, calibrated, and unassigned before issuing to personnel.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.retryScanBand() },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Another Band", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.selectDifferentWorker(); onBack() }) {
                    Text("Cancel Assignment", color = TextSecondary)
                }
            }
        )
    }

    // ── 6. Confirm Assignment Dialog Modal ────────────────────────────────────
    if (uiState is BandAssignmentViewModel.UiState.ConfirmPending || uiState is BandAssignmentViewModel.UiState.Assigning) {
        val pendingWorker = (uiState as? BandAssignmentViewModel.UiState.ConfirmPending)?.worker
            ?: (uiState as? BandAssignmentViewModel.UiState.Assigning)?.worker
        val pendingBand = (uiState as? BandAssignmentViewModel.UiState.ConfirmPending)?.band
            ?: (uiState as? BandAssignmentViewModel.UiState.Assigning)?.band
        val isAssigning = uiState is BandAssignmentViewModel.UiState.Assigning

        if (pendingWorker != null && pendingBand != null) {
            AlertDialog(
                onDismissRequest = { if (!isAssigning) vm.retryScanBand() },
                containerColor = CardWhite,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = NavyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Band Assignment", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Verify worker and dosimeter details before finalizing the assignment.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        // Worker Section Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceBg,
                            border = BorderStroke(1.dp, CardStroke),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("ASSIGN TO WORKER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NavyPrimary, fontFamily = FontFamily.Monospace)
                                Text("${pendingWorker.name} (${pendingWorker.employeeId})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text("${pendingWorker.designation} • ${pendingWorker.department}", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        // Band Section Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BlueLight.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("DOSIMETER WRISTBAND", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BluePrimary, fontFamily = FontFamily.Monospace)
                                Text("Band ID: ${pendingBand.bandId}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                                Text("Batch: ${pendingBand.batchNo}  |  Status: Available", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    "Expiry: " + SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(pendingBand.expiryDate)),
                                    fontSize = 11.sp,
                                    color = StatusSafe,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { vm.confirmAssignment() },
                        enabled = !isAssigning,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        if (isAssigning) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Assigning…")
                        } else {
                            Text("Assign Band", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (!isAssigning) {
                        TextButton(onClick = { vm.retryScanBand() }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                }
            )
        }
    }

    // ── 7. Manual Band Entry Dialog ───────────────────────────────────────────
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            containerColor = CardWhite,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Band ID Manually", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Type the Band ID printed on the dosimeter wristband label.", fontSize = 13.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = manualBandInput,
                        onValueChange = { manualBandInput = it },
                        label = { Text("Band ID") },
                        placeholder = { Text("e.g. BAND-001285") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NavyPrimary,
                            unfocusedBorderColor = CardStroke
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualBandInput.isNotBlank()) {
                            showManualDialog = false
                            vm.onBandDecoded(manualBandInput)
                            manualBandInput = ""
                        }
                    },
                    enabled = manualBandInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Validate & Assign", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
