package com.doseguard.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.doseguard.app.ui.theme.NavyPrimary
import com.doseguard.app.ui.theme.StatusSafe
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * Reusable full-screen camera scanner with a Material 3 industrial overlay:
 * dimmed scrim, square cut-out reticle and pulsing green corner guides.
 *
 * Handles the runtime CAMERA permission itself and exposes a manual-entry
 * fallback so the operator is never blocked if permission is denied.
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
fun QrScannerCamera(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCodeScanned: (String) -> Unit,
    onPermissionDenied: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    // Latest callback, so the analyzer never captures a stale lambda.
    val currentOnScanned by rememberUpdatedState(onCodeScanned)
    val scanEnabled by rememberUpdatedState(enabled)

    Box(modifier = modifier.background(Color.Black)) {

        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val future = ProcessCameraProvider.getInstance(ctx)
                        future.addListener({
                            val provider = future.get()
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
                                        if (mediaImage == null || !scanEnabled) {
                                            proxy.close()
                                            return@setAnalyzer
                                        }
                                        val img = InputImage.fromMediaImage(
                                            mediaImage, proxy.imageInfo.rotationDegrees
                                        )
                                        scanner.process(img)
                                            .addOnSuccessListener { barcodes ->
                                                barcodes.firstOrNull { it.rawValue != null }
                                                    ?.rawValue
                                                    ?.let { raw -> if (scanEnabled) currentOnScanned(raw) }
                                            }
                                            .addOnCompleteListener { proxy.close() }
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
                                Log.e("QrScannerCamera", "Camera bind failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                }
            )
            ScannerReticle(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Camera permission is required to scan codes.",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) { Text("Grant Permission") }
                if (onPermissionDenied != null) {
                    Spacer(Modifier.height(12.dp))
                    onPermissionDenied()
                }
            }
        }
    }
}

/** Dimmed scrim with a transparent square window and pulsing green corner guides. */
@Composable
fun ScannerReticle(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "reticle")
    val alpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "cornerAlpha"
    )
    val sweep by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweep"
    )

    Canvas(modifier = modifier) {
        val side = minOf(size.width * 0.72f, size.height * 0.46f)
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val radius = 20.dp.toPx()
        val arm = side * 0.18f
        val strokeW = 5.dp.toPx()

        drawRect(Color.Black.copy(alpha = 0.55f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = CornerRadius(radius, radius),
            blendMode = BlendMode.Clear
        )

        val green = StatusSafe.copy(alpha = alpha)
        // Top-left
        drawLine(green, Offset(left + radius, top), Offset(left + arm, top), strokeW)
        drawLine(green, Offset(left, top + radius), Offset(left, top + arm), strokeW)
        // Top-right
        drawLine(green, Offset(left + side - arm, top), Offset(left + side - radius, top), strokeW)
        drawLine(green, Offset(left + side, top + radius), Offset(left + side, top + arm), strokeW)
        // Bottom-left
        drawLine(green, Offset(left + radius, top + side), Offset(left + arm, top + side), strokeW)
        drawLine(green, Offset(left, top + side - radius), Offset(left, top + side - arm), strokeW)
        // Bottom-right
        drawLine(green, Offset(left + side - arm, top + side), Offset(left + side - radius, top + side), strokeW)
        drawLine(green, Offset(left + side, top + side - radius), Offset(left + side, top + side - arm), strokeW)

        // Sweep line inside the window
        val y = top + side * sweep
        drawLine(
            color = StatusSafe.copy(alpha = 0.6f),
            start = Offset(left + 8.dp.toPx(), y),
            end = Offset(left + side - 8.dp.toPx(), y),
            strokeWidth = 2.dp.toPx()
        )
    }
}
