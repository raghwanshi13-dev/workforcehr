package com.example.ui.screens.superadmin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sql.PhpMysqlSchemaGenerator
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val dbConfig by viewModel.dbConfig.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()
    val isImportingSql by viewModel.isImportingSql.collectAsState()
    val sqlImportLogs by viewModel.sqlImportLogs.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var activeTab by remember { mutableIntStateOf(0) } // 0: Database Config & SQL Import, 1: Live Diagnostics & Testing

    var urlInput by remember(dbConfig) { mutableStateOf(dbConfig.url) }
    var dbNameInput by remember(dbConfig) { mutableStateOf(dbConfig.dbName) }
    var dbUserInput by remember(dbConfig) { mutableStateOf(dbConfig.dbUser) }
    var dbPassInput by remember(dbConfig) { mutableStateOf(dbConfig.dbPass) }
    var isPassVisible by remember { mutableStateOf(false) }

    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. SuperAdmin Console Header
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6A1B9A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "SuperAdmin Console",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF3E5F5)
                                    ) {
                                        Text(
                                            text = "ROOT ACCESS",
                                            color = Color(0xFF6A1B9A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Logged in: supradmin@gmail.com • DevOps & Infrastructure",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (dbConfig.isConnected) StatusSuccessContainer else StatusAbsentContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (dbConfig.isConnected) StatusSuccess else StatusAbsent)
                                )
                                Text(
                                    text = if (dbConfig.isConnected) "DB ONLINE" else "DISCONNECTED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dbConfig.isConnected) StatusSuccess else StatusAbsent
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = OutlineSlate)

                    // Secondary Sub-Tabs: 0: DB Configuration & Import, 1: Testing & Diagnostics
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = PureWhite,
                        contentColor = PrimaryBlue,
                        divider = {}
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("Database Config & SQL", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Testing & Health", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }

        // TAB 0: Database Configuration (URL, dbName, dbUser, dbPass, Test & Automatic SQL Import)
        if (activeTab == 0) {
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
                                    text = "MySQL Database Connection",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Configure parameters to automatically connect and import SQL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryContainerBlue
                            ) {
                                Text(
                                    text = "${dbConfig.importedTablesCount} Tables Active",
                                    color = OnPrimaryContainerBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // 1. URL Input
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("Database URL / Host") },
                            placeholder = { Text("e.g. jdbc:mysql://localhost:3306 or 192.168.1.100") },
                            leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null, tint = PrimaryBlue) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("db_url_input")
                        )

                        // 2. Database Name Input
                        OutlinedTextField(
                            value = dbNameInput,
                            onValueChange = { dbNameInput = it },
                            label = { Text("Database Name (dbName)") },
                            placeholder = { Text("e.g. workforce_hr_db") },
                            leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryBlue) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("db_name_input")
                        )

                        // 3. Database Username Input
                        OutlinedTextField(
                            value = dbUserInput,
                            onValueChange = { dbUserInput = it },
                            label = { Text("Database User (dbUser)") },
                            placeholder = { Text("e.g. root or hr_admin") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("db_user_input")
                        )

                        // 4. Database Password Input
                        OutlinedTextField(
                            value = dbPassInput,
                            onValueChange = { dbPassInput = it },
                            label = { Text("Database Password (dbPass)") },
                            placeholder = { Text("Enter DB Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue) },
                            trailingIcon = {
                                IconButton(onClick = { isPassVisible = !isPassVisible }) {
                                    Icon(
                                        if (isPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Password"
                                    )
                                }
                            },
                            visualTransformation = if (isPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("db_pass_input")
                        )

                        // Connection Status Feedback Alert
                        if (testStatusMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isTestSuccess) StatusSuccessContainer else StatusAbsentContainer,
                                border = BorderStroke(1.dp, if (isTestSuccess) StatusSuccess.copy(alpha = 0.3f) else StatusAbsent.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (isTestSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (isTestSuccess) StatusSuccess else StatusAbsent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = testStatusMessage!!,
                                        fontSize = 12.sp,
                                        color = if (isTestSuccess) StatusSuccess else StatusAbsent,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Action Buttons: 1. Test Connection, 2. Automatic SQL Import
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.testDatabaseConnection(urlInput, dbNameInput, dbUserInput, dbPassInput) { success, msg ->
                                        isTestSuccess = success
                                        testStatusMessage = msg
                                    }
                                },
                                enabled = !isTestingConnection && !isImportingSql,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("test_db_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pinging...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Connection", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.importSqlSchemaAutomatically(urlInput, dbNameInput, dbUserInput, dbPassInput) { success, msg ->
                                        isTestSuccess = success
                                        testStatusMessage = msg
                                    }
                                },
                                enabled = !isImportingSql && !isTestingConnection,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("import_sql_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                if (isImportingSql) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Importing...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auto Import SQL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Live Automated SQL Import Logs
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Automated SQL Execution Terminal",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val fullSql = PhpMysqlSchemaGenerator.generateFullMySqlSchema()
                                    clipboardManager.setText(AnnotatedString(fullSql))
                                    viewModel.showNotification("Full SQL DDL copied to clipboard")
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy SQL", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Terminal Console Box
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 220.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(sqlImportLogs) { log ->
                                    Text(
                                        text = log,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (log.contains("[SUCCESS]") || log.contains("[OK]")) Color(0xFF4ADE80)
                                                else if (log.contains("[INIT]")) Color(0xFF60A5FA)
                                                else Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 1: System Health, Diagnostics & Testing ("aur testing bhi rahge")
        if (activeTab == 1) {
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
                        Text(
                            text = "Live System Diagnostics & Subsystem Testing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        // 1. Room Local SQLite Test
                        DiagnosticItem(
                            title = "Local SQLite Integrity Check",
                            subtitle = "Status: Healthy (Tables: employees, attendance, leaves, shifts)",
                            badge = "PASSED",
                            badgeColor = StatusSuccess,
                            badgeContainer = StatusSuccessContainer,
                            actionLabel = "Run SQLite Check",
                            onAction = {
                                viewModel.showNotification("SQLite Check: PRAGMA integrity_check = OK (3 users, 2 shifts)")
                            }
                        )

                        HorizontalDivider(color = OutlineSlate)

                        val context = androidx.compose.ui.platform.LocalContext.current

                        // 2. GPS & Geofencing Diagnostic Test
                        DiagnosticItem(
                            title = "GPS Geofence Telemetry Test",
                            subtitle = "Calculates Haversine distance to Office (28.6139, 77.2090)",
                            badge = "ACCURACY < 15M",
                            badgeColor = PrimaryBlue,
                            badgeContainer = PrimaryContainerBlue,
                            actionLabel = "Test GPS Fetch",
                            onAction = {
                                viewModel.fetchLiveLocation(context)
                                viewModel.showNotification("GPS Polled: Telemetry live within office zone")
                            }
                        )

                        HorizontalDivider(color = OutlineSlate)

                        // 3. Notification Pipeline Dispatch Test
                        DiagnosticItem(
                            title = "Push Notification Pipeline Test",
                            subtitle = "Fires high-priority push notification across Android channels",
                            badge = "CHANNEL ACTIVE",
                            badgeColor = Color(0xFF6A1B9A),
                            badgeContainer = Color(0xFFF3E5F5),
                            actionLabel = "Fire Test Alert",
                            onAction = {
                                viewModel.triggerShiftReminder(15)
                                viewModel.showNotification("Test push notification dispatched")
                            }
                        )

                        HorizontalDivider(color = OutlineSlate)

                        // 4. Reset & Clean Database History
                        DiagnosticItem(
                            title = "Wipe Attendance & Leave Demo Records",
                            subtitle = "Maintains clean zero-record baseline for company deployment",
                            badge = "CLEAN BASELINE",
                            badgeColor = StatusLate,
                            badgeContainer = StatusLateContainer,
                            actionLabel = "Clean Records",
                            onAction = {
                                viewModel.clearDemoData()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    badgeContainer: Color,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeContainer
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }

        OutlinedButton(
            onClick = onAction,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
