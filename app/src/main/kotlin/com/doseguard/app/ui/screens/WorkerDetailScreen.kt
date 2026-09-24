package com.doseguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.repository.DoseGuardRepository
import com.doseguard.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkerDetailScreen(
    workerId: String,
    bandId: String,
    repository: DoseGuardRepository,
    onStartScan: () -> Unit,
    onViewHistory: () -> Unit,
    onBack: () -> Unit
) {
    val scope  = rememberCoroutineScope()
    var worker by remember { mutableStateOf<WorkerEntity?>(null) }
    var band   by remember { mutableStateOf<BandEntity?>(null) }

    // Load from Room on first composition
    LaunchedEffect(workerId, bandId) {
        worker = repository.getWorkerById(workerId)
        band   = repository.getBandById(bandId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Worker Details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

        if (worker == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyPrimary)
            }
            return@Scaffold
        }

        val w = worker!!
        val b = band

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Worker avatar + name card ──────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = w.name.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column {
                        Text(w.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(w.designation, color = Color.White.copy(alpha = 0.80f), style = MaterialTheme.typography.bodyMedium)
                        Text("EMP: ${w.employeeId}", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // ── Info rows ──────────────────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Worker Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    HorizontalDivider()
                    InfoRow(Icons.Default.Business,  "Department", w.department)
                    InfoRow(Icons.Default.Schedule,  "Shift",      w.shift)
                    InfoRow(Icons.Default.Circle,    "Status",     w.status,
                        valueColor = if (w.status == "ACTIVE") StatusSafe else StatusCritical)
                    InfoRow(Icons.Default.CalendarToday, "Registered",
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            .format(Date(w.createdAt)))
                }
            }

            // ── Band summary card ──────────────────────────────────────────────
            b?.let { band ->
                val dosePercent   = (band.currentEstimatedDose / band.maximumDose).coerceIn(0.0, 1.0)
                val doseColor     = when {
                    dosePercent < 0.4 -> StatusSafe
                    dosePercent < 0.7 -> StatusModerate
                    dosePercent < 0.9 -> StatusHigh
                    else              -> StatusCritical
                }
                val expiryFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(band.expiryDate))
                val isExpired = System.currentTimeMillis() > band.expiryDate

                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Watch, contentDescription = null, tint = NavyPrimary)
                            Text("Dosimeter Band", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        HorizontalDivider()
                        InfoRow(Icons.Default.QrCode, "Band ID", band.bandId)
                        InfoRow(Icons.Default.EventAvailable, "Expiry",  "$expiryFmt${if (isExpired) " ⚠️ EXPIRED" else ""}")
                        InfoRow(Icons.Default.Circle, "Status", band.bandStatus,
                            valueColor = if (band.bandStatus == "ACTIVE") StatusSafe else StatusCritical)

                        // Dose progress bar
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cumulative Dose", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                Text(
                                    "${band.currentEstimatedDose} / ${band.maximumDose} ppm·hr",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = doseColor
                                )
                            }
                            LinearProgressIndicator(
                                progress     = { dosePercent.toFloat() },
                                modifier     = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color        = doseColor,
                                trackColor   = CardStroke
                            )
                        }

                        if (isExpired) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StatusCriticalBg),
                                shape  = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(16.dp))
                                    Text("Band has expired. Replace immediately.", color = StatusCritical, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            // ── Action buttons ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick  = onStartScan,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Band", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick  = onViewHistory,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("History", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = valueColor)
        }
    }
}
