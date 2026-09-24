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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doseguard.app.ui.theme.NavyPrimary
import com.doseguard.app.ui.theme.StatusSafe
import com.doseguard.app.ui.theme.AccentCyan
import com.doseguard.app.viewmodel.QrScanViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScanScreen(
    vm: QrScanViewModel = viewModel(),
    onBandAssigned: (bandId: String, workerId: String) -> Unit,
    onNewBand: (bandId: String, qrData: String) -> Unit,
    onBack: () -> Unit
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by vm.state.collectAsState()

    // Animated corner pulse for the reticle
    val pulse = rememberInfiniteTransition(label = "pulse")
    val cornerAlpha by pulse.animateFloat(
        initialValue = 0.5f, targetValue = 1f, label = "alpha",
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
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Live camera preview ───────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val preview  = Preview.Builder().build().also {
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
                                            mediaImage, proxy.imageInfo.rotationDegrees
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

        // ── Dark overlay with transparent reticle ─────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val reticleSize = size.width * 0.65f
            val left  = (size.width  - reticleSize) / 2f
            val top   = (size.height - reticleSize) / 2f

            // Semi-transparent overlay
            drawRect(Color.Black.copy(alpha = 0.55f))

            // Punch transparent hole for the reticle
            drawRoundRect(
                color        = Color.Transparent,
                topLeft      = Offset(left, top),
                size         = Size(reticleSize, reticleSize),
                cornerRadius = CornerRadius(16.dp.toPx()),
                blendMode    = BlendMode.Clear
            )
        }

        // ── Animated corner brackets ──────────────────────────────────────────
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val size = w * 0.65f
            val left = (w - size) / 2f
            val top  = (h - size) / 2f
            val arm  = size * 0.12f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val color  = Color(0xFF00E5FF).copy(alpha = cornerAlpha)
                val stroke = Stroke(width = 4.dp.toPx())
                val r      = 16.dp.toPx()

                // Top-left
                drawLine(color, Offset(left + r, top), Offset(left + arm, top), stroke.width)
                drawLine(color, Offset(left, top + r), Offset(left, top + arm), stroke.width)
                // Top-right
                drawLine(color, Offset(left + size - arm, top), Offset(left + size - r, top), stroke.width)
                drawLine(color, Offset(left + size, top + r), Offset(left + size, top + arm), stroke.width)
                // Bottom-left
                drawLine(color, Offset(left + r, top + size), Offset(left + arm, top + size), stroke.width)
                drawLine(color, Offset(left, top + size - arm), Offset(left, top + size - r), stroke.width)
                // Bottom-right
                drawLine(color, Offset(left + size - arm, top + size), Offset(left + size - r, top + size), stroke.width)
                drawLine(color, Offset(left + size, top + size - arm), Offset(left + size, top + size - r), stroke.width)
            }
        }

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text  = "Scan Wristband QR Code",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // ── Bottom status card ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val s = state) {
                is QrScanViewModel.ScanState.Processing -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape  = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = NavyPrimary
                            )
                            Text("Looking up band…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is QrScanViewModel.ScanState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape  = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Error: ${s.message}",
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFEF4444)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.reset() }) { Text("Try Again") }
                }
                else -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                        shape  = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Point camera at the QR code\non the wristband",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }
}
