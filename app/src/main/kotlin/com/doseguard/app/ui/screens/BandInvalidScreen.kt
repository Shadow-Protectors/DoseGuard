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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.repository.DoseGuardRepository
import com.doseguard.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandInvalidScreen(
    bandId: String,
    reason: String,
    workerId: String,
    repository: DoseGuardRepository,
    onIssueReplacement: (workerId: String) -> Unit,
    onViewHistory: (workerId: String) -> Unit,
    onBack: () -> Unit
) {
    var worker by remember { mutableStateOf<WorkerEntity?>(null) }
    var band by remember { mutableStateOf<BandEntity?>(null) }

    val actualWorkerId = if (workerId == "none") "" else workerId

    LaunchedEffect(bandId, actualWorkerId) {
        band = repository.getBandById(bandId)
        if (actualWorkerId.isNotBlank()) {
            worker = repository.getWorkerById(actualWorkerId)
        } else if (band?.workerId?.isNotBlank() == true) {
            worker = repository.getWorkerById(band!!.workerId)
        }
    }

    val reasonTitle = when (reason.uppercase()) {
        "EXPIRED"   -> "SHELF-LIFE EXPIRED"
        "SATURATED" -> "DOSE SATURATION REACHED"
        "REPLACED"  -> "WRISTBAND RETIRED & REPLACED"
        else        -> "INVALID DOSIMETER"
    }

    val reasonDescription = when (reason.uppercase()) {
        "EXPIRED"   -> "The chemical lead acetate reagent strip has exceeded its 30-day calibrated shelf-life. Colorimetric readings from expired badges are invalid and unsafe."
        "SATURATED" -> "The dosimeter has accumulated 50.0 ppm·hr of cumulative H2S exposure, reaching full optical saturation. The badge cannot measure further exposure."
        "REPLACED"  -> "This wristband has been formally replaced. Historical records remain archived under the worker profile."
        else        -> "This wristband cannot be used for active shift monitoring."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Band Validity Gate", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StatusCritical,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Invalidation Banner ──────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusCriticalBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = StatusCritical,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = reasonTitle,
                        color = StatusCritical,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Camera shutter access is blocked to prevent false safety readings.",
                        color = StatusCritical.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Diagnostic Details Card ──────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Dosimeter Gate Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Band ID", color = TextSecondary)
                        Text(bandId, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Assigned Worker", color = TextSecondary)
                        Text(worker?.name ?: if (actualWorkerId.isNotBlank()) actualWorkerId else "Unassigned", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gate Status", color = TextSecondary)
                        Text(reason.uppercase(), fontWeight = FontWeight.Bold, color = StatusCritical)
                    }

                    band?.let { b ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cumulative Dose", color = TextSecondary)
                            Text("${b.currentEstimatedDose} / ${b.maximumDose} ppm·hr", fontWeight = FontWeight.Bold, color = StatusCritical)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = reasonDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // ── Statutory Protocol Notice ────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Security, null, tint = NavyPrimary, modifier = Modifier.size(28.dp))
                    Column {
                        Text("DGMS / OSHA Protocol", fontWeight = FontWeight.Bold, color = NavyPrimary, style = MaterialTheme.typography.labelLarge)
                        Text("Mandatory dosimeter badge replacement required prior to entering plant hazard units.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            // ── Action Buttons ───────────────────────────────────────────────
            Button(
                onClick = { onIssueReplacement(worker?.workerId ?: actualWorkerId) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Icon(Icons.Default.Autorenew, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Issue Replacement Wristband", fontWeight = FontWeight.Bold, color = Color.White)
            }

            if (worker != null || actualWorkerId.isNotBlank()) {
                OutlinedButton(
                    onClick = { onViewHistory(worker?.workerId ?: actualWorkerId) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("View Worker Cumulative History", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
