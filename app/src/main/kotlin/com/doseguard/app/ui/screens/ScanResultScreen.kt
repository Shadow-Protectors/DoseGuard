package com.doseguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doseguard.app.imageprocessing.ExposureEstimator
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.ScanViewModel

@Composable
fun ScanResultScreen(
    bandId: String,
    workerId: String,
    vm: ScanViewModel,
    onViewHistory: () -> Unit,
    onScanAgain: () -> Unit,
    onShowAlert: () -> Unit,
    onBack: () -> Unit
) {
    val result by vm.lastResult.collectAsState()
    val worker by vm.worker.collectAsState()
    val band   by vm.band.collectAsState()

    if (result == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NavyPrimary)
        }
        return
    }

    val r = result!!
    val rColor   = riskColor(r.riskLevel)
    val rBgColor = riskBgColor(r.riskLevel)
    val isCritical = r.riskLevel == "CRITICAL" || r.riskLevel == "HIGH"

    // Auto-navigate to alert if threshold breached
    LaunchedEffect(r.riskLevel) {
        if (isCritical) onShowAlert()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Result", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Risk banner ───────────────────────────────────────────────────
            Card(
                colors    = CardDefaults.cardColors(containerColor = rBgColor),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (r.riskLevel) {
                            "SAFE"     -> Icons.Default.CheckCircle
                            "MODERATE" -> Icons.Default.Warning
                            "HIGH"     -> Icons.Default.Error
                            else       -> Icons.Default.Dangerous
                        },
                        contentDescription = null,
                        tint     = rColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text  = r.riskLevel,
                        color = rColor,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text  = r.actionRequired,
                        color = rColor.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Key metrics grid ──────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label    = "Estimated Dose",
                    value    = "${r.estimatedDosePpmHr}",
                    unit     = "ppm·hr",
                    color    = rColor
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label    = "8-hr TWA",
                    value    = "${r.twa8hrPpm}",
                    unit     = "ppm",
                    color    = rColor
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label    = "Uncertainty",
                    value    = "±${r.uncertaintyPpmHr}",
                    unit     = "ppm·hr",
                    color    = TextSecondary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label    = "Confidence",
                    value    = "${(r.confidence * 100).toInt()}",
                    unit     = "%",
                    color    = TextSecondary
                )
            }

            // ── Debug / model info card ───────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Analysis Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    HorizontalDivider()
                    ResultRow("Raw ΔE", "${r.rawDeltaE}")
                    ResultRow("Band ID", bandId)
                    ResultRow("Worker", worker?.name ?: workerId)
                    if (r.debugInfo.isNotBlank()) {
                        ResultRow("Debug", r.debugInfo)
                    }
                    // Mark where AI model output will appear
                    Text(
                        "// [AI_INTEGRATION_POINT]: TFLite model output will replace rule-based ΔE here",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // ── PEL reference card ────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("OSHA H₂S Exposure Limits", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text("PEL: 10 ppm (8-hr TWA)  |  STEL: 15 ppm  |  IDLH: 50 ppm",
                        style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick  = onScanAgain,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Again", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick  = onViewHistory,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("View History", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(modifier: Modifier = Modifier, label: String, value: String, unit: String, color: Color) {
    Card(
        modifier  = modifier,
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier  = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = color)
            Text(unit,  style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
