package com.doseguard.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doseguard.app.imageprocessing.ExposureEstimator
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dosimeter Scan Result", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
                    }
                }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. Risk banner ───────────────────────────────────────────────
            Card(
                colors    = CardDefaults.cardColors(containerColor = rBgColor),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
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
                        text  = "${r.riskLevel} STATUS",
                        color = rColor,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text  = r.actionRequired,
                        color = rColor.copy(alpha = 0.90f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── 2. Alert Trigger Action if Critical / High ───────────────────
            if (isCritical) {
                Button(
                    onClick = onShowAlert,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                ) {
                    Icon(Icons.Default.Emergency, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("View Safety Alert & Emergency SOP", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // ── 3. Key metrics grid ──────────────────────────────────────────
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
                    label    = "Uncertainty (σ)",
                    value    = "±${r.uncertaintyPpmHr}",
                    unit     = "ppm·hr",
                    color    = TextSecondary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label    = "Confidence",
                    value    = "${(r.confidence * 100).toInt()}%",
                    unit     = "Model certainty",
                    color    = TextSecondary
                )
            }

            // ── 4. Analysis details ──────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape  = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chemical & Optical Model Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    HorizontalDivider()
                    ResultRow("CIE ΔE (vs Pristine Cream)", "${r.rawDeltaE}")
                    ResultRow("Band ID", bandId)
                    ResultRow("Worker", worker?.name ?: workerId)
                    ResultRow("Calculation Model", "Kinetic Saturation Model (1st Order)")
                    if (r.debugInfo.isNotBlank()) {
                        ResultRow("Model Debug", r.debugInfo)
                    }
                }
            }

            // ── 5. Statutory OSHA Reference ──────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("OSHA / DGMS Statutory Exposure Limits", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text("• Action Level: 1.0 ppm TWA  |  PEL Limit: 10.0 ppm TWA\n• STEL Limit: 15.0 ppm  |  IDLH Evacuation: 50.0 ppm",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            // ── 6. Navigation Actions ────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick  = onScanAgain,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Again", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick  = onViewHistory,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color.White)
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
            Text(unit,  style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
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
