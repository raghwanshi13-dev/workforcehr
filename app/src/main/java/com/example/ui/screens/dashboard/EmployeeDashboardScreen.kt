package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboardScreen(
    viewModel: MainViewModel,
    onNavigateToPunch: () -> Unit,
    onNavigateToLeaves: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liveState by viewModel.liveState.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val leaveBalance by viewModel.currentLeaveBalance.collectAsState()
    val holidays by viewModel.holidays.collectAsState()
    val employee = liveState.currentEmployee
    val shift = liveState.currentShift
    val todayRecord = liveState.todayRecord
    val isInsideGeofence = liveState.isInsideGeofence

    val employeeAttendance = remember(allAttendance, employee) {
        allAttendance.filter { it.employeeId == employee?.id }
    }

    val todayDate = remember { DateTimeUtils.getTodayDate() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Employee Profile & Role Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, OutlineSlate)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryContainerBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = employee?.fullName ?: "Staff Member",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${employee?.designation ?: "Staff"} • ${employee?.department ?: "Operations"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Code: ${employee?.empCode ?: "EMP-1002"} • ${employee?.email ?: ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryContainerBlue
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = OnPrimaryContainerBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "ROLE: EMPLOYEE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnPrimaryContainerBlue
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = OutlineSlate)

                    // Shift Information & Geofence Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${shift?.shiftName ?: "General Shift"} (${shift?.startTime ?: "09:00"} - ${shift?.endTime ?: "18:00"})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isInsideGeofence) StatusSuccessContainer else StatusAbsentContainer
                        ) {
                            Text(
                                text = if (isInsideGeofence) "Office Geofence Active" else "Outside Office",
                                color = if (isInsideGeofence) StatusSuccess else StatusAbsent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Today's Live Attendance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, OutlineSlate)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Attendance Status",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Date: $todayDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (todayRecord?.status) {
                                "PRESENT" -> StatusSuccessContainer
                                "LATE" -> StatusLateContainer
                                "HALF_DAY" -> StatusHalfDayContainer
                                else -> StatusAbsentContainer
                            }
                        ) {
                            Text(
                                text = todayRecord?.status ?: "NOT CHECKED IN",
                                color = when (todayRecord?.status) {
                                    "PRESENT" -> StatusSuccess
                                    "LATE" -> StatusLate
                                    "HALF_DAY" -> StatusHalfDay
                                    else -> StatusAbsent
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Punch stats grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Check In Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceVariantWhite
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Check-In", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = todayRecord?.checkInTime ?: "--:--",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (todayRecord?.checkInTime != null) StatusSuccess else TextSecondary
                                )
                                Text(
                                    text = if ((todayRecord?.lateMinutes ?: 0) > 0) "Late by ${todayRecord?.lateMinutes}m" else "On Time",
                                    fontSize = 10.sp,
                                    color = if ((todayRecord?.lateMinutes ?: 0) > 0) StatusLate else StatusSuccess
                                )
                            }
                        }

                        // Check Out Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceVariantWhite
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Check-Out", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = todayRecord?.checkOutTime ?: "--:--",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (todayRecord?.checkOutTime != null) PrimaryBlue else TextSecondary
                                )
                                Text(
                                    text = if (todayRecord?.checkOutTime != null) "Completed" else "Pending",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Total Work Duration Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceVariantWhite
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Duration", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                val mins = todayRecord?.workingMinutes ?: 0
                                Text(
                                    text = "${mins / 60}h ${mins % 60}m",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Target: 8h 30m",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Punch CTA Button
                    Button(
                        onClick = onNavigateToPunch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("employee_punch_cta"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (todayRecord == null) "Mark Check-In (GPS & Face)" else if (todayRecord.checkOutTime == null) "Mark Check-Out" else "View Punch Details",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. My Leave Balances (3 Quota Cards)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, OutlineSlate)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "My Leave Quotas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Annual allocated leave allowance",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        TextButton(
                            onClick = onNavigateToLeaves,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Apply Leave", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LeaveQuotaTile(
                            title = "Casual",
                            used = leaveBalance?.casualLeaveUsed ?: 0,
                            total = leaveBalance?.casualLeaveTotal ?: 12,
                            color = PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )

                        LeaveQuotaTile(
                            title = "Sick",
                            used = leaveBalance?.sickLeaveUsed ?: 0,
                            total = leaveBalance?.sickLeaveTotal ?: 10,
                            color = StatusLate,
                            modifier = Modifier.weight(1f)
                        )

                        LeaveQuotaTile(
                            title = "Earned",
                            used = leaveBalance?.earnedLeaveUsed ?: 0,
                            total = leaveBalance?.earnedLeaveTotal ?: 18,
                            color = StatusSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Recent Attendance History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Attendance History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${employeeAttendance.size} records",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        if (employeeAttendance.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, OutlineSlate)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Attendance History Yet",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Your daily punch-in, punch-out, and GPS coordinates will appear here once marked.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(employeeAttendance.take(5), key = { it.id }) { record ->
                AttendanceHistoryRow(record = record)
            }
        }

        // 5. Upcoming Company Holidays
        if (holidays.isNotEmpty()) {
            item {
                Text(
                    text = "Upcoming Company Holidays",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(holidays.take(3), key = { it.id }) { holiday ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, OutlineSlate)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryContainerBlue
                            ) {
                                Icon(
                                    Icons.Default.Celebration,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.padding(6.dp).size(18.dp)
                                )
                            }
                            Column {
                                Text(holiday.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text("${holiday.date} • ${holiday.dayOfWeek}", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceVariantWhite
                        ) {
                            Text(
                                text = holiday.type,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaveQuotaTile(
    title: String,
    used: Int,
    total: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val remaining = (total - used).coerceAtLeast(0)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceVariantWhite,
        border = BorderStroke(1.dp, OutlineSlate)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$remaining",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "of $total Left",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun AttendanceHistoryRow(record: AttendanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, OutlineSlate)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = record.date,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "In: ${record.checkInTime ?: "--"} • Out: ${record.checkOutTime ?: "--"}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                if (record.isFaceVerified) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Face, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(12.dp))
                        Text("Face Verified (${record.faceMatchConfidence.toInt()}%)", fontSize = 10.sp, color = StatusSuccess)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (record.status) {
                    "PRESENT" -> StatusSuccessContainer
                    "LATE" -> StatusLateContainer
                    "HALF_DAY" -> StatusHalfDayContainer
                    else -> StatusAbsentContainer
                }
            ) {
                Text(
                    text = record.status,
                    color = when (record.status) {
                        "PRESENT" -> StatusSuccess
                        "LATE" -> StatusLate
                        "HALF_DAY" -> StatusHalfDay
                        else -> StatusAbsent
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
