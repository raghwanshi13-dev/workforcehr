package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.attendance.AttendanceScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.dashboard.AdminDashboardScreen
import com.example.ui.screens.dashboard.EmployeeDashboardScreen
import com.example.ui.screens.employee.EmployeeScreen
import com.example.ui.screens.leave.LeaveScreen
import com.example.ui.screens.shifts.ShiftScreen
import com.example.ui.screens.sqlgenerator.SqlSchemaScreen
import com.example.ui.screens.superadmin.SuperAdminDashboardScreen
import com.example.ui.theme.*
import com.example.util.AppNotification
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkforceTheme(darkTheme = false) {
                WorkforceRoot()
            }
        }
    }
}

@Composable
fun WorkforceRoot(viewModel: MainViewModel = viewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(viewModel = viewModel)
    } else {
        WorkforceApp(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkforceApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val notification by viewModel.notification.collectAsState()
    val liveState by viewModel.liveState.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val isSuperAdminMode by viewModel.isSuperAdminMode.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val pushNotifications by viewModel.notifications.collectAsState()

    var showNotificationCenterDialog by remember { mutableStateOf(false) }

    // Request Notification Runtime Permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showNotification("Push notifications enabled")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Handle snackbar notifications
    LaunchedEffect(notification) {
        notification?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it.message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearNotification()
            }
        }
    }

    // Back button returns to Attendance if on other tabs
    BackHandler(enabled = currentTab != 0) {
        currentTab = 0
    }

    val currentRole = liveState.currentEmployee?.getRoleDisplayName() ?: if (isSuperAdminMode) "SUPERADMIN" else if (isAdminMode) "ADMIN" else "STAFF"
    val badgeBgColor = when (currentRole) {
        "SUPERADMIN" -> Color(0xFFF3E5F5)
        "ADMIN" -> TertiaryContainerAmber
        else -> PrimaryContainerBlue
    }
    val badgeTextColor = when (currentRole) {
        "SUPERADMIN" -> Color(0xFF6A1B9A)
        "ADMIN" -> OnTertiaryContainerAmber
        else -> OnPrimaryContainerBlue
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlue
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WorkforceHR",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeBgColor
                                ) {
                                    Text(
                                        text = currentRole,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = badgeTextColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${liveState.currentEmployee?.fullName ?: "Staff"} (${liveState.currentEmployee?.empCode ?: ""})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // Push Notification Bell Icon with Badge Counter
                    IconButton(
                        onClick = { showNotificationCenterDialog = true },
                        modifier = Modifier.testTag("notification_center_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (pushNotifications.isNotEmpty()) {
                                    Badge(containerColor = PrimaryBlue) {
                                        Text("${pushNotifications.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (pushNotifications.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                                contentDescription = "Push Alerts",
                                tint = if (pushNotifications.isNotEmpty()) PrimaryBlue else TextPrimary
                            )
                        }
                    }

                    // Logout Button back to Login Screen (User switch completely removed per instruction)
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = StatusAbsent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = PureWhite,
                tonalElevation = 2.dp
            ) {
                if (isSuperAdminMode) {
                    // SuperAdmin View Tabs (DB Config, Admin Dash, Attendance, Leaves, Staff, Shifts)
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(
                                if (currentTab == 0) Icons.Default.Storage else Icons.Outlined.Storage,
                                contentDescription = "DB Config"
                            )
                        },
                        label = { Text("DB Config") },
                        modifier = Modifier.testTag("nav_db_config")
                    )

                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Icon(
                                if (currentTab == 1) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Admin Dash"
                            )
                        },
                        label = { Text("Admin Dash") },
                        modifier = Modifier.testTag("nav_admin_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            Icon(
                                if (currentTab == 2) Icons.Default.Fingerprint else Icons.Outlined.Fingerprint,
                                contentDescription = "Punch"
                            )
                        },
                        label = { Text("Punch") },
                        modifier = Modifier.testTag("nav_attendance")
                    )

                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = {
                            Icon(
                                if (currentTab == 3) Icons.Default.EventBusy else Icons.Outlined.EventBusy,
                                contentDescription = "Leaves"
                            )
                        },
                        label = { Text("Leaves") },
                        modifier = Modifier.testTag("nav_leaves")
                    )

                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { currentTab = 4 },
                        icon = {
                            Icon(
                                if (currentTab == 4) Icons.Default.People else Icons.Outlined.People,
                                contentDescription = "Staff"
                            )
                        },
                        label = { Text("Staff") },
                        modifier = Modifier.testTag("nav_employees")
                    )

                    NavigationBarItem(
                        selected = currentTab == 5,
                        onClick = { currentTab = 5 },
                        icon = {
                            Icon(
                                if (currentTab == 5) Icons.Default.Schedule else Icons.Outlined.Schedule,
                                contentDescription = "Shifts"
                            )
                        },
                        label = { Text("Shifts") },
                        modifier = Modifier.testTag("nav_shifts")
                    )
                } else if (isAdminMode) {
                    // Admin View Tabs
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(
                                if (currentTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Admin Dashboard"
                            )
                        },
                        label = { Text("Admin Dash") },
                        modifier = Modifier.testTag("nav_admin_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Icon(
                                if (currentTab == 1) Icons.Default.Fingerprint else Icons.Outlined.Fingerprint,
                                contentDescription = "Attendance"
                            )
                        },
                        label = { Text("Punch") },
                        modifier = Modifier.testTag("nav_attendance")
                    )

                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            Icon(
                                if (currentTab == 2) Icons.Default.EventBusy else Icons.Outlined.EventBusy,
                                contentDescription = "Leaves"
                            )
                        },
                        label = { Text("Leaves") },
                        modifier = Modifier.testTag("nav_leaves")
                    )

                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = {
                            Icon(
                                if (currentTab == 3) Icons.Default.People else Icons.Outlined.People,
                                contentDescription = "Staff"
                            )
                        },
                        label = { Text("Staff") },
                        modifier = Modifier.testTag("nav_employees")
                    )

                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { currentTab = 4 },
                        icon = {
                            Icon(
                                if (currentTab == 4) Icons.Default.Schedule else Icons.Outlined.Schedule,
                                contentDescription = "Shifts"
                            )
                        },
                        label = { Text("Shifts") },
                        modifier = Modifier.testTag("nav_shifts")
                    )

                    NavigationBarItem(
                        selected = currentTab == 5,
                        onClick = { currentTab = 5 },
                        icon = {
                            Icon(
                                if (currentTab == 5) Icons.Default.DataObject else Icons.Outlined.DataObject,
                                contentDescription = "SQL"
                            )
                        },
                        label = { Text("SQL") },
                        modifier = Modifier.testTag("nav_sql")
                    )
                } else {
                    // Employee View Tabs
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(
                                if (currentTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "My Dashboard"
                            )
                        },
                        label = { Text("My Dash") },
                        modifier = Modifier.testTag("nav_employee_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Icon(
                                if (currentTab == 1) Icons.Default.Fingerprint else Icons.Outlined.Fingerprint,
                                contentDescription = "Punch"
                            )
                        },
                        label = { Text("Punch") },
                        modifier = Modifier.testTag("nav_attendance")
                    )

                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            Icon(
                                if (currentTab == 2) Icons.Default.EventBusy else Icons.Outlined.EventBusy,
                                contentDescription = "My Leaves"
                            )
                        },
                        label = { Text("My Leaves") },
                        modifier = Modifier.testTag("nav_leaves")
                    )

                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = {
                            Icon(
                                if (currentTab == 3) Icons.Default.Schedule else Icons.Outlined.Schedule,
                                contentDescription = "My Shift"
                            )
                        },
                        label = { Text("My Shift") },
                        modifier = Modifier.testTag("nav_shifts")
                    )

                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { currentTab = 4 },
                        icon = {
                            Icon(
                                if (currentTab == 4) Icons.Default.DataObject else Icons.Outlined.DataObject,
                                contentDescription = "SQL"
                            )
                        },
                        label = { Text("SQL") },
                        modifier = Modifier.testTag("nav_sql")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSuperAdminMode) {
                when (currentTab) {
                    0 -> SuperAdminDashboardScreen(viewModel = viewModel)
                    1 -> AdminDashboardScreen(viewModel = viewModel)
                    2 -> AttendanceScreen(viewModel = viewModel)
                    3 -> LeaveScreen(viewModel = viewModel)
                    4 -> EmployeeScreen(viewModel = viewModel)
                    5 -> ShiftScreen(viewModel = viewModel)
                    else -> SuperAdminDashboardScreen(viewModel = viewModel)
                }
            } else if (isAdminMode) {
                when (currentTab) {
                    0 -> AdminDashboardScreen(viewModel = viewModel)
                    1 -> AttendanceScreen(viewModel = viewModel)
                    2 -> LeaveScreen(viewModel = viewModel)
                    3 -> EmployeeScreen(viewModel = viewModel)
                    4 -> ShiftScreen(viewModel = viewModel)
                    5 -> SqlSchemaScreen(viewModel = viewModel)
                    else -> AdminDashboardScreen(viewModel = viewModel)
                }
            } else {
                when (currentTab) {
                    0 -> EmployeeDashboardScreen(
                        viewModel = viewModel,
                        onNavigateToPunch = { currentTab = 1 },
                        onNavigateToLeaves = { currentTab = 2 }
                    )
                    1 -> AttendanceScreen(viewModel = viewModel)
                    2 -> LeaveScreen(viewModel = viewModel)
                    3 -> ShiftScreen(viewModel = viewModel)
                    4 -> SqlSchemaScreen(viewModel = viewModel)
                    else -> EmployeeDashboardScreen(
                        viewModel = viewModel,
                        onNavigateToPunch = { currentTab = 1 },
                        onNavigateToLeaves = { currentTab = 2 }
                    )
                }
            }
        }
    }

    // In-App Push Notification Center Modal
    if (showNotificationCenterDialog) {
        NotificationCenterDialog(
            notifications = pushNotifications,
            onDismiss = { showNotificationCenterDialog = false },
            onClearAll = {
                viewModel.clearNotifications()
                showNotificationCenterDialog = false
            },
            onTestShiftAlert = {
                viewModel.triggerShiftReminder(15)
            }
        )
    }
}

@Composable
fun NotificationCenterDialog(
    notifications: List<AppNotification>,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    onTestShiftAlert: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PrimaryBlue)
                    Text("Employee Push Alerts", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                if (notifications.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear", fontSize = 12.sp, color = StatusAbsent)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Trigger Shift Alert
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryContainerBlue.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shift Start Reminder", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnPrimaryContainerBlue)
                            Text("Alert sent 15 mins before scheduled shift.", fontSize = 11.sp, color = TextSecondary)
                        }
                        FilledTonalButton(
                            onClick = onTestShiftAlert,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Trigger Alert", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                            Text("No alerts yet.", fontWeight = FontWeight.Medium, color = TextSecondary)
                            Text("You will receive notifications when leave requests are reviewed or when your shift approaches.", fontSize = 11.sp, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(notifications, key = { it.id }) { item ->
                            val icon = if (item.type == "LEAVE") Icons.Default.EventAvailable else Icons.Default.Alarm
                            val iconColor = if (item.type == "LEAVE") SecondaryTeal else TertiaryAmber

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceVariantWhite,
                                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSlate),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(iconColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                            Text(item.timestamp, fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(item.message, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
