package com.doseguard.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doseguard.app.model.AlertEntity
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.AlertViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertScreen(
    workerId: String,
    bandId: String,
    vm: AlertViewModel = viewModel(),
    onBack: () -> Unit,
    onViewHistory: () -> Unit
) {
    val alerts by vm.activeAlerts.collectAsState()
    val count  by vm.activeCount.collectAsState()

    // Pulsing warning animation
    val pulse = rememberInfiniteTransition(label = "alert_pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.12f, label = "scale",
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚠ Active Alerts", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StatusCritical,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    if (count > 0) {
                        TextButton(onClick = { vm.acknowledgeAllForWorker(workerId) }) {
                            Text("Ack All", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF5F5))
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Critical warning banner ───────────────────────────────────────
            item {
                Card(
                    colors    = CardDefaults.cardColors(containerColor = StatusCritical),
                    shape     = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier  = Modifier.scale(scale)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Dangerous,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            "EXPOSURE THRESHOLD EXCEEDED",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Immediate action required. Refer to safety officer.",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$count active alert${if (count != 1) "s" else ""}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── OSHA reference ────────────────────────────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("H₂S Safety Thresholds", fontWeight = FontWeight.Bold, color = StatusCritical, style = MaterialTheme.typography.titleMedium)
                        ThresholdRow("Action Level",  "1 ppm TWA",  "Inspect equipment, rotate worker soon")
                        ThresholdRow("High Risk",     "2.5 ppm TWA","Increase ventilation, supervisor alert")
                        ThresholdRow("PEL (OSHA)",    "10 ppm TWA", "Evacuate area immediately")
                        ThresholdRow("IDLH (NIOSH)",  "50 ppm",     "Immediately dangerous to life/health")
                    }
                }
            }

            // ── Alert list ────────────────────────────────────────────────────
            if (alerts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = StatusSafe, modifier = Modifier.size(48.dp))
                            Text("No active alerts", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                        }
                    }
                }
            } else {
                item {
                    Text("Active Alert Records", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                items(alerts) { alert ->
                    AlertRecordCard(alert = alert, onAcknowledge = { vm.acknowledge(alert.alertId) }, onResolve = { vm.resolve(alert.alertId) })
                }
            }

            // ── Navigation ────────────────────────────────────────────────────
            item {
                OutlinedButton(
                    onClick  = onViewHistory,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("View Full Exposure History", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AlertRecordCard(
    alert: AlertEntity,
    onAcknowledge: () -> Unit,
    onResolve: () -> Unit
) {
    val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val isHigh  = alert.riskLevel == "HIGH"
    val color   = if (isHigh) StatusHigh else StatusCritical
    val bgColor = if (isHigh) StatusHighBg else StatusCriticalBg

    Card(
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (isHigh) Icons.Default.Error else Icons.Default.Dangerous,
                    contentDescription = null,
                    tint = color
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${alert.riskLevel} — ${alert.triggerDose} ppm·hr",
                        fontWeight = FontWeight.Bold,
                        color = color,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        dateFmt.format(Date(alert.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                // Status chip
                Surface(
                    color = color,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        alert.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (alert.status == "ACTIVE") {
                    TextButton(onClick = onAcknowledge) {
                        Text("Acknowledge", color = color, fontWeight = FontWeight.Bold)
                    }
                }
                if (alert.status != "RESOLVED") {
                    TextButton(onClick = onResolve) {
                        Text("Mark Resolved", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThresholdRow(level: String, threshold: String, action: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text("• $level:", fontWeight = FontWeight.Medium, color = StatusCritical, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(100.dp))
        Column {
            Text(threshold, fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
            Text(action, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}
