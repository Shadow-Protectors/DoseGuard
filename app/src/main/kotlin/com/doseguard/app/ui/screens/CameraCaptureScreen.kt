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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.doseguard.app.ui.theme.NavyPrimary
import com.doseguard.app.ui.theme.StatusSafe
import com.doseguard.app.ui.theme.CardWhite
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

    // Hold a reference to ImageCapture so the shutter button can trigger it
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Shutter button pulse animation
    val pulse = rememberInfiniteTransition(label = "shutter")
    val shutterScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.08f, label = "scale",
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

        // ── Camera preview ────────────────────────────────────────────────────
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
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            Log.e("Camera", "Bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }
        )

        // ── Alignment guide overlay ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp, 160.dp)
                .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
        ) {
            Text(
                "Align the dosimeter strip here",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
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
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text("Capture Strip Image", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("Band: $bandId", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
            }
        }

        // ── Bottom controls ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (uiState) {
                is ScanViewModel.ScanUiState.Analyzing -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape  = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp,
                                color = NavyPrimary
                            )
                            Text("Analyzing strip color…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                is ScanViewModel.ScanUiState.Idle -> {
                    // Instructions card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Position the strip in the frame.\nEnsure good lighting for accurate results.",
                            modifier  = Modifier.padding(12.dp),
                            color     = Color.White,
                            textAlign = TextAlign.Center,
                            style     = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Shutter button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
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
                            modifier = Modifier.size(76.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint     = NavyPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Text("Tap to capture", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                }

                else -> {}
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
    val ic = imageCapture ?: return

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        createImageFile(context)
    ).build()

    ic.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val uri = output.savedUri ?: return
                try {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    vm.onImageCaptured(
                        imageBytes = bytes,
                        imagePath  = uri.toString(),
                        bandId     = bandId,
                        workerId   = workerId
                    )
                } catch (e: Exception) {
                    Log.e("Camera", "Failed to read captured image", e)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "Capture error: ${exception.message}", exception)
            }
        }
    )
}

private fun createImageFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
    val storageDir = context.getExternalFilesDir("DoseGuard_Images")
        ?: context.filesDir
    return File(storageDir, "STRIP_$timestamp.jpg")
}
