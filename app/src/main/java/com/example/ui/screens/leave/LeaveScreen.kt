package com.example.ui.screens.leave

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Holiday
import com.example.data.model.LeaveApplication
import com.example.data.model.LeaveBalance
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val leaves by viewModel.leaves.collectAsState()
    val holidays by viewModel.holidays.collectAsState()
    val corrections by viewModel.corrections.collectAsState()
    val leaveBalance by viewModel.currentLeaveBalance.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val liveState by viewModel.liveState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Leaves & Balances, 1: Holidays, 2: Corrections
    var showApplyLeaveDialog by remember { mutableStateOf(false) }
    var showAddHolidayDialog by remember { mutableStateOf(false) }
    var reviewLeaveDialog by remember { mutableStateOf<LeaveApplication?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Leave Balances Cards (Casual, Sick, Earned)
        Spacer(modifier = Modifier.height(16.dp))
        LeaveBalancesHeader(balance = leaveBalance)

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Tab Row
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Leaves", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Holidays", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    val pendingCorrectionsCount = corrections.count { it.status == "PENDING" }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Corrections", fontWeight = FontWeight.Bold)
                        if (pendingCorrectionsCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge { Text("$pendingCorrectionsCount") }
                        }
                    }
                },
                icon = { Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                // Leaves List & Apply Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAdminMode) "All Employee Leaves" else "My Leave Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showApplyLeaveDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("apply_leave_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply Leave", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val displayedLeaves = if (isAdminMode) {
                    leaves
                } else {
                    val myEmpId = liveState.currentEmployee?.id ?: 1L
                    leaves.filter { it.employeeId == myEmpId }
                }

                if (displayedLeaves.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No leave requests found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayedLeaves, key = { it.id }) { leave ->
                            LeaveCard(
                                leave = leave,
                                isAdminMode = isAdminMode,
                                onReviewClick = { reviewLeaveDialog = leave }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Holidays List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Corporate Holiday Calendar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (isAdminMode) {
                        FilledTonalButton(
                            onClick = { showAddHolidayDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Holiday", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(holidays, key = { it.id }) { holiday ->
                        HolidayCard(holiday = holiday)
                    }
                }
            }

            2 -> {
                // Corrections List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attendance Regularization Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val displayedCorrections = if (isAdminMode) {
                    corrections
                } else {
                    val myEmpId = liveState.currentEmployee?.id ?: 1L
                    corrections.filter { it.employeeId == myEmpId }
                }

                if (displayedCorrections.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No correction requests submitted.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayedCorrections, key = { it.id }) { correction ->
                            CorrectionCard(
                                correction = correction,
                                isAdminMode = isAdminMode,
                                onApprove = { viewModel.reviewCorrection(correction.id, true) },
                                onReject = { viewModel.reviewCorrection(correction.id, false) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Apply Leave Dialog
    if (showApplyLeaveDialog) {
        ApplyLeaveDialog(
            balance = leaveBalance,
            onDismiss = { showApplyLeaveDialog = false },
            onSubmit = { type, start, end, days, reason ->
                viewModel.submitLeaveApplication(type, start, end, days, reason) {
                    showApplyLeaveDialog = false
                }
            }
        )
    }

    // Add Holiday Dialog (Admin)
    if (showAddHolidayDialog) {
        AddHolidayDialog(
            onDismiss = { showAddHolidayDialog = false },
            onAdd = { name, date, day, type, desc ->
                viewModel.addHoliday(name, date, day, type, desc) {
                    showAddHolidayDialog = false
                }
            }
        )
    }

    // Review Leave Dialog (Admin)
    reviewLeaveDialog?.let { leave ->
        ReviewLeaveDialog(
            leave = leave,
            onDismiss = { reviewLeaveDialog = null },
            onAction = { isApproved, remarks ->
                viewModel.reviewLeave(leave.id, isApproved, remarks)
                reviewLeaveDialog = null
            }
        )
    }
}

@Composable
fun LeaveBalancesHeader(balance: LeaveBalance?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Casual Leave
        val clTotal = balance?.casualLeaveTotal ?: 12
        val clUsed = balance?.casualLeaveUsed ?: 0
        BalanceCard(
            title = "Casual Leave",
            shortCode = "CL",
            remaining = clTotal - clUsed,
            total = clTotal,
            accentColor = PrimaryBlue,
            modifier = Modifier.weight(1f)
        )

        // Sick Leave
        val slTotal = balance?.sickLeaveTotal ?: 10
        val slUsed = balance?.sickLeaveUsed ?: 0
        BalanceCard(
            title = "Sick Leave",
            shortCode = "SL",
            remaining = slTotal - slUsed,
            total = slTotal,
            accentColor = SecondaryTeal,
            modifier = Modifier.weight(1f)
        )

        // Earned Leave
        val elTotal = balance?.earnedLeaveTotal ?: 18
        val elUsed = balance?.earnedLeaveUsed ?: 0
        BalanceCard(
            title = "Earned Leave",
            shortCode = "EL",
            remaining = elTotal - elUsed,
            total = elTotal,
            accentColor = TertiaryAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BalanceCard(
    title: String,
    shortCode: String,
    remaining: Int,
    total: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = shortCode,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "Total $total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$remaining",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Text(
                text = "Days Available",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LeaveCard(
    leave: LeaveApplication,
    isAdminMode: Boolean,
    onReviewClick: () -> Unit
) {
    val statusColor = when (leave.status) {
        "APPROVED" -> StatusSuccess
        "REJECTED" -> StatusAbsent
        else -> StatusPending
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${leave.leaveType} LEAVE (${leave.numberOfDays} ${if (leave.numberOfDays == 1) "Day" else "Days"})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${leave.employeeName} • ${leave.employeeCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = leave.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Duration dates
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${leave.startDate}  to  ${leave.endDate}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            // Reason
            Text(
                text = "Reason: ${leave.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Manager remarks if any
            if (!leave.managerRemarks.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "HR Note: ${leave.managerRemarks}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Review button for Admin
            if (isAdminMode && leave.status == "PENDING") {
                Button(
                    onClick = onReviewClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text("Review Application", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun HolidayCard(holiday: Holiday) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = holiday.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${holiday.date} (${holiday.dayOfWeek})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (holiday.description.isNotBlank()) {
                    Text(
                        text = holiday.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = holiday.type,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CorrectionCard(
    correction: com.example.data.model.AttendanceCorrection,
    isAdminMode: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val statusColor = when (correction.status) {
        "APPROVED" -> StatusSuccess
        "REJECTED" -> StatusAbsent
        else -> StatusPending
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Date: ${correction.date}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = correction.employeeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = correction.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Original Punch", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${correction.originalCheckIn} - ${correction.originalCheckOut}", style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("Requested Corrected", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
                    Text("${correction.requestedCheckIn} - ${correction.requestedCheckOut}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            }

            Text(
                text = "Reason: ${correction.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isAdminMode && correction.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reject")
                    }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                    ) {
                        Text("Approve")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLeaveDialog(
    balance: LeaveBalance?,
    onDismiss: () -> Unit,
    onSubmit: (type: String, start: String, end: String, days: Int, reason: String) -> Unit
) {
    var selectedType by remember { mutableStateOf("CASUAL") }
    var startDate by remember { mutableStateOf("2026-11-16") }
    var endDate by remember { mutableStateOf("2026-11-17") }
    var daysCount by remember { mutableIntStateOf(2) }
    var reason by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply For Leave Online", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Leave Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "CASUAL",
                        onClick = { selectedType = "CASUAL" },
                        label = { Text("Casual") }
                    )
                    FilterChip(
                        selected = selectedType == "SICK",
                        onClick = { selectedType = "SICK" },
                        label = { Text("Sick") }
                    )
                    FilterChip(
                        selected = selectedType == "EARNED",
                        onClick = { selectedType = "EARNED" },
                        label = { Text("Earned") }
                    )
                }

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Number of Days: $daysCount", fontWeight = FontWeight.SemiBold)
                    Row {
                        IconButton(onClick = { if (daysCount > 1) daysCount-- }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        IconButton(onClick = { daysCount++ }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Leave") },
                    placeholder = { Text("Provide details for HR approval") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        errorMessage = "Please enter reason for leave"
                    } else {
                        onSubmit(selectedType, startDate, endDate, daysCount, reason)
                    }
                }
            ) {
                Text("Submit Application")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReviewLeaveDialog(
    leave: LeaveApplication,
    onDismiss: () -> Unit,
    onAction: (isApproved: Boolean, remarks: String) -> Unit
) {
    var remarks by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Leave Request", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${leave.employeeName} (${leave.employeeCode})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text("Type: ${leave.leaveType} • ${leave.numberOfDays} Days")
                Text("Duration: ${leave.startDate} to ${leave.endDate}")
                Text("Reason: ${leave.reason}")

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Manager / HR Remarks") },
                    placeholder = { Text("e.g. Approved. Reliever Aarav assigned.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(true, remarks) },
                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
            ) {
                Text("Approve")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onAction(false, remarks) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Reject")
            }
        }
    )
}

@Composable
fun AddHolidayDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, date: String, day: String, type: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-12-31") }
    var day by remember { mutableStateOf("Thursday") }
    var type by remember { mutableStateOf("NATIONAL") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Corporate Holiday", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Holiday Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = day,
                    onValueChange = { day = it },
                    label = { Text("Day of Week") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && date.isNotBlank()) {
                        onAdd(name, date, day, type, desc)
                    }
                }
            ) {
                Text("Save Holiday")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
