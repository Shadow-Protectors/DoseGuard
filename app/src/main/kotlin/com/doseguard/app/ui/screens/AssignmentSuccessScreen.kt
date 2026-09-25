package com.doseguard.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doseguard.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen 3 — Assignment confirmed.
 * Animated green check, assignment summary and the operator's next actions.
 */
@Composable
fun AssignmentSuccessScreen(
    employeeId: String,
    workerName: String,
    bandId: String,
    assignedTime: Long,
    onDone: () -> Unit,
    onStartScan: () -> Unit
) {
    // Entry animation for the check badge
    val scale = remember { Animatable(0.4f) }
    val ringScale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        ringScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 260f)
        )
    }

    val timeText = remember(assignedTime) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(assignedTime))
    }
    val dateText = remember(assignedTime) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(assignedTime))
    }

    Scaffold(containerColor = SurfaceBg) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .scale(ringScale.value)
                        .background(StatusSafe.copy(alpha = 0.15f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(scale.value)
                        .background(StatusSafe, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Success",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Band Assigned Successfully",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "The dosimeter is now linked to the worker and actively logging exposure.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    DetailRow(Icons.Filled.Badge, "Worker", employeeId)
                    DetailRow(Icons.Filled.Person, "Name", workerName)
                    DetailRow(Icons.Filled.Watch, "Band", bandId)
                    DetailRow(Icons.Filled.Schedule, "Assignment Time", "$timeText · $dateText")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.VerifiedUser,
                            null,
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Status",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill("Active", StatusSafe, StatusSafeBg)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onDone,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .height(58.dp)
            ) {
                Text("Done", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onStartScan,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .height(54.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, null)
                Spacer(Modifier.width(10.dp))
                Text("Start Exposure Scan", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
