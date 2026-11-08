package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empCode: String,
    val fullName: String,
    val email: String,
    val mobile: String,
    val address: String,
    val department: String,
    val designation: String,
    val joiningDate: String,
    val isActive: Boolean = true,
    val shiftId: Long = 1,
    val registeredDeviceId: String = "DEV-83921",
    val registeredDeviceModel: String = "Google Pixel 8",
    val emergencyContact: String = "+1 555-0199",
    val documents: String = "Aadhaar_ID.pdf,Degree_Certificate.pdf,Offer_Letter.pdf",
    val role: String = "EMPLOYEE" // "SUPERADMIN", "ADMIN", "MANAGER", "EMPLOYEE"
) {
    fun isSuperAdmin(): Boolean =
        role.equals("SUPERADMIN", ignoreCase = true) ||
        email.equals("supradmin@gmail.com", ignoreCase = true)

    fun isAdministrator(): Boolean =
        isSuperAdmin() ||
        role.equals("ADMIN", ignoreCase = true) ||
        designation.contains("Admin", ignoreCase = true) ||
        designation.contains("HR", ignoreCase = true) ||
        designation.contains("Manager", ignoreCase = true)

    fun getRoleDisplayName(): String = when {
        isSuperAdmin() -> "SUPERADMIN"
        isAdministrator() -> "ADMIN"
        else -> "STAFF"
    }
}

data class DatabaseConfig(
    val url: String = "jdbc:mysql://localhost:3306",
    val dbName: String = "workforce_hr_db",
    val dbUser: String = "root",
    val dbPass: String = "",
    val isConnected: Boolean = false,
    val lastSyncTime: String? = null,
    val importedTablesCount: Int = 0
)

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftName: String,
    val startTime: String,      // "09:00"
    val endTime: String,        // "18:00"
    val gracePeriodMinutes: Int = 15,
    val breakDurationMinutes: Int = 45,
    val isFlexible: Boolean = false,
    val weeklyOffDays: String = "Saturday,Sunday"
)

@Entity(tableName = "attendance")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val employeeCode: String,
    val employeeName: String,
    val date: String,                   // "YYYY-MM-DD"
    val checkInTime: String? = null,    // "HH:mm:ss"
    val checkOutTime: String? = null,   // "HH:mm:ss"
    val checkInTimestamp: Long = 0L,
    val checkOutTimestamp: Long? = null,
    val workingMinutes: Int = 0,
    val lateMinutes: Int = 0,
    val earlyExitMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val status: String = "PRESENT",     // PRESENT, LATE, HALF_DAY, ABSENT, ON_LEAVE
    val latitude: Double = 28.6139,
    val longitude: Double = 77.2090,
    val locationAddress: String = "Tech Park Tower, Cyber City",
    val isGeofenced: Boolean = true,
    val distanceToOfficeMeters: Float = 42.5f,
    val isFaceVerified: Boolean = true,
    val faceMatchConfidence: Float = 98.4f,
    val selfieUri: String? = null,
    val isMockGpsDetected: Boolean = false,
    val deviceId: String = "DEV-83921",
    val deviceModel: String = "Google Pixel 8",
    val ipAddress: String = "192.168.1.108",
    val isOnBreak: Boolean = false,
    val breakStartTimestamp: Long? = null,
    val totalBreakMinutes: Int = 0
)

@Entity(tableName = "leave_applications")
data class LeaveApplication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val employeeCode: String,
    val employeeName: String,
    val leaveType: String,      // CASUAL, SICK, EARNED
    val startDate: String,
    val endDate: String,
    val numberOfDays: Int,
    val reason: String,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val appliedDate: String,
    val reviewedBy: String? = null,
    val managerRemarks: String? = null
)

@Entity(tableName = "leave_balances")
data class LeaveBalance(
    @PrimaryKey val employeeId: Long,
    val casualLeaveTotal: Int = 12,
    val casualLeaveUsed: Int = 3,
    val sickLeaveTotal: Int = 10,
    val sickLeaveUsed: Int = 2,
    val earnedLeaveTotal: Int = 18,
    val earnedLeaveUsed: Int = 4
)

@Entity(tableName = "attendance_corrections")
data class AttendanceCorrection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attendanceId: Long,
    val employeeId: Long,
    val employeeName: String,
    val date: String,
    val originalCheckIn: String,
    val originalCheckOut: String,
    val requestedCheckIn: String,
    val requestedCheckOut: String,
    val reason: String,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val reviewedBy: String? = null,
    val submittedAt: String
)

@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val date: String,
    val dayOfWeek: String,
    val type: String, // PUBLIC, NATIONAL, COMPANY, OPTIONAL
    val description: String
)
