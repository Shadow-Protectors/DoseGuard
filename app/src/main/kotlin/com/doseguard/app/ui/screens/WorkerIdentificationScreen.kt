package com.doseguard.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doseguard.app.model.BandEntity
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.BandAssignmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerIdentificationScreen(
    vm: BandAssignmentViewModel,
    onContinueToScanBand: (workerId: String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by vm.uiState.collectAsState()
    val employeeInput by vm.employeeInput.collectAsState()

    var showCameraScannerDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SurfaceBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Assign Dosimeter Band",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        )
                        Text(
                            text = "Identify worker before issuing a dosimeter",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardWhite,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = NavyPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Instructions Header ──────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BlueLight,
                border = BorderStroke(1.dp, CardStroke),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Identify the worker against the plant directory using their Employee ID or ID badge barcode.",
                        fontSize = 13.sp,
                        color = NavyPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            // ── Dynamic State Switching Card ─────────────────────────────────
            when (val state = uiState) {
                is BandAssignmentViewModel.UiState.WorkerFound -> {
                    WorkerFoundCard(
                        worker = state.worker,
                        activeBand = state.activeBand,
                        onSearchDifferent = { vm.selectDifferentWorker() },
                        onProceed = {
                            vm.proceedToScanBand()
                            onContinueToScanBand(state.worker.workerId)
                        }
                    )
                }

                is BandAssignmentViewModel.UiState.WorkerNotFound -> {
                    WorkerNotFoundCard(
                        attemptedId = state.employeeId,
                        onRetry = { vm.selectDifferentWorker() }
                    )
                }

                else -> {
                    // Search & Scanner Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        border = BorderStroke(1.dp, CardStroke),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "WORKER IDENTIFICATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )

                            // Method 1: Manual Employee ID Search
                            OutlinedTextField(
                                value = employeeInput,
                                onValueChange = { vm.onEmployeeInputChanged(it) },
                                label = { Text("Worker ID") },
                                placeholder = { Text("Enter Employee ID (e.g. EMP-1052)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = NavyPrimary)
                                },
                                trailingIcon = {
                                    if (employeeInput.isNotBlank()) {
                                        IconButton(onClick = { vm.onEmployeeInputChanged("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = { vm.searchWorker() }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CardWhite,
                                    unfocusedContainerColor = CardWhite,
                                    focusedBorderColor = NavyPrimary,
                                    unfocusedBorderColor = CardStroke,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Search Button (56dp tall)
                            Button(
                                onClick = { vm.searchWorker() },
                                enabled = employeeInput.isNotBlank() && state !is BandAssignmentViewModel.UiState.SearchingWorker,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                if (state is BandAssignmentViewModel.UiState.SearchingWorker) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Searching Database…", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Search Worker", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Divider Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(1f), color = CardStroke)
                                Text(
                                    text = "  OR  ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f), color = CardStroke)
                            }

                            // Method 2: Scan Worker Card Button (56dp tall)
                            OutlinedButton(
                                onClick = { showCameraScannerDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SurfaceBg,
                                    contentColor = NavyPrimary
                                ),
                                border = BorderStroke(1.5.dp, NavyPrimary.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = NavyPrimary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Scan Worker ID Card / Barcode", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // ── Demo Fast-Select Chips ────────────────────────────────
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        border = BorderStroke(1.dp, CardStroke),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "1-TAP EVALUATOR PRESETS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ActionChip(
                                    label = "EMP-1052",
                                    subtext = "Arun Kumar",
                                    onClick = {
                                        vm.onEmployeeInputChanged("EMP-1052")
                                        vm.searchWorker("EMP-1052")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ActionChip(
                                    label = "EMP02345",
                                    subtext = "John Mathew",
                                    onClick = {
                                        vm.onEmployeeInputChanged("EMP02345")
                                        vm.searchWorker("EMP02345")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ActionChip(
                                    label = "EMP02346",
                                    subtext = "Anita Desai",
                                    onClick = {
                                        vm.onEmployeeInputChanged("EMP02346")
                                        vm.searchWorker("EMP02346")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ActionChip(
                                    label = "EMP-9999",
                                    subtext = "Test Error",
                                    isWarning = true,
                                    onClick = {
                                        vm.onEmployeeInputChanged("EMP-9999")
                                        vm.searchWorker("EMP-9999")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Quick Worker ID Card Scanner Modal ────────────────────────────────────
    if (showCameraScannerDialog) {
        AlertDialog(
            onDismissRequest = { showCameraScannerDialog = false },
            containerColor = CardWhite,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Employee ID Card", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Point camera at employee QR code or 1D barcode on the ID badge.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    // Quick simulation select buttons for instant demo
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceBg,
                        border = BorderStroke(1.dp, CardStroke),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Simulate Barcode Detection:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showCameraScannerDialog = false
                                        vm.onEmployeeInputChanged("EMP-1052")
                                        vm.searchWorker("EMP-1052")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("EMP-1052", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        showCameraScannerDialog = false
                                        vm.onEmployeeInputChanged("EMP02345")
                                        vm.searchWorker("EMP02345")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("EMP02345", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCameraScannerDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

// ── Component: Worker Found Information Card ────────────────────────────────────
@Composable
private fun WorkerFoundCard(
    worker: WorkerEntity,
    activeBand: BandEntity?,
    onSearchDifferent: () -> Unit,
    onProceed: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.5.dp, StatusSafe),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Green Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StatusSafeBg,
                    border = BorderStroke(1.dp, StatusSafe.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSafe, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "WORKER FOUND",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatusSafe
                        )
                    }
                }

                TextButton(
                    onClick = onSearchDifferent,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("Search a different worker", fontSize = 12.sp, color = NavyPrimary, fontWeight = FontWeight.Bold)
                }
            }

            // Avatar & Name Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = BlueLight,
                    border = BorderStroke(2.dp, NavyPrimary),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = worker.name.take(2).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = worker.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = worker.designation,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider(color = CardStroke)

            // Details Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = "Employee ID", value = worker.employeeId, isMonospace = true)
                DetailRow(label = "Department", value = worker.department)
                DetailRow(label = "Shift Assignment", value = "${worker.shift} Shift")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Employment Status", fontSize = 13.sp, color = TextSecondary)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StatusSafeBg
                    ) {
                        Text(
                            text = "Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusSafe,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Notice if worker already has a band
            if (activeBand != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusModerateBg,
                    border = BorderStroke(1.dp, StatusModerate.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = StatusModerate, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Currently linked to ${activeBand.bandId}. Assigning a new dosimeter will formally retire the previous band.",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Full-Width Action Button (56dp)
            Button(
                onClick = onProceed,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Continue to Assign Band", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }
    }
}

// ── Component: Worker Not Found Error Card ──────────────────────────────────────
@Composable
private fun WorkerNotFoundCard(
    attemptedId: String,
    onRetry: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.5.dp, StatusCritical),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = StatusCriticalBg,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PersonOff, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(28.dp))
                }
            }

            Text(
                text = "Worker Not Found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StatusCritical
            )

            Text(
                text = "No employee matching \"$attemptedId\" was found in the industrial worker database. Please verify the Employee ID number or scan the physical ID card.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again / Search Different ID", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    subtext: String,
    isWarning: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isWarning) StatusCriticalBg else BlueLight,
        border = BorderStroke(1.dp, if (isWarning) StatusCritical.copy(alpha = 0.5f) else CardStroke),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) StatusCritical else NavyPrimary
            )
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}
