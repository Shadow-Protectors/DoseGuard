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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doseguard.app.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Info", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
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
            // ── App Info ──────────────────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = NavyPrimary), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DoseGuard v2.0", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("H₂S Passive Dosimeter System", color = Color.White.copy(alpha = 0.80f), style = MaterialTheme.typography.bodyMedium)
                    Text("SIH 2026 • Problem Statement 26118", color = Color.White.copy(alpha = 0.60f), style = MaterialTheme.typography.labelLarge)
                }
            }

            // ── Exposure Limits Reference ─────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("H₂S Regulatory Limits (OSHA / NIOSH)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    HorizontalDivider()
                    LimitRow("PEL (OSHA)",      "10 ppm",  "8-hr TWA",           StatusSafe)
                    LimitRow("Action Level",     "1 ppm",   "8-hr TWA",           StatusModerate)
                    LimitRow("STEL",             "15 ppm",  "15-min ceiling",      StatusHigh)
                    LimitRow("IDLH (NIOSH)",     "50 ppm",  "Immediately Dangerous", StatusCritical)
                    LimitRow("Olfactory Loss",   "100 ppm", "Immediate",           StatusCritical)
                }
            }

            // ── Exposure Estimator Info ───────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estimation Model", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    HorizontalDivider()
                    InfoItem(Icons.Default.Biotech, "Algorithm",
                        "Kinetic saturation model: D = -(1/k)·ln(1−ΔE/ΔEmax)")
                    InfoItem(Icons.Default.Palette, "Color Space",
                        "CIE LAB (D65 illuminant) — ΔE against pristine cream reference")
                    InfoItem(Icons.Default.Speed, "Rate Constant k",
                        "0.028 (calibrated from bench experiments)")
                    InfoItem(Icons.Default.Info, "Uncertainty",
                        "Gaussian error propagation: σ_D = σ_ΔE / (k·(ΔEmax − ΔE))")
                    InfoItem(Icons.Default.Lightbulb, "AI Integration",
                        "[FUTURE] TFLite CNN model on calibrated strip images will replace rule-based ΔE")
                }
            }

            // ── Demo Presets ──────────────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Demo Simulation Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    HorizontalDivider()
                    InfoItem(Icons.Default.CheckCircle,  "SAFE",     "ΔE = 4.2  →  ~0.6 ppm·hr  |  TWA < 1 ppm",    StatusSafe)
                    InfoItem(Icons.Default.Warning,      "MODERATE", "ΔE = 18.5 →  ~11 ppm·hr   |  TWA 1–2.5 ppm",  StatusModerate)
                    InfoItem(Icons.Default.Error,        "HIGH",     "ΔE = 35.0 →  ~27 ppm·hr   |  TWA 2.5–10 ppm", StatusHigh)
                    InfoItem(Icons.Default.Dangerous,    "CRITICAL", "ΔE = 48.0 →  ~50 ppm·hr   |  TWA > 10 ppm",   StatusCritical)
                }
            }

            // ── Tech Stack ────────────────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Technology Stack", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    HorizontalDivider()
                    InfoItem(Icons.Default.PhoneAndroid, "UI",       "Jetpack Compose + Material 3")
                    InfoItem(Icons.Default.Storage,      "Database", "Room (SQLite) — 4 entities, offline-first")
                    InfoItem(Icons.Default.Camera,       "Camera",   "CameraX ImageCapture + ML Kit Barcode")
                    InfoItem(Icons.Default.AccountTree, "Pattern",  "MVVM + Repository + StateFlow")
                    InfoItem(Icons.Default.Block,        "Backend",  "None — fully offline demo")
                }
            }
        }
    }
}

@Composable
private fun LimitRow(label: String, value: String, note: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Text(note,  style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
            Text(value, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun InfoItem(icon: ImageVector, label: String, value: String, iconTint: Color = NavyPrimary) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}
