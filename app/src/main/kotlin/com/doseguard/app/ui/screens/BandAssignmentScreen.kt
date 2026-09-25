package com.doseguard.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doseguard.app.model.WorkerEntity
import com.doseguard.app.ui.components.QrScannerCamera
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.BandAssignmentViewModel

/**
 * Screen 1 — Worker Identification.
 *
 * The operator identifies the worker first (typed Employee ID or scanned employee
 * card), reviews the record pulled from the worker database, and only then moves
 * on to scanning a dosimeter band.
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
fun BandAssignmentScreen(
    vm: BandAssignmentViewModel = viewModel(),
    onAssignBand: (workerId: String) -> Unit,
    onBack: () -> Unit
) {
    val lookup by vm.workerLookup.collectAsState()
    val input by vm.workerIdInput.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    var showCardScanner by remember { mutableStateOf(false) }

    // Close the card scanner as soon as a lookup starts or resolves.
    LaunchedEffect(lookup) {
        if (lookup !is BandAssignmentViewModel.WorkerLookup.Idle) showCardScanner = false
    }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 720

    Scaffold(
        containerColor = SurfaceBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Assign Dosimeter Band",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            "Identify the worker before assigning a band",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isTablet) 48.dp else 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.widthIn(max = 620.dp)) {
                AnimatedContent(
                    targetState = lookup is BandAssignmentViewModel.WorkerLookup.Found,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 6 }) togetherWith fadeOut()
                    },
                    label = "workerSection"
                ) { found ->
                    if (found) {
                        val worker =
                            (lookup as BandAssignmentViewModel.WorkerLookup.Found).worker
                        WorkerInformationCard(
                            worker = worker,
                            onAssignBand = { onAssignBand(worker.workerId) },
                            onSearchAgain = { vm.clearWorker() }
                        )
                    } else {
                        WorkerSearchCard(
                            input = input,
                            lookup = lookup,
                            onInputChange = vm::onWorkerIdInputChange,
                            onSearch = {
                                keyboard?.hide()
                                vm.searchWorker()
                            },
                            onScanCard = { showCardScanner = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HintFooter()
        }
    }

    // ── Employee card scanner overlay ────────────────────────────────────────
    if (showCardScanner) {
        WorkerCardScannerOverlay(
            onScanned = vm::onWorkerCardScanned,
            onClose = { showCardScanner = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search card (idle / searching / not-found)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorkerSearchCard(
    input: String,
    lookup: BandAssignmentViewModel.WorkerLookup,
    onInputChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanCard: () -> Unit
) {
    val searching = lookup is BandAssignmentViewModel.WorkerLookup.Searching

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(BlueLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Badge, null, tint = NavyPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Worker Identification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Text(
                        "Look up the worker in the plant database",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = { Text("Worker ID") },
                placeholder = { Text("Enter Employee ID") },
                leadingIcon = { Icon(Icons.Filled.Person, null, tint = NavyPrimary) },
                singleLine = true,
                enabled = !searching,
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    focusedLabelColor = NavyPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onSearch,
                enabled = input.isNotBlank() && !searching,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (searching) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Searching…", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Filled.Search, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Search Worker", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(18.dp))
            OrDivider()
            Spacer(Modifier.height(18.dp))

            OutlinedButton(
                onClick = onScanCard,
                enabled = !searching,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, NavyPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Filled.QrCodeScanner, null)
                Spacer(Modifier.width(10.dp))
                Text("Scan Worker ID Card", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            AnimatedVisibility(visible = lookup is BandAssignmentViewModel.WorkerLookup.NotFound) {
                val q = (lookup as? BandAssignmentViewModel.WorkerLookup.NotFound)?.query.orEmpty()
                Column {
                    Spacer(Modifier.height(16.dp))
                    StatusBanner(
                        icon = Icons.Filled.WarningAmber,
                        tint = StatusCritical,
                        background = StatusCriticalBg,
                        title = "Worker Not Found",
                        message = "No worker matches \"$q\" in the plant database. " +
                            "Check the Employee ID and try again."
                    )
                }
            }

            AnimatedVisibility(visible = lookup is BandAssignmentViewModel.WorkerLookup.Error) {
                val msg = (lookup as? BandAssignmentViewModel.WorkerLookup.Error)?.message.orEmpty()
                Column {
                    Spacer(Modifier.height(16.dp))
                    StatusBanner(
                        icon = Icons.Filled.ErrorOutline,
                        tint = StatusHigh,
                        background = StatusHighBg,
                        title = "Lookup Failed",
                        message = msg
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Worker information card (found)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorkerInformationCard(
    worker: WorkerEntity,
    onAssignBand: () -> Unit,
    onSearchAgain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = StatusSafe)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Worker Found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                }
                StatusPill(
                    text = if (worker.status.equals("ACTIVE", true)) "Active" else worker.status,
                    color = if (worker.status.equals("ACTIVE", true)) StatusSafe else TextMuted,
                    background = if (worker.status.equals("ACTIVE", true)) StatusSafeBg else SurfaceBg
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(BlueLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        worker.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(worker.designation, fontSize = 13.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = CardStroke)
            Spacer(Modifier.height(6.dp))

            DetailRow(Icons.Filled.Badge, "Employee ID", worker.employeeId)
            DetailRow(Icons.Filled.Person, "Name", worker.name)
            DetailRow(Icons.Filled.Factory, "Department", worker.department)
            DetailRow(Icons.Filled.Schedule, "Shift", worker.shift)
            DetailRow(
                Icons.Filled.VerifiedUser,
                "Status",
                if (worker.status.equals("ACTIVE", true)) "Active" else worker.status
            )

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = onAssignBand,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Icon(Icons.Filled.Watch, null)
                Spacer(Modifier.width(10.dp))
                Text("Assign Band", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }

            Spacer(Modifier.height(6.dp))

            TextButton(
                onClick = onSearchAgain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search a different worker", color = TextSecondary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Employee card scanner overlay
// ─────────────────────────────────────────────────────────────────────────────

@androidx.camera.core.ExperimentalGetImage
@Composable
private fun WorkerCardScannerOverlay(
    onScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        QrScannerCamera(
            modifier = Modifier.fillMaxSize(),
            onCodeScanned = onScanned,
            onPermissionDenied = {
                TextButton(onClick = onClose) {
                    Text("Enter Worker ID manually", color = Color.White)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) { Icon(Icons.Filled.Close, "Close scanner") }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Scan Worker ID Card",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "Hold the employee card steady inside the frame.",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(32.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared small pieces (used by the band scan + success screens too)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = CardStroke)
        Text(
            "OR",
            modifier = Modifier.padding(horizontal = 14.dp),
            color = TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = CardStroke)
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun StatusPill(text: String, color: Color, background: Color) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusBanner(
    icon: ImageVector,
    tint: Color,
    background: Color,
    title: String,
    message: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(12.dp))
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Icon(icon, null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(message, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun HintFooter() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(max = 620.dp)
    ) {
        Icon(
            Icons.Filled.Info,
            null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Demo IDs: EMP02345, EMP02346, EMP02347, EMP-7821",
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}
