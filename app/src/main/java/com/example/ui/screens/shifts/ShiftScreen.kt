package com.example.ui.screens.shifts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Shift
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val shifts by viewModel.shifts.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    var showAddShiftDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Shift & Roster Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure flexible timings, grace periods & break allowances",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isAdminMode) {
                Button(
                    onClick = { showAddShiftDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Shift", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Automatic Late Marking & Policies Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                Column {
                    Text(
                        text = "Automated Late Marking Rule",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Employees checking in after the shift start time + grace period (15m) are automatically flagged as 'LATE' with an amber status audit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List of Shifts
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(shifts, key = { it.id }) { shift ->
                ShiftCard(shift = shift)
            }
        }
    }

    if (showAddShiftDialog) {
        AddShiftDialog(
            onDismiss = { showAddShiftDialog = false },
            onAdd = { name, start, end, grace, breakDur, isFlex, weeklyOff ->
                viewModel.addShift(name, start, end, grace, breakDur, isFlex, weeklyOff) {
                    showAddShiftDialog = false
                }
            }
        )
    }
}

@Composable
fun ShiftCard(shift: Shift) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val shiftIcon = when {
                        shift.isFlexible -> Icons.Default.AllInclusive
                        shift.shiftName.contains("Night", ignoreCase = true) -> Icons.Default.NightsStay
                        shift.shiftName.contains("Evening", ignoreCase = true) -> Icons.Default.WbTwilight
                        else -> Icons.Default.WbSunny
                    }

                    Icon(shiftIcon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Text(
                        text = shift.shiftName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (shift.isFlexible) SecondaryTeal.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (shift.isFlexible) "FLEXIBLE" else "FIXED SHIFT",
                        color = if (shift.isFlexible) SecondaryTeal else PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Working Hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${shift.startTime} - ${shift.endTime}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }

                Column {
                    Text("Grace Period", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${shift.gracePeriodMinutes} mins", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = StatusLate)
                }

                Column {
                    Text("Break Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${shift.breakDurationMinutes} mins", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Weekly Off
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Weekend, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Weekly Off: ${shift.weeklyOffDays}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AddShiftDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, start: String, end: String, grace: Int, breakDur: Int, isFlex: Boolean, weeklyOff: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("09:00") }
    var end by remember { mutableStateOf("18:00") }
    var grace by remember { mutableIntStateOf(15) }
    var breakDur by remember { mutableIntStateOf(45) }
    var isFlex by remember { mutableStateOf(false) }
    var weeklyOff by remember { mutableStateOf("Saturday,Sunday") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure New Shift", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Shift Title (e.g. US Support Shift)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = weeklyOff,
                    onValueChange = { weeklyOff = it },
                    label = { Text("Weekly Off Days") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Flexible Working Hours?")
                    Switch(checked = isFlex, onCheckedChange = { isFlex = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, start, end, grace, breakDur, isFlex, weeklyOff)
                    }
                }
            ) {
                Text("Save Shift")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
