package com.doseguard.app.ui.screens

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.QrScanViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@androidx.camera.core.ExperimentalGetImage
@Composable
fun QrScanScreen(
    vm: QrScanViewModel = viewModel(),
    onBandAssigned: (bandId: String, workerId: String) -> Unit,
    onNewBand: (bandId: String, qrData: String) -> Unit,
    onBandInvalid: (bandId: String, reason: String, workerId: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by vm.state.collectAsState()

    var showManualDialog by remember { mutableStateOf(false) }
    var manualBandInput by remember { mutableStateOf("") }

    // Animated corner pulse for the reticle
    val pulse = rememberInfiniteTransition(label = "pulse")
    val cornerAlpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        label = "alpha",
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    )

    // Handle navigation side-effects
    LaunchedEffect(state) {
        when (val s = state) {
            is QrScanViewModel.ScanState.BandAssigned -> {
                onBandAssigned(s.band.bandId, s.worker.workerId)
                vm.reset()
            }
            is QrScanViewModel.ScanState.BandUnassigned -> {
                onNewBand(s.band.bandId, s.band.qrData)
                vm.reset()
            }
            is QrScanViewModel.ScanState.NewBand -> {
                onNewBand(s.bandId, s.qrData)
                vm.reset()
            }
            is QrScanViewModel.ScanState.BandInvalid -> {
                onBandInvalid(s.bandId, s.reason, s.workerId)
                vm.reset()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── 1. Live camera preview ───────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val scanner = BarcodeScanning.getClient()
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { ia ->
                                ia.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                                    val mediaImage = proxy.image
                                    if (mediaImage != null) {
                                        val img = InputImage.fromMediaImage(
                                            mediaImage,
                                            proxy.imageInfo.rotationDegrees
                                        )
                                        scanner.process(img)
                                            .addOnSuccessListener { barcodes ->
                                                barcodes.firstOrNull { it.rawValue != null }?.let { bc ->
                                                    bc.rawValue?.let { raw ->
                                                        vm.onQrScanned(raw)
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener { proxy.close() }
                                    } else {
                                        proxy.close()
                                    }
                                }
                            }

                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        } catch (e: Exception) {
                            Log.e("QrScan", "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }
        )

        // ── 2. Dark overlay with transparent reticle ─────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val reticleSize = size.width * 0.52f
            val left = (size.width - reticleSize) / 2f
            val top = size.height * 0.12f

            drawRect(Color.Black.copy(alpha = 0.50f))

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(reticleSize, reticleSize),
                cornerRadius = CornerRadius(16.dp.toPx()),
                blendMode = BlendMode.Clear
            )
        }

        // ── 3. Animated corner brackets ──────────────────────────────────────
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val reticleSize = w * 0.52f
            val left = (w - reticleSize) / 2f
            val top = h * 0.12f
            val arm = reticleSize * 0.15f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val color = Color(0xFF00E5FF).copy(alpha = cornerAlpha)
                val stroke = Stroke(width = 4.dp.toPx())
                val r = 16.dp.toPx()

                // Top-left
                drawLine(color, Offset(left + r, top), Offset(left + arm, top), stroke.width)
                drawLine(color, Offset(left, top + r), Offset(left, top + arm), stroke.width)
                // Top-right
                drawLine(color, Offset(left + reticleSize - arm, top), Offset(left + reticleSize - r, top), stroke.width)
                drawLine(color, Offset(left + reticleSize, top + r), Offset(left + reticleSize, top + arm), stroke.width)
                // Bottom-left
                drawLine(color, Offset(left + r, top + reticleSize), Offset(left + arm, top + reticleSize), stroke.width)
                drawLine(color, Offset(left, top + reticleSize - arm), Offset(left, top + reticleSize - r), stroke.width)
                // Bottom-right
                drawLine(color, Offset(left + reticleSize - arm, top + reticleSize), Offset(left + reticleSize - r, top + reticleSize), stroke.width)
                drawLine(color, Offset(left + reticleSize, top + reticleSize - arm), Offset(left + reticleSize, top + reticleSize - r), stroke.width)
            }
        }

        // ── 4. Top App Bar ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = "Scan Wristband QR",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Statutory Validity Gate Active",
                        color = Color(0xFF00E5FF),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(onClick = { showManualDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Manual Entry", tint = Color(0xFF00E5FF))
            }
        }

        // ── 5. Bottom Interactive Demo & Validity Gate Panel ─────────────────
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (val s = state) {
                    is QrScanViewModel.ScanState.Processing -> {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = NavyPrimary
                            )
                            Text(
                                "Evaluating band validity & lifetime dose…",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyPrimary
                            )
                        }
                    }

                    is QrScanViewModel.ScanState.Error -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = StatusCritical, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                s.message,
                                color = StatusCritical,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { vm.reset() }) {
                                Text("Retry", color = StatusCritical, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.PlayCircle, null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                Text(
                                    "Demo Simulation Presets",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }
                            Text(
                                "Tap to evaluate gate",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Preset 1: Valid Active Band (Rajesh Kumar)
                        Button(
                            onClick = { vm.onQrScanned("DG:BAND:WB-1001") },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text("Valid Band (WB-1001)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("Active Profile ➔", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }

                        // Preset 2: Expired Shelf-Life Band (Triggers Gate Block)
                        Button(
                            onClick = { vm.onQrScanned("DG:BAND:WB-EXP-01") },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Block, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text("Simulate EXPIRED Band (WB-EXP-01)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("Gate Test ➔", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }

                        // Preset 3: Saturated Band (50 ppm·hr Maximum Capacity)
                        Button(
                            onClick = { vm.onQrScanned("DG:BAND:WB-SAT-99") },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusModerate)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text("Simulate SATURATED Band (WB-SAT-99)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("Gate Test ➔", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }

                        // Preset 4: Unassigned Band -> Registration / Replacement
                        OutlinedButton(
                            onClick = { vm.onQrScanned("DG:BAND:WB-9042") },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.PersonAdd, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                    Text("New Band -> Register / Replace (WB-9042)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Text("Assign ➔", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // ── 6. Manual Entry Dialog ───────────────────────────────────────────
        if (showManualDialog) {
            AlertDialog(
                onDismissRequest = { showManualDialog = false },
                title = { Text("Manual Wristband ID", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Enter the Band ID printed on the wristband:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = manualBandInput,
                            onValueChange = { manualBandInput = it },
                            placeholder = { Text("e.g. WB-1001, WB-EXP-01, WB-9042") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (manualBandInput.isNotBlank()) {
                                val input = manualBandInput.trim()
                                showManualDialog = false
                                vm.onQrScanned("DG:BAND:$input")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text("Lookup Band")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
