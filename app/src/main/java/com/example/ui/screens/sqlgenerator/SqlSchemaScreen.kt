package com.example.ui.screens.sqlgenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SqlSchemaScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: MySQL DDL, 1: db_connect.php, 2: api_attendance.php, 3: Schema Tables

    val fullSql = remember { viewModel.getFullSqlSchema() }
    val phpDb = remember { viewModel.getPhpConnectionSnippet() }
    val phpAttendance = remember { viewModel.getPhpAttendanceApiSnippet() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Executive Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlue.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                Icons.Default.DataObject,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "PHP & MySQL Schema Initializer",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Auto-generated SQL Queries & PHP PDO Backend",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusSuccess.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "READY",
                            color = StatusSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Complete database architecture containing 8 tables, foreign keys, indexes, and ready-to-run PHP endpoints for Geofencing, Live Attendance, Fake GPS checks, and Leave Balances.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Navigation Tabs
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("MySQL DDL (.sql)", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("db_connect.php", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("api_attendance.php", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Tables & Schema Overview", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content Area based on selected Tab
        when (selectedTab) {
            0 -> CodeViewer(
                title = "MySQL Initialization DDL (DDL + Constraints + Seed Data)",
                code = fullSql,
                fileExtension = ".sql",
                onCopy = {
                    copyToClipboard(context, fullSql)
                    viewModel.showNotification("Full MySQL Schema SQL copied to clipboard!")
                }
            )

            1 -> CodeViewer(
                title = "PHP PDO Database Connection (db_connect.php)",
                code = phpDb,
                fileExtension = ".php",
                onCopy = {
                    copyToClipboard(context, phpDb)
                    viewModel.showNotification("db_connect.php snippet copied!")
                }
            )

            2 -> CodeViewer(
                title = "PHP Attendance API with Geofence & Mock GPS Check (api_attendance.php)",
                code = phpAttendance,
                fileExtension = ".php",
                onCopy = {
                    copyToClipboard(context, phpAttendance)
                    viewModel.showNotification("api_attendance.php code copied!")
                }
            )

            3 -> SchemaTablesOverview()
        }
    }
}

@Composable
fun CodeViewer(
    title: String,
    code: String,
    fileExtension: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(StatusSuccess, androidx.compose.foundation.shape.CircleShape)
                    )
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onCopy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("copy_sql_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy $fileExtension", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Code Block
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = code,
                    color = Color(0xFFE2E8F0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SchemaTablesOverview() {
    val tables = listOf(
        Pair("departments", "Organizational units: Engineering, HR, Product, Operations, QA"),
        Pair("designations", "Job titles mapped to departments with grade levels (L1-L5)"),
        Pair("shifts", "Shift timings, grace period (15m), break duration, weekly offs"),
        Pair("employees", "Profiles, IDs, mobile, email, designation, active status, device ID"),
        Pair("employee_documents", "Document attachments (Aadhaar, Degree, Offer Letter)"),
        Pair("attendance", "Live punches, Geofence coords, selfie match %, fake GPS flag, hours"),
        Pair("attendance_corrections", "Manual regularization requests with manager approvals"),
        Pair("leave_types", "Leave categories (Casual, Sick, Earned, Comp-Off)"),
        Pair("leave_balances", "Casual (CL: 12), Sick (SL: 10), Earned (EL: 18) quotas"),
        Pair("leave_applications", "Online submissions, date ranges, reasons, approval workflow"),
        Pair("holiday_schedules", "National & corporate holiday calendar with date and day"),
        Pair("vw_daily_attendance_summary", "SQL View: Executive present, late, absent counts per day"),
        Pair("sp_record_attendance_punch", "Stored Procedure: Validates GPS, Geofence, and Shift Late rules")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Database Structure & Table Dictionary (workforce_hr_db)",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        tables.forEachIndexed { index, (tableName, desc) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryBlue.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp
                        )
                    }

                    Column {
                        Text(
                            text = tableName,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimaryBlue
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Workforce SQL Schema", text)
    clipboard.setPrimaryClip(clip)
}
