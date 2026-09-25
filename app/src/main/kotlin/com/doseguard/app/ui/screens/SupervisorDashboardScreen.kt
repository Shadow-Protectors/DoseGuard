package com.doseguard.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doseguard.app.viewmodel.RosterFilter
import com.doseguard.app.viewmodel.SupervisorDashboardViewModel
import com.doseguard.app.viewmodel.WorkerRosterItem
import java.util.Locale

// Theme colors
private val BgDark       = Color(0xFF0B1120)
private val CardSurface  = Color(0xFF161F36)
private val CardBorder   = Color(0xFF263554)
private val PrimaryCyan  = Color(0xFF06B6D4)
private val CyanLight    = Color(0xFF22D3EE)
private val SafeGreen    = Color(0xFF10B981)
private val WarningAmber = Color(0xFFF59E0B)
private val AlertRed     = Color(0xFFEF4444)
private val TextMuted    = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorDashboardScreen(
    vm: SupervisorDashboardViewModel = viewModel(),
    onScanWristband: () -> Unit,
    onWorkerSelected: (workerId: String, bandId: String) -> Unit,
    onViewHistory: (workerId: String, bandId: String) -> Unit,
    onOpenAlerts: (workerId: String, bandId: String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val plantMetrics by vm.plantMetrics.collectAsState()
    val rosterList by vm.rosterList.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val selectedFilter by vm.selectedFilter.collectAsState()
    val showEnrollDialog by vm.showEnrollDialog.collectAsState()
    val alertCount by vm.activeAlertCount.collectAsState()

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Dose",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Guard",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCyan
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PrimaryCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "SUPERVISOR HUB",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanLight,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Sweetening & Refining Unit - Sector 04",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    if (alertCount > 0) {
                        IconButton(onClick = {
                            val alertItem = rosterList.firstOrNull { it.hasActiveAlert }
                            if (alertItem != null) {
                                onOpenAlerts(alertItem.worker.workerId, alertItem.activeBand?.bandId ?: "")
                            }
                        }) {
                            BadgedBox(badge = { Badge { Text("$alertCount") } }) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alerts",
                                    tint = AlertRed
                                )
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanWristband,
                containerColor = PrimaryCyan,
                contentColor = Color(0xFF0B1120),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Wristband", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. Active Alert Hazard Banner ──────────────────────────────────
            if (plantMetrics.alertCount > 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlertRed.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val alertItem = rosterList.firstOrNull { it.hasActiveAlert }
                                if (alertItem != null) {
                                    onOpenAlerts(alertItem.worker.workerId, alertItem.activeBand?.bandId ?: "")
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "STATUTORY HAZARD ALERT ACTIVE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = AlertRed
                                )
                                Text(
                                    text = "${plantMetrics.alertCount} worker(s) exceeded OSHA 8-hr TWA limit (>10 ppm). Tap to review SOP.",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AlertRed)
                        }
                    }
                }
            }

            // ── 2. Plant KPI Metrics Cards ─────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "TOTAL WORKERS",
                        value = "${plantMetrics.totalWorkers}",
                        subtext = "Enrolled personnel",
                        icon = Icons.Default.People,
                        accentColor = PrimaryCyan,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "ACTIVE BANDS",
                        value = "${plantMetrics.activeBandsCount}",
                        subtext = "Monitored dosimeters",
                        icon = Icons.Default.Watch,
                        accentColor = SafeGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "HAZARD ALERTS",
                        value = "${plantMetrics.alertCount}",
                        subtext = if (plantMetrics.alertCount == 0) "All zones safe" else "Action required",
                        icon = Icons.Default.Shield,
                        accentColor = if (plantMetrics.alertCount == 0) SafeGreen else AlertRed,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "COMPLIANCE",
                        value = "${String.format(Locale.US, "%.1f", plantMetrics.complianceRatePercent)}%",
                        subtext = "OSHA/DGMS index",
                        icon = Icons.Default.CheckCircle,
                        accentColor = if (plantMetrics.complianceRatePercent >= 95.0) SafeGreen else WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── 3. Quick Action Buttons ────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onScanWristband,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardSurface
                        ),
                        border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Wristband", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { vm.setEnrollDialogVisible(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardSurface
                        ),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = CyanLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enroll Worker", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── 4. Search Bar & Filter Chips ───────────────────────────────────
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { vm.setSearchQuery(it) },
                    placeholder = { Text("Search worker name, EMP ID, department, or band...", fontSize = 13.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { vm.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(RosterFilter.values()) { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { vm.setFilter(filter) },
                            label = { Text(filter.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CardSurface,
                                labelColor = TextMuted,
                                selectedContainerColor = PrimaryCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyanLight
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PrimaryCyan else CardBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // ── 5. Worker Roster Header ────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLANT WORKER ROSTER (${rosterList.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }

            // ── 6. Worker Cards List ───────────────────────────────────────────
            if (rosterList.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No workers match your filter",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try adjusting your search query or enroll a new worker profile.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(rosterList, key = { it.worker.workerId }) { item ->
                    WorkerRosterCard(
                        item = item,
                        onCardClick = {
                            onWorkerSelected(item.worker.workerId, item.activeBand?.bandId ?: "")
                        },
                        onViewHistory = {
                            onViewHistory(item.worker.workerId, item.activeBand?.bandId ?: "")
                        },
                        onScanSensor = {
                            onWorkerSelected(item.worker.workerId, item.activeBand?.bandId ?: "")
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // ── Enroll Worker Modal Dialog ─────────────────────────────────────────────
    if (showEnrollDialog) {
        EnrollWorkerDialog(
            onDismiss = { vm.setEnrollDialogVisible(false) },
            onEnroll = { name, empId, dept, designation, shift ->
                vm.enrollWorker(name, empId, dept, designation, shift)
            }
        )
    }
}

// ── Component: KPI Metric Card ──────────────────────────────────────────────────
@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtext,
                fontSize = 11.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Component: Worker Roster Card ───────────────────────────────────────────────
@Composable
private fun WorkerRosterCard(
    item: WorkerRosterItem,
    onCardClick: () -> Unit,
    onViewHistory: () -> Unit,
    onScanSensor: () -> Unit
) {
    val worker = item.worker
    val band = item.activeBand
    val initials = worker.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")

    val statusColor = when {
        item.hasActiveAlert -> AlertRed
        band == null -> TextMuted
        band.bandStatus == "EXPIRED" -> WarningAmber
        band.bandStatus == "SATURATED" -> AlertRed
        else -> SafeGreen
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, if (item.hasActiveAlert) AlertRed.copy(alpha = 0.6f) else CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Initials
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, statusColor),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = worker.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = worker.employeeId,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyanLight,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${worker.designation} • ${worker.department}",
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Shift tag
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = worker.shift,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(10.dp))

            // Band Assignment & Exposure Progress
            if (band != null) {
                val dose = band.currentEstimatedDose
                val maxDose = band.maximumDose
                val fraction = (dose / maxDose).toFloat().coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Watch,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = band.bandId,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = band.bandStatus,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "${String.format(Locale.US, "%.1f", dose)} / ${String.format(Locale.US, "%.0f", maxDose)} ppm·hr",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (fraction > 0.8f) AlertRed else CyanLight
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        fraction >= 0.8f -> AlertRed
                        fraction >= 0.5f -> WarningAmber
                        else -> PrimaryCyan
                    },
                    trackColor = Color(0xFF1E293B)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "No wristband currently assigned. Scan a band QR to link.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onViewHistory,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("History Trends", fontSize = 12.sp, color = TextMuted)
                }

                Spacer(modifier = Modifier.width(6.dp))

                FilledTonalButton(
                    onClick = onScanSensor,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PrimaryCyan.copy(alpha = 0.15f),
                        contentColor = CyanLight
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inspect / Scan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Component: Enroll Worker Dialog ─────────────────────────────────────────────
@Composable
private fun EnrollWorkerDialog(
    onDismiss: () -> Unit,
    onEnroll: (name: String, empId: String, dept: String, designation: String, shift: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Refinery Sweetening Unit") }
    var designation by remember { mutableStateOf("Plant Operator") }
    var shift by remember { mutableStateOf("Morning") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PrimaryCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enroll Plant Worker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pre-register an employee in the plant database. A dosimeter band can be assigned later via QR scan.",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                // 1-Tap Demo Auto-Fill
                OutlinedButton(
                    onClick = {
                        name = "Ananya Verma"
                        employeeId = "EMP-4412"
                        department = "Sulfur Recovery Unit"
                        designation = "Process Engineer"
                        shift = "Morning"
                    },
                    border = BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Fill Demo Employee Profile", color = CyanLight, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = employeeId,
                    onValueChange = { employeeId = it },
                    label = { Text("Employee ID (e.g. EMP-7821) *", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Shift Chips
                Text("Shift Assignment", fontSize = 12.sp, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Morning", "Evening", "Night").forEach { s ->
                        FilterChip(
                            selected = shift == s,
                            onClick = { shift = s },
                            label = { Text(s, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = BgDark,
                                labelColor = TextMuted,
                                selectedContainerColor = PrimaryCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyanLight
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && employeeId.isNotBlank()) {
                        onEnroll(name, employeeId, department, designation, shift)
                    }
                },
                enabled = name.isNotBlank() && employeeId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
            ) {
                Text("Enroll Employee", color = Color(0xFF0B1120), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
