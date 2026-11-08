package com.example.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaveApplication
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val employees by viewModel.employees.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val leaves by viewModel.leaves.collectAsState()
    val shifts by viewModel.shifts.collectAsState()

    var reviewLeaveDialog by remember { mutableStateOf<LeaveApplication?>(null) }

    val todayDate = remember { DateTimeUtils.getTodayDate() }
    val todayAttendance = remember(allAttendance, todayDate) {
        allAttendance.filter { it.date == todayDate }
    }

    val totalEmployees = employees.count { it.isActive }.coerceAtLeast(1)
    val presentCount = todayAttendance.count { it.status == "PRESENT" }
    val lateCount = todayAttendance.count { it.status == "LATE" }
    val halfDayCount = todayAttendance.count { it.status == "HALF_DAY" }
    val onLeaveCount = leaves.count { it.status == "APPROVED" && it.startDate <= todayDate && it.endDate >= todayDate }
    val absentCount = (totalEmployees - (presentCount + lateCount + halfDayCount + onLeaveCount)).coerceAtLeast(0)

    val pendingLeaves = remember(leaves) {
        leaves.filter { it.status == "PENDING" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Dashboard Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSlate)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryBlue
                            ) {
                                Icon(
                                    Icons.Default.Dashboard,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp).size(18.dp)
                                )
                            }
                            Text(
                                text = "Admin Executive Dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Today: $todayDate • Workforce Pulse & Real-Time Audits",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusSuccess.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "LIVE",
                            color = StatusSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 2. Summary KPI Cards Grid (Present, Late, Half Day, Absent, On Leave, Total)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Present Today",
                        count = presentCount,
                        subtitle = "On-time punches",
                        accentColor = StatusSuccess,
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Late Entries",
                        count = lateCount,
                        subtitle = "Grace exceeded",
                        accentColor = StatusLate,
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Half Day",
                        count = halfDayCount,
                        subtitle = "< 4.5 hours",
                        accentColor = StatusEarlyExit,
                        icon = Icons.Default.Timelapse,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Absent / Pending",
                        count = absentCount,
                        subtitle = "Not checked in",
                        accentColor = StatusAbsent,
                        icon = Icons.Default.Cancel,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "On Leave",
                        count = onLeaveCount,
                        subtitle = "Approved leaves",
                        accentColor = PrimaryBlue,
                        icon = Icons.Default.EventAvailable,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Attendance Status Donut Chart
        item {
            AttendanceDonutChartCard(
                present = presentCount,
                late = lateCount,
                halfDay = halfDayCount,
                absent = absentCount,
                onLeave = onLeaveCount,
                total = totalEmployees
            )
        }

        // 4. Weekly Attendance Trend Bar Chart
        item {
            WeeklyAttendanceTrendCard()
        }

        // 5. Pending Leave Requests Requiring Approval Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pending Leave Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (pendingLeaves.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = StatusLate
                        ) {
                            Text(
                                text = "${pendingLeaves.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Requires Approval",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Pending Leave Cards
        if (pendingLeaves.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSlate)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(36.dp))
                        Text(
                            text = "All Leave Requests Resolved",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "There are no pending leave requests awaiting approval at this time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(pendingLeaves, key = { it.id }) { leave ->
                PendingLeaveApprovalCard(
                    leave = leave,
                    onApprove = { remarks ->
                        viewModel.reviewLeave(leave.id, true, remarks)
                    },
                    onReject = { remarks ->
                        viewModel.reviewLeave(leave.id, false, remarks)
                    }
                )
            }
        }
    }
}

@Composable
fun KpiStatCard(
    title: String,
    count: Int,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSlate),
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
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "$count",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AttendanceDonutChartCard(
    present: Int,
    late: Int,
    halfDay: Int,
    absent: Int,
    onLeave: Int,
    total: Int
) {
    val totalPunched = (present + late + halfDay).coerceAtLeast(0)
    val checkInRate = if (total > 0) ((totalPunched.toFloat() / total.toFloat()) * 100).toInt() else 0

    val slicePresent = if (total > 0) (present.toFloat() / total) * 360f else 0f
    val sliceLate = if (total > 0) (late.toFloat() / total) * 360f else 0f
    val sliceHalfDay = if (total > 0) (halfDay.toFloat() / total) * 360f else 0f
    val sliceLeave = if (total > 0) (onLeave.toFloat() / total) * 360f else 0f
    val sliceAbsent = (360f - (slicePresent + sliceLate + sliceHalfDay + sliceLeave)).coerceAtLeast(0f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSlate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Attendance Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryContainerBlue
                ) {
                    Text(
                        text = "$checkInRate% CHECKED IN",
                        fontWeight = FontWeight.Bold,
                        color = OnPrimaryContainerBlue,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Donut Chart Canvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(150.dp)) {
                        val strokeWidth = 24.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        var startAngle = -90f

                        if (total == 0 || (present == 0 && late == 0 && halfDay == 0 && onLeave == 0)) {
                            drawArc(
                                color = OutlineSlate,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )
                        } else {
                            if (slicePresent > 0) {
                                drawArc(StatusSuccess, startAngle, slicePresent, false, topLeft, arcSize, style = Stroke(strokeWidth))
                                startAngle += slicePresent
                            }
                            if (sliceLate > 0) {
                                drawArc(StatusLate, startAngle, sliceLate, false, topLeft, arcSize, style = Stroke(strokeWidth))
                                startAngle += sliceLate
                            }
                            if (sliceHalfDay > 0) {
                                drawArc(StatusEarlyExit, startAngle, sliceHalfDay, false, topLeft, arcSize, style = Stroke(strokeWidth))
                                startAngle += sliceHalfDay
                            }
                            if (sliceLeave > 0) {
                                drawArc(PrimaryBlue, startAngle, sliceLeave, false, topLeft, arcSize, style = Stroke(strokeWidth))
                                startAngle += sliceLeave
                            }
                            if (sliceAbsent > 0) {
                                drawArc(StatusAbsent.copy(alpha = 0.5f), startAngle, sliceAbsent, false, topLeft, arcSize, style = Stroke(strokeWidth))
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$checkInRate%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Attendance",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Legend Column
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChartLegendItem(color = StatusSuccess, label = "Present", count = present)
                    ChartLegendItem(color = StatusLate, label = "Late", count = late)
                    ChartLegendItem(color = StatusEarlyExit, label = "Half Day", count = halfDay)
                    ChartLegendItem(color = PrimaryBlue, label = "On Leave", count = onLeave)
                    ChartLegendItem(color = StatusAbsent.copy(alpha = 0.6f), label = "Absent", count = absent)
                }
            }
        }
    }
}

@Composable
fun ChartLegendItem(color: Color, label: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = "$label ($count)",
            fontSize = 12.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun WeeklyAttendanceTrendCard() {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val rates = listOf(92, 88, 95, 91, 84, 45, 20)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSlate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Attendance Trend (%)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Avg: 89% Weekdays",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            // Custom Bar Chart on Canvas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEachIndexed { index, day ->
                    val rate = rates[index]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$rate%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rate >= 80) PrimaryBlue else TextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((rate * 0.85).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (index == 4) StatusSuccess else if (rate >= 80) PrimaryBlue else OutlineSlate
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PendingLeaveApprovalCard(
    leave: LeaveApplication,
    onApprove: (remarks: String) -> Unit,
    onReject: (remarks: String) -> Unit
) {
    var remarksText by remember { mutableStateOf("Approved by Admin") }
    var showRemarksInput by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = leave.employeeName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${leave.employeeCode} • Applied on ${leave.appliedDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (leave.leaveType) {
                        "CASUAL" -> PrimaryContainerBlue
                        "SICK" -> SecondaryContainerTeal
                        else -> TertiaryContainerAmber
                    }
                ) {
                    Text(
                        text = "${leave.leaveType} (${leave.numberOfDays} ${if (leave.numberOfDays == 1) "Day" else "Days"})",
                        fontWeight = FontWeight.Bold,
                        color = when (leave.leaveType) {
                            "CASUAL" -> OnPrimaryContainerBlue
                            "SICK" -> OnSecondaryContainerTeal
                            else -> OnTertiaryContainerAmber
                        },
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Duration & Reason
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceVariantWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                        Text(
                            text = "${leave.startDate}  to  ${leave.endDate}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Reason: ${leave.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            if (showRemarksInput) {
                OutlinedTextField(
                    value = remarksText,
                    onValueChange = { remarksText = it },
                    label = { Text("Manager Review Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Action Buttons (Approve / Reject)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (!showRemarksInput) {
                            remarksText = "Rejected by Admin"
                            showRemarksInput = true
                        } else {
                            onReject(remarksText)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAbsent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusAbsent)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onApprove(remarksText)
                    },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Approve & Notify", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
