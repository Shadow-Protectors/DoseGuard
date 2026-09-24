package com.doseguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doseguard.app.ui.theme.*
import com.doseguard.app.viewmodel.WorkerRegistrationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerRegistrationScreen(
    bandId: String,
    qrData: String,
    vm: WorkerRegistrationViewModel = viewModel(),
    onRegistered: (workerId: String, bandId: String) -> Unit,
    onBack: () -> Unit
) {
    val state           by vm.state.collectAsState()
    val existingWorkers by vm.existingWorkers.collectAsState()
    val selectedWorker  by vm.selectedWorkerId.collectAsState()

    val name       by vm.name.collectAsState()
    val empId      by vm.employeeId.collectAsState()
    val dept       by vm.department.collectAsState()
    val desig      by vm.designation.collectAsState()
    val shift      by vm.shift.collectAsState()
    val nameErr    by vm.nameError.collectAsState()
    val empErr     by vm.employeeIdError.collectAsState()
    val deptErr    by vm.departmentError.collectAsState()
    val desigErr   by vm.designationError.collectAsState()

    var selectedTab by remember { mutableIntStateOf(if (existingWorkers.isNotEmpty()) 0 else 1) }

    val shiftOptions = listOf("Morning", "Evening", "Night", "Rotational")
    var shiftExpanded by remember { mutableStateOf(false) }

    // Navigate on success
    LaunchedEffect(state) {
        if (state is WorkerRegistrationViewModel.RegistrationState.Success) {
            val s = state as WorkerRegistrationViewModel.RegistrationState.Success
            vm.resetState()
            onRegistered(s.workerId, bandId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wristband Assignment", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Band info header
            Card(
                colors = CardDefaults.cardColors(containerColor = BlueLight),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Watch, contentDescription = null, tint = NavyPrimary)
                    Column {
                        Text("Target Wristband", style = MaterialTheme.typography.labelMedium, color = NavyPrimary)
                        Text(bandId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                }
            }

            // Mode Selector: Link to Existing vs Create New
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardWhite,
                contentColor = NavyPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Link Existing (Replace)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Autorenew, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("New Worker Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTab == 0) {
                // ── Tab 0: Link to Existing Worker (Band Replacement) ─────────
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Select Worker for Band Replacement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Assigns this new wristband to the worker while archiving the previous unit. The worker's cumulative health records remain continuous.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                        HorizontalDivider()

                        if (existingWorkers.isEmpty()) {
                            Text("No existing workers found. Please create a new profile.", color = TextSecondary)
                        } else {
                            existingWorkers.forEach { w ->
                                val isSelected = selectedWorker == w.workerId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) BlueLight else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) NavyPrimary else CardStroke,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { vm.selectedWorkerId.value = w.workerId }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(w.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("${w.employeeId} • ${w.department} (${w.shift} Shift)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { vm.selectedWorkerId.value = w.workerId },
                                        colors = RadioButtonDefaults.colors(selectedColor = NavyPrimary)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        val isSaving = state is WorkerRegistrationViewModel.RegistrationState.Saving
                        Button(
                            onClick = { vm.assignToExistingWorker(bandId) },
                            enabled = !isSaving && existingWorkers.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Link, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Assign Replacement Band to Worker", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // ── Tab 1: Register Brand New Worker Profile ─────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "New Worker Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Button(
                        onClick = {
                            vm.name.value = "Arun Varma"
                            vm.employeeId.value = "EMP-8842"
                            vm.department.value = "Hydrotreater Sweetening Unit"
                            vm.designation.value = "Process Technician"
                            vm.shift.value = "Morning"
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Demo Auto-Fill", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Form fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { vm.name.value = it },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = nameErr != null,
                    supportingText = nameErr?.let { { Text(it, color = StatusCritical) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = empId,
                    onValueChange = { vm.employeeId.value = it },
                    label = { Text("Employee ID *") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    isError = empErr != null,
                    supportingText = empErr?.let { { Text(it, color = StatusCritical) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = dept,
                    onValueChange = { vm.department.value = it },
                    label = { Text("Department *") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    isError = deptErr != null,
                    supportingText = deptErr?.let { { Text(it, color = StatusCritical) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = desig,
                    onValueChange = { vm.designation.value = it },
                    label = { Text("Designation *") },
                    leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                    isError = desigErr != null,
                    supportingText = desigErr?.let { { Text(it, color = StatusCritical) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Shift dropdown
                ExposedDropdownMenuBox(
                    expanded  = shiftExpanded,
                    onExpandedChange = { shiftExpanded = it }
                ) {
                    OutlinedTextField(
                        value = shift,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Shift") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = shiftExpanded,
                        onDismissRequest = { shiftExpanded = false }
                    ) {
                        shiftOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    vm.shift.value = option
                                    shiftExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val isSaving = state is WorkerRegistrationViewModel.RegistrationState.Saving
                Button(
                    onClick  = { vm.registerNewWorker(bandId) },
                    enabled  = !isSaving,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Saving to Database…", color = Color.White, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Register & Assign Band", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Error message
            if (state is WorkerRegistrationViewModel.RegistrationState.Error) {
                val e = state as WorkerRegistrationViewModel.RegistrationState.Error
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Text("Error: ${e.message}", modifier = Modifier.padding(12.dp), color = StatusCritical)
                }
            }
        }
    }
}
