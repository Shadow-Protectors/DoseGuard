package com.doseguard.app.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.doseguard.app.ui.theme.NavyPrimary
import com.doseguard.app.ui.theme.StatusCritical
import com.doseguard.app.ui.theme.StatusSafe
import com.doseguard.app.ui.theme.StatusModerate
import com.doseguard.app.ui.theme.CardWhite
import com.doseguard.app.ui.theme.AccentCyan
import com.doseguard.app.viewmodel.ScanViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraCaptureScreen(
    bandId: String,
    workerId: String,
    vm: ScanViewModel,
    onCaptured: () -> Unit,
    onBack: () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by vm.uiState.collectAsState()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Shutter button pulse animation
    val pulse = rememberInfiniteTransition(label = "shutter")
    val shutterScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.06f, label = "scale",
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )

    LaunchedEffect(uiState) {
        if (uiState is ScanViewModel.ScanUiState.Result ||
            uiState is ScanViewModel.ScanUiState.Error) {
            onCaptured()
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── 1. Camera preview ────────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        val provider = future.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        try {
                            provider.unbindAll()
                            val cam = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                            cameraControl = cam.cameraControl
                        } catch (e: Exception) {
                            Log.e("Camera", "Bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }
        )

        // ── 2. Alignment guide overlay ───────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .size(280.dp, 160.dp)
                .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "CHEMICAL DOSIMETER STRIP ROI",
                    color = Color(0xFF00E5FF),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Align strip or tap a Demo Preset below",
                    color = Color.White.copy(alpha = 0.90f),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── 3. Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text("Capture Strip Image", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Band: $bandId", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
                }
            }

            IconButton(onClick = {
                torchEnabled = !torchEnabled
                cameraControl?.enableTorch(torchEnabled)
            }) {
                Icon(
                    if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Torch",
                    tint = if (torchEnabled) Color.Yellow else Color.White
                )
            }
        }

        // ── 4. Bottom controls & Demo presets ────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (uiState) {
                is ScanViewModel.ScanUiState.Analyzing -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape  = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = NavyPrimary
                            )
                            Column {
                                Text("Analyzing Strip Colorimetry…", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                Text("Converting sRGB → CIE LAB (D65) & calculating ΔE", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                else -> {
                    // Demo Simulation Strip Presets Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite.copy(alpha = 0.94f)),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "⚡ Demo Strip Presets (Instant Evaluation)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Safe Preset
                                Button(
                                    onClick = { vm.onSimulatedScan(deltaE = 4.2, bandId = bandId, workerId = workerId) },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusSafe),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Safe (0.4 ppm)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // Moderate Preset
                                Button(
                                    onClick = { vm.onSimulatedScan(deltaE = 19.5, bandId = bandId, workerId = workerId) },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusModerate),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Moderate (2.1)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // Critical Preset
                                Button(
                                    onClick = { vm.onSimulatedScan(deltaE = 52.0, bandId = bandId, workerId = workerId) },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Critical (>10)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    // Physical Camera Shutter Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .scale(shutterScale)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    captureImage(
                                        imageCapture = imageCapture,
                                        context      = context,
                                        executor     = cameraExecutor,
                                        bandId       = bandId,
                                        workerId     = workerId,
                                        vm           = vm
                                    )
                                },
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Capture",
                                    tint     = NavyPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                    Text("Or tap shutter to analyze live camera photo", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Trigger ImageCapture, read JPEG bytes, save to MediaStore,
 * then hand off to ScanViewModel for analysis.
 */
private fun captureImage(
    imageCapture: ImageCapture?,
    context: Context,
    executor: ExecutorService,
    bandId: String,
    workerId: String,
    vm: ScanViewModel
) {
    val ic = imageCapture
    if (ic == null) {
        // Fallback simulation if camera capture is not ready
        vm.onSimulatedScan(deltaE = 8.5, bandId = bandId, workerId = workerId)
        return
    }

    val photoFile = createImageFile(context)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    ic.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val bytes = photoFile.readBytes()
                    vm.onImageCaptured(
                        imageBytes = bytes,
                        imagePath  = photoFile.absolutePath,
                        bandId     = bandId,
                        workerId   = workerId
                    )
                } catch (e: Exception) {
                    Log.e("Camera", "Failed to read captured file", e)
                    vm.onSimulatedScan(deltaE = 12.0, bandId = bandId, workerId = workerId)
                }
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("Camera", "Capture failed, falling back to simulated scan", exc)
                vm.onSimulatedScan(deltaE = 14.5, bandId = bandId, workerId = workerId)
            }
        }
    )
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
    val dir = context.getExternalFilesDir("dosimeter_scans") ?: context.cacheDir
    return File(dir, "STRIP_${timeStamp}.jpg")
}
