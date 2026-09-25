package com.doseguard.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.repository.DoseGuardRepository
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.BandAssignmentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandAssignmentSuccessScreen(
    workerId: String,
    bandId: String,
    repository: DoseGuardRepository,
    vm: BandAssignmentViewModel,
    onDone: () -> Unit,
    onStartExposureScan: (workerId: String, bandId: String) -> Unit,
    onAssignAnother: () -> Unit
) {
    var worker by remember { mutableStateOf<WorkerEntity?>(null) }
    var band by remember { mutableStateOf<BandEntity?>(null) }

    // Load from Room DB
    LaunchedEffect(workerId, bandId) {
        worker = repository.getWorkerById(workerId)
        band = repository.getBandById(bandId)
    }

    // Success checkmark scale animation
    val scale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val now = remember { System.currentTimeMillis() }
    val timeFormatted = remember(now) {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date(now))
    }

    Scaffold(
        containerColor = SurfaceBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── Animated Green Circle with Checkmark ─────────────────────────
            Surface(
                shape = CircleShape,
                color = StatusSafeBg,
                border = BorderStroke(3.dp, StatusSafe),
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale.value)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = StatusSafe,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Text(
                text = "Band Assigned Successfully",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "The dosimeter wristband is now registered and active in the industrial safety database.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // ── Summary Details Card ─────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, CardStroke),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ASSIGNMENT SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    HorizontalDivider(color = CardStroke)

                    SummaryItem(
                        label = "Worker",
                        value = "${worker?.name ?: "Worker"} (${worker?.employeeId ?: workerId})",
                        subtext = worker?.department ?: "Plant Unit"
                    )

                    SummaryItem(
                        label = "Band ID",
                        value = band?.bandId ?: bandId,
                        subtext = "Batch: ${band?.batchNo ?: "BATCH-2026-A1"}",
                        isMonospace = true
                    )

                    SummaryItem(
                        label = "Assignment Time",
                        value = timeFormatted,
                        subtext = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(Date(now))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dosimeter Status", fontSize = 13.sp, color = TextSecondary)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StatusSafeBg,
                            border = BorderStroke(1.dp, StatusSafe.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Active • Monitored",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusSafe,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Action Buttons ───────────────────────────────────────────────
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Done (Return to Dashboard)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { onStartExposureScan(workerId, bandId) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = BlueLight.copy(alpha = 0.4f),
                    contentColor = NavyPrimary
                ),
                border = BorderStroke(1.5.dp, NavyPrimary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Exposure Scan", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = {
                    vm.resetAll()
                    onAssignAnother()
                }
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Assign Another Band", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    subtext: String,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            )
            Text(subtext, fontSize = 11.sp, color = TextSecondary)
        }
    }
}
