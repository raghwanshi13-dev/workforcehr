package com.example.ui.screens.attendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AttendanceRecord
import com.example.ui.LiveAttendanceState
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.DateTimeUtils
import com.example.util.GeoFenceUtils
import com.example.util.GeoLocationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveState by viewModel.liveState.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val simulateOutside by viewModel.simulateOutsideOffice.collectAsState()
    val simulateMock by viewModel.simulateMockGps.collectAsState()

    var showSelfieScannerDialog by remember { mutableStateOf(false) }
    var showCorrectionDialog by remember { mutableStateOf<AttendanceRecord?>(null) }

    // Runtime Permission Request for GPS Location & Camera
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            viewModel.fetchLiveLocation(context)
        }
    }

    // Auto-fetch location on first launch if permission already granted
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            viewModel.fetchLiveLocation(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        }
    }

    val myAttendance = remember(allAttendance, liveState.currentEmployee?.id) {
        val empId = liveState.currentEmployee?.id ?: 1L
        allAttendance.filter { it.employeeId == empId }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Status Hero Card
        item {
            LiveAttendanceHeroCard(
                liveState = liveState,
                onCheckInClick = {
                    // Fetch location first or refresh, then open verification
                    viewModel.fetchLiveLocation(context) {
                        showSelfieScannerDialog = true
                    }
                },
                onCheckOutClick = { viewModel.checkOut() },
                onToggleBreak = { viewModel.toggleBreak() }
            )
        }

        // 2. Real-Time Geo Fetching & GPS Telemetry Card
        item {
            LiveGeoFetchingCard(
                liveState = liveState,
                isFetching = liveState.isFetchingLocation,
                onRefreshGps = {
                    if (hasLocationPermission) {
                        viewModel.fetchLiveLocation(context)
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                        )
                    }
                },
                simulateOutside = simulateOutside,
                simulateMock = simulateMock,
                onToggleGeofenceSim = { viewModel.toggleGeofenceSimulation() },
                onToggleMockSim = { viewModel.toggleMockGpsSimulation() }
            )
        }

        // 3. Shift & Policy Metrics Card
        item {
            ShiftAndWorkingMetricsCard(
                liveState = liveState,
                onTriggerShiftAlert = { viewModel.triggerShiftReminder(15) }
            )
        }

        // 4. Attendance History Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${myAttendance.size} Recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // 5. History Items or Clean State
        if (myAttendance.isEmpty()) {
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
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryContainerBlue
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier
                                    .padding(14.dp)
                                    .size(32.dp)
                            )
                        }
                        Text(
                            text = "Clean State: No Demo Records",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap 'Verify Face & Mark Attendance' above to record your first real GPS and Face-verified attendance punch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(myAttendance, key = { it.id }) { record ->
                AttendanceHistoryCard(
                    record = record,
                    onRequestCorrection = { showCorrectionDialog = record }
                )
            }
        }
    }

    // Live Face Verification & Geo-Mark Attendance Modal
    if (showSelfieScannerDialog) {
        LiveFaceAndGeoVerificationDialog(
            liveState = liveState,
            onDismiss = { showSelfieScannerDialog = false },
            onConfirmAttendance = { faceConfidence, locResult ->
                viewModel.markAttendanceWithFaceAndGeo(
                    faceConfidence = faceConfidence,
                    locationResult = locResult,
                    onSuccess = {
                        showSelfieScannerDialog = false
                    }
                )
            }
        )
    }

    // Attendance Correction Request Dialog
    showCorrectionDialog?.let { record ->
        AttendanceCorrectionDialog(
            record = record,
            onDismiss = { showCorrectionDialog = null },
            onSubmit = { inTime, outTime, reason ->
                viewModel.submitCorrection(
                    attendanceId = record.id,
                    date = record.date,
                    originalCheckIn = record.checkInTime ?: "09:00:00",
                    originalCheckOut = record.checkOutTime ?: "18:00:00",
                    requestedCheckIn = inTime,
                    requestedCheckOut = outTime,
                    reason = reason,
                    onSuccess = { showCorrectionDialog = null }
                )
            }
        )
    }
}

@Composable
fun LiveAttendanceHeroCard(
    liveState: LiveAttendanceState,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    onToggleBreak: () -> Unit
) {
    val todayRecord = liveState.todayRecord
    val isCheckedIn = todayRecord != null && todayRecord.checkInTime != null
    val isCheckedOut = todayRecord?.checkOutTime != null
    val isOnBreak = todayRecord?.isOnBreak == true

    val statusTitle = when {
        isCheckedOut -> "Checked Out"
        isOnBreak -> "On Break"
        isCheckedIn -> "Checked In"
        else -> "Ready To Punch"
    }

    val badgeColor = when {
        isCheckedOut -> MaterialTheme.colorScheme.outline
        isOnBreak -> TertiaryAmber
        isCheckedIn -> StatusSuccess
        else -> PrimaryBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PrimaryBlue.copy(alpha = 0.05f),
                            PureWhite
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Top Row: Status badge and Today's Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(badgeColor, CircleShape)
                            )
                            Text(
                                text = statusTitle.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }

                    Text(
                        text = DateTimeUtils.getTodayDate(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Center: Time display & Employee name
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = liveState.currentEmployee?.fullName ?: "Staff Member",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${liveState.currentEmployee?.empCode ?: ""} • ${liveState.currentEmployee?.designation ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Punch In/Out Timestamps preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CHECK-IN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = todayRecord?.checkInTime ?: "--:--:--",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (todayRecord?.checkInTime != null) StatusSuccess else TextPrimary
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(36.dp), color = OutlineSlate)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CHECK-OUT",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = todayRecord?.checkOutTime ?: "--:--:--",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (todayRecord?.checkOutTime != null) PrimaryBlue else TextPrimary
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(36.dp), color = OutlineSlate)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "HOURS WORKED",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = if (todayRecord != null && todayRecord.workingMinutes > 0)
                                    DateTimeUtils.formatDuration(todayRecord.workingMinutes)
                                else if (isCheckedIn && !isCheckedOut) "In Progress" else "--",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isCheckedIn) {
                        Button(
                            onClick = onCheckInClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("check_in_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify Face & Mark Attendance", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    } else if (!isCheckedOut) {
                        // Break button
                        OutlinedButton(
                            onClick = onToggleBreak,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("break_toggle_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isOnBreak) StatusSuccess else TertiaryAmber
                            )
                        ) {
                            Icon(
                                if (isOnBreak) Icons.Default.PlayArrow else Icons.Default.Coffee,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnBreak) "Resume Work" else "Take Break",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Check-Out button
                        Button(
                            onClick = onCheckOutClick,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(52.dp)
                                .testTag("check_out_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Punch Out", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = StatusSuccess.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attendance Completed for Today",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusSuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveGeoFetchingCard(
    liveState: LiveAttendanceState,
    isFetching: Boolean,
    onRefreshGps: () -> Unit,
    simulateOutside: Boolean,
    simulateMock: Boolean,
    onToggleGeofenceSim: () -> Unit,
    onToggleMockSim: () -> Unit
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Live GPS & Geo-Fence Telemetry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onRefreshGps,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh GPS", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(color = OutlineSlate)

            // Live Coordinates display
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariantWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT COORDINATES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = "Lat: ${String.format("%.5f", liveState.currentLatitude)}, Lng: ${String.format("%.5f", liveState.currentLongitude)}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Accuracy: ±${liveState.currentAccuracy.toInt()}m • Distance to HQ: ${liveState.currentDistanceMeters.toInt()}m",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (liveState.isInsideGeofence) StatusSuccess.copy(alpha = 0.15f) else StatusAbsent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (liveState.isInsideGeofence) "IN GEOFENCE" else "OUTSIDE",
                            color = if (liveState.isInsideGeofence) StatusSuccess else StatusAbsent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Quick Telemetry Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleGeofenceSim,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(if (simulateOutside) "Simulate Inside" else "Simulate Outside", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onToggleMockSim,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(if (simulateMock) "Clear Mock GPS" else "Test Mock GPS", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ShiftAndWorkingMetricsCard(
    liveState: LiveAttendanceState,
    onTriggerShiftAlert: () -> Unit = {}
) {
    val shift = liveState.currentShift
    val record = liveState.todayRecord

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
                    text = "Shift & Late Marking Rules",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryContainerBlue
                ) {
                    Text(
                        text = shift?.shiftName ?: "General Morning Shift",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimaryContainerBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Working Hours", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("${shift?.startTime ?: "09:00"} - ${shift?.endTime ?: "18:00"}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }

                Column {
                    Text("Grace Period", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("${shift?.gracePeriodMinutes ?: 15} mins", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = StatusLate)
                }

                Column {
                    Text("Break Allowance", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("${shift?.breakDurationMinutes ?: 45} mins", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(color = OutlineSlate)

            // Push Notification Alert Reminder Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryContainerBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Shift Alert: Push notification active 15m prior to ${shift?.startTime ?: "09:00"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnPrimaryContainerBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                FilledTonalButton(
                    onClick = onTriggerShiftAlert,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("test_shift_alert_button")
                ) {
                    Text("Test Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AttendanceHistoryCard(
    record: AttendanceRecord,
    onRequestCorrection: () -> Unit
) {
    val statusColor = when (record.status) {
        "PRESENT" -> StatusSuccess
        "LATE" -> StatusLate
        "HALF_DAY" -> StatusEarlyExit
        "ABSENT" -> StatusAbsent
        else -> PrimaryBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSlate)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PrimaryBlue
                    )
                    Text(
                        text = record.date,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = record.status.replace("_", " "),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Timings & Face Match
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "In: ${record.checkInTime ?: "--"}  •  Out: ${record.checkOutTime ?: "--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
                Text(
                    text = "Face: ${record.faceMatchConfidence.toInt()}% Verified",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusSuccess
                )
            }

            // Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TextSecondary
                    )
                    Text(
                        text = record.locationAddress,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }

                TextButton(
                    onClick = onRequestCorrection,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Correction", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun LiveFaceAndGeoVerificationDialog(
    liveState: LiveAttendanceState,
    onDismiss: () -> Unit,
    onConfirmAttendance: (faceConfidence: Float, locationResult: GeoLocationResult?) -> Unit
) {
    var isScanning by remember { mutableStateOf(true) }
    var faceConfidence by remember { mutableFloatStateOf(98.6f) }

    // Animated scanning line
    val infiniteTransition = rememberInfiniteTransition(label = "face_scan")
    val scanLineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_offset"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Face, contentDescription = null, tint = PrimaryBlue)
                Text("Face & GPS Attendance Scan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Interactive Camera Scanning Viewfinder Box
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    // Face Silhouette
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Color.White.copy(alpha = 0.85f)
                    )

                    // Target Reticle Box
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .border(2.5.dp, StatusSuccess, RoundedCornerShape(14.dp))
                    )

                    // Live animated scanning beam
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(2.5.dp)
                            .offset(y = (-60 + (120 * scanLineOffset)).dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, StatusSuccess, Color.Transparent)
                                )
                            )
                    )

                    // Top Liveness & Camera FPS Indicator
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "FRONT CAMERA • 30 FPS",
                            color = StatusSuccess,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Bottom Match Score
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = StatusSuccess
                    ) {
                        Text(
                            text = "FACE MATCH: ${faceConfidence.toInt()}% VERIFIED",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Live Geo-Coordinates Telemetry Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceVariantWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "GEO FETCHING ACQUIRED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Text(
                                text = if (liveState.isInsideGeofence) "GEOFENCE PASS" else "OUTSIDE RADIUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (liveState.isInsideGeofence) StatusSuccess else StatusAbsent
                            )
                        }
                        Text(
                            text = "Lat: ${String.format("%.5f", liveState.currentLatitude)}, Lng: ${String.format("%.5f", liveState.currentLongitude)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Distance to HQ: ${liveState.currentDistanceMeters.toInt()}m (Max 250m) • Device: ${liveState.currentDeviceModel}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmAttendance(faceConfidence, null)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm & Mark Attendance", fontWeight = FontWeight.Bold)
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
fun AttendanceCorrectionDialog(
    record: AttendanceRecord,
    onDismiss: () -> Unit,
    onSubmit: (inTime: String, outTime: String, reason: String) -> Unit
) {
    var requestedIn by remember { mutableStateOf(record.checkInTime ?: "09:00:00") }
    var requestedOut by remember { mutableStateOf(record.checkOutTime ?: "18:00:00") }
    var reason by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Manual Attendance Correction", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Request regularization for Date: ${record.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = requestedIn,
                    onValueChange = { requestedIn = it },
                    label = { Text("Requested Check-In (HH:mm:ss)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = requestedOut,
                    onValueChange = { requestedOut = it },
                    label = { Text("Requested Check-Out (HH:mm:ss)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Correction") },
                    placeholder = { Text("e.g. Biometric terminal offline, On-duty client visit") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                if (errorText != null) {
                    Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        errorText = "Please enter reason for correction"
                    } else {
                        onSubmit(requestedIn, requestedOut, reason)
                    }
                }
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
