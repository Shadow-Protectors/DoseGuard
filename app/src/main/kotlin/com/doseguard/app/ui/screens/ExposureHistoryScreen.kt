package com.doseguard.app.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doseguard.app.model.ExposureHistoryEntity
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExposureHistoryScreen(
    workerId: String,
    bandId: String,
    vm: HistoryViewModel = viewModel(),
    onBack: () -> Unit
) {
    LaunchedEffect(workerId, bandId) {
        vm.load(workerId, bandId)
    }

    val history by vm.historyFlow.collectAsState()
    val weekly  by vm.weeklyFlow.collectAsState()
    val worker  by vm.worker.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exposure History", fontWeight = FontWeight.SemiBold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Worker summary header ─────────────────────────────────────────
            item {
                worker?.let { w ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                        shape  = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Column {
                                Text(w.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${w.department} • ${w.shift} Shift", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // ── 7-day trend bar chart ─────────────────────────────────────────
            item {
                if (weekly.isNotEmpty()) {
                    WeeklyTrendChart(weekly)
                }
            }

            // ── Stats summary ─────────────────────────────────────────────────
            item {
                if (history.isNotEmpty()) {
                    val total     = history.size
                    val totalDose = history.sumOf { it.estimatedDose }
                    val avgConf   = history.map { it.confidence }.average()
                    val critCount = history.count { it.riskLevel == "CRITICAL" || it.riskLevel == "HIGH" }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatChip(modifier = Modifier.weight(1f), label = "Total Scans", value = "$total",    color = NavyPrimary)
                        StatChip(modifier = Modifier.weight(1f), label = "Total Dose",  value = "%.1f".format(totalDose), color = riskColor(if (totalDose > 20) "HIGH" else "SAFE"))
                        StatChip(modifier = Modifier.weight(1f), label = "Alerts",      value = "$critCount", color = if (critCount > 0) StatusCritical else StatusSafe)
                    }
                }
            }

            // ── Section title ─────────────────────────────────────────────────
            item {
                Text(
                    "All Scan Records",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // ── History list ──────────────────────────────────────────────────
            if (history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.SearchOff, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Text("No scan records yet", style = MaterialTheme.typography.bodyLarge, color = TextMuted)
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
private fun WeeklyTrendChart(data: List<ExposureHistoryEntity>) {
    val maxDose = data.maxOfOrNull { it.estimatedDose }?.coerceAtLeast(1.0) ?: 1.0
    val dayFmt  = SimpleDateFormat("EEE", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape  = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("7-Day Exposure Trend (ppm·hr)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

            // Simple bar chart using Box height
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { entry ->
                    val fraction = (entry.estimatedDose / maxDose).toFloat().coerceIn(0.02f, 1f)
                    val color    = riskColor(entry.riskLevel)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(color)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            dayFmt.format(Date(entry.scanTime)),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("SAFE" to StatusSafe, "MODERATE" to StatusModerate, "HIGH" to StatusHigh, "CRITICAL" to StatusCritical).forEach { (label, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(record: ExposureHistoryEntity) {
    val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val rColor  = riskColor(record.riskLevel)
    val rBg     = riskBgColor(record.riskLevel)

    Card(
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Risk indicator dot
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(rBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (record.riskLevel) {
                        "SAFE"     -> Icons.Default.CheckCircle
                        "MODERATE" -> Icons.Default.Warning
                        "HIGH"     -> Icons.Default.Error
                        else       -> Icons.Default.Dangerous
                    },
                    contentDescription = null,
                    tint     = rColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFmt.format(Date(record.scanTime)),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Text(
                    "${record.estimatedDose} ppm·hr  |  ${record.riskLevel}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = rColor
                )
                Text(
                    "Confidence: ${(record.confidence * 100).toInt()}%  |  Temp: ${record.temperature}°C",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun StatChip(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Card(
        modifier  = modifier,
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier  = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
