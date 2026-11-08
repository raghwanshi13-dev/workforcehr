package com.example.ui.screens.employee

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val employees by viewModel.employees.collectAsState()
    val shifts by viewModel.shifts.collectAsState()
    val selectedEmpId by viewModel.selectedEmployeeId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, INACTIVE, DEPT
    var showRegisterDialog by remember { mutableStateOf(false) }
    var selectedDetailEmployee by remember { mutableStateOf<Employee?>(null) }

    val filteredEmployees = remember(employees, searchQuery, selectedFilter) {
        employees.filter { emp ->
            val matchesSearch = emp.fullName.contains(searchQuery, ignoreCase = true) ||
                    emp.empCode.contains(searchQuery, ignoreCase = true) ||
                    emp.department.contains(searchQuery, ignoreCase = true) ||
                    emp.designation.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "ACTIVE" -> emp.isActive
                "INACTIVE" -> !emp.isActive
                "ENGINEERING" -> emp.department.contains("Engineering", ignoreCase = true)
                "HR" -> emp.department.contains("Human Resources", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRegisterDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Register Employee", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("register_employee_fab")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name, code, department...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${employees.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "ACTIVE",
                    onClick = { selectedFilter = "ACTIVE" },
                    label = { Text("Active (${employees.count { it.isActive }})") }
                )
                FilterChip(
                    selected = selectedFilter == "INACTIVE",
                    onClick = { selectedFilter = "INACTIVE" },
                    label = { Text("Inactive (${employees.count { !it.isActive }})") }
                )
                FilterChip(
                    selected = selectedFilter == "ENGINEERING",
                    onClick = { selectedFilter = "ENGINEERING" },
                    label = { Text("Eng") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Employee List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredEmployees, key = { it.id }) { employee ->
                    EmployeeCard(
                        employee = employee,
                        isSelected = employee.id == selectedEmpId,
                        onSelect = { viewModel.selectEmployee(employee.id) },
                        onToggleActive = { viewModel.toggleEmployeeStatus(employee.id, employee.isActive) },
                        onViewDetails = { selectedDetailEmployee = employee }
                    )
                }
            }
        }
    }

    // Register Employee Modal Dialog
    if (showRegisterDialog) {
        RegisterEmployeeDialog(
            shifts = shifts,
            onDismiss = { showRegisterDialog = false },
            onRegister = { code, name, email, mobile, addr, dept, desig, joinDate, shiftId, emerg, docs ->
                viewModel.registerNewEmployee(
                    empCode = code,
                    fullName = name,
                    email = email,
                    mobile = mobile,
                    address = addr,
                    department = dept,
                    designation = desig,
                    joiningDate = joinDate,
                    shiftId = shiftId,
                    emergencyContact = emerg,
                    documents = docs
                ) {
                    showRegisterDialog = false
                }
            }
        )
    }

    // View Details / Document Upload Dialog
    selectedDetailEmployee?.let { emp ->
        EmployeeDetailsDialog(
            employee = emp,
            onDismiss = { selectedDetailEmployee = null },
            onToggleStatus = {
                viewModel.toggleEmployeeStatus(emp.id, emp.isActive)
                selectedDetailEmployee = null
            }
        )
    }
}

@Composable
fun EmployeeCard(
    employee: Employee,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleActive: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryBlue) else null,
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar circle with initials
                    val initials = employee.fullName.split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2).joinToString("")

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (employee.isActive) PrimaryBlue else Color.Gray
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = employee.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryBlue
                                ) {
                                    Text(
                                        text = "CURRENT",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${employee.empCode} • ${employee.designation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Active / Inactive Switch
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = employee.isActive,
                        onCheckedChange = { onToggleActive() },
                        modifier = Modifier.height(28.dp)
                    )
                    Text(
                        text = if (employee.isActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (employee.isActive) StatusSuccess else StatusAbsent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Contact and Department row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(employee.mobile, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(employee.department, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(employee.joiningDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Document Tags
            val docList = employee.documents.split(",").filter { it.isNotBlank() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                docList.take(3).forEach { doc ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(doc.take(15), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!isSelected) {
                    FilledTonalButton(
                        onClick = onSelect,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Select as Me", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterEmployeeDialog(
    shifts: List<com.example.data.model.Shift>,
    onDismiss: () -> Unit,
    onRegister: (
        code: String,
        name: String,
        email: String,
        mobile: String,
        addr: String,
        dept: String,
        desig: String,
        joinDate: String,
        shiftId: Long,
        emerg: String,
        docs: String
    ) -> Unit
) {
    var code by remember { mutableStateOf("EMP-${(1005..1999).random()}") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Engineering") }
    var designation by remember { mutableStateOf("Software Engineer") }
    var joiningDate by remember { mutableStateOf("2026-11-01") }
    var selectedShiftId by remember { mutableLongStateOf(1L) }
    var emergencyContact by remember { mutableStateOf("+91 98000 11223") }
    var documents by remember { mutableStateOf("Aadhaar_ID.pdf,Offer_Letter.pdf") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Register New Employee", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Employee ID / Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Work Email *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = designation,
                        onValueChange = { designation = it },
                        label = { Text("Designation") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Residential Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = joiningDate,
                        onValueChange = { joiningDate = it },
                        label = { Text("Joining Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        label = { Text("Emergency Contact") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = documents,
                        onValueChange = { documents = it },
                        label = { Text("Uploaded Documents (comma-separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (errorMessage != null) {
                    item {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || mobile.isBlank()) {
                        errorMessage = "Please enter Name, Email, and Mobile."
                    } else {
                        onRegister(
                            code, name, email, mobile, address, department, designation,
                            joiningDate, selectedShiftId, emergencyContact, documents
                        )
                    }
                }
            ) {
                Text("Register Employee")
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
fun EmployeeDetailsDialog(
    employee: Employee,
    onDismiss: () -> Unit,
    onToggleStatus: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = PrimaryBlue)
                Text(employee.fullName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Employee Code: ${employee.empCode}", fontWeight = FontWeight.SemiBold)
                Text("Designation: ${employee.designation}")
                Text("Department: ${employee.department}")
                Text("Email: ${employee.email}")
                Text("Mobile: ${employee.mobile}")
                Text("Address: ${employee.address}")
                Text("Joining Date: ${employee.joiningDate}")
                Text("Emergency Contact: ${employee.emergencyContact}")
                Text("Registered Device: ${employee.registeredDeviceModel} (${employee.registeredDeviceId})")

                HorizontalDivider()

                Text("Uploaded Documents & Credentials:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                val docs = employee.documents.split(",").filter { it.isNotBlank() }
                docs.forEach { doc ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                        Text(doc, style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onToggleStatus) {
                Text(if (employee.isActive) "Mark as Inactive" else "Mark as Active")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
