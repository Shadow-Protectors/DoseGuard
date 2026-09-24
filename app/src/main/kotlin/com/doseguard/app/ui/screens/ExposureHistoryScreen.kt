package com.doseguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doseguard.app.model.ExposureHistoryEntity
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposureHistoryScreen(
    workerId: String,
    bandId: String = "",
    vm: HistoryViewModel = viewModel(),
    onBack: () -> Unit
) {
    LaunchedEffect(workerId, bandId) {
        vm.load(workerId, bandId)
    }

    val history   by vm.historyFlow.collectAsState()
    val trendData by vm.trendFlow.collectAsState()
    val worker    by vm.worker.collectAsState()
    val band      by vm.band.collectAsState()
    val timeRange by vm.selectedTimeRange.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exposure History & Trends", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. Worker summary header ─────────────────────────────────────
            item {
                worker?.let { w ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                        shape  = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(w.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${w.department} • ${w.shift} Shift", color = Color.White.copy(alpha = 0.80f), style = MaterialTheme.typography.bodyMedium)
                                band?.let { b ->
                                    Text("Active Band: ${b.bandId} (${b.bandStatus})", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // ── 2. Time Range Selector (7-Day vs 30-Day Monthly vs All) ─────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HistoryViewModel.TimeRange.values().forEach { range ->
                            val isSelected = timeRange == range
                            Button(
                                onClick = { vm.setTimeRange(range) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                                    contentColor = if (isSelected) Color.White else Color(0xFF475569)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(range.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            // ── 3. Exposure Trend Chart ──────────────────────────────────────
            item {
                if (trendData.isNotEmpty()) {
                    TrendBarChart(trendData, timeRange.label)
                }
            }

            // ── 4. Stats Summary Grid ────────────────────────────────────────
            item {
                if (history.isNotEmpty()) {
                    val totalScans = history.size
                    val totalDose  = history.sumOf { it.estimatedDose }
                    val critCount  = history.count { it.riskLevel == "CRITICAL" || it.riskLevel == "HIGH" }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(modifier = Modifier.weight(1f), label = "Total Scans", value = "$totalScans", icon = Icons.Default.QrCodeScanner, color = NavyPrimary)
                        StatTile(modifier = Modifier.weight(1f), label = "Lifetime Dose", value = "%.2f ppm·hr".format(totalDose), icon = Icons.Default.Assessment, color = AccentCyan)
                        StatTile(modifier = Modifier.weight(1f), label = "Alert Events", value = "$critCount", icon = Icons.Default.Warning, color = if (critCount > 0) StatusCritical else StatusSafe)
                    }
                }
            }

            // ── 5. Chronological History Logs ────────────────────────────────
            item {
                Text(
                    "Shift Exposure Records (${history.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (history.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No exposure records yet for this worker.", color = TextSecondary)
                        }
                    }
                }
            } else {
                items(history) { record ->
                    HistoryRecordCard(record)
                }
            }
        }
    }
}

@Composable
private fun TrendBarChart(data: List<ExposureHistoryEntity>, title: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$title Exposure Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("OSHA PEL = 10 ppm", style = MaterialTheme.typography.labelSmall, color = StatusCritical, fontWeight = FontWeight.Bold)
            }

            val maxVal = maxOf(4.0, data.maxOfOrNull { it.estimatedDose } ?: 4.0)

            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val displayItems = data.takeLast(14)
                displayItems.forEach { item ->
                    val fraction = (item.estimatedDose / maxVal).toFloat().coerceIn(0.05f, 1f)
                    val barColor = riskColor(item.riskLevel)
                    val dateLabel = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(item.scanTime))

                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text("%.1f".format(item.estimatedDose), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = barColor)
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(dateLabel, fontSize = 8.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier = Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun HistoryRecordCard(record: ExposureHistoryEntity) {
    val rColor = riskColor(record.riskLevel)
    val rBgColor = riskBgColor(record.riskLevel)
    val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(record.scanTime))

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(rBgColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(record.riskLevel, color = rColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Band: ${record.bandId}", fontSize = 12.sp, color = TextMuted)
                }
                Text(dateFmt, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("%.2f ppm·hr".format(record.estimatedDose), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = rColor)
                Text("${(record.confidence * 100).toInt()}% conf", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}
