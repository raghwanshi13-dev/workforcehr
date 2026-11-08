package com.example.data.local

import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

object DatabaseInitializer {

    val superAdminUser = Employee(
        id = 1,
        empCode = "SUPER-001",
        fullName = "Super Administrator",
        email = "supradmin@gmail.com",
        mobile = "+91 99999 00000",
        address = "Infrastructure & DevOps HQ",
        department = "System Architecture",
        designation = "Chief System Administrator",
        joiningDate = "2026-01-01",
        isActive = true,
        shiftId = 1,
        registeredDeviceId = "DEV-SUPER-001",
        registeredDeviceModel = "Secured Console",
        emergencyContact = "+91 99999 11111",
        documents = "SuperAdmin_Credential.pdf",
        role = "SUPERADMIN"
    )

    val defaultInitialUser = Employee(
        id = 2,
        empCode = "EMP-1001",
        fullName = "Shikhar Vilas (HR Admin)",
        email = "shikharvilas@gmail.com",
        mobile = "+91 98765 43210",
        address = "Headquarters, Cyber City",
        department = "Operations",
        designation = "HR & System Administrator",
        joiningDate = "2026-01-01",
        isActive = true,
        shiftId = 1,
        registeredDeviceId = "DEV-PRIMARY-001",
        registeredDeviceModel = "Verified Mobile",
        emergencyContact = "+91 98000 00000",
        documents = "ID_Badge.pdf,Employment_Letter.pdf",
        role = "ADMIN"
    )

    val defaultStaffUser = Employee(
        id = 3,
        empCode = "EMP-1002",
        fullName = "Aarav Sharma",
        email = "aarav.sharma@workforce.com",
        mobile = "+91 98111 22334",
        address = "Tower B, Cyber Hub",
        department = "Engineering",
        designation = "Software Engineer",
        joiningDate = "2026-02-15",
        isActive = true,
        shiftId = 1,
        registeredDeviceId = "DEV-STAFF-002",
        registeredDeviceModel = "Samsung Galaxy S24",
        emergencyContact = "+91 98111 00000",
        documents = "Degree.pdf,ID.pdf",
        role = "EMPLOYEE"
    )

    fun getDefaultEmployees(): List<Employee> = listOf(superAdminUser, defaultInitialUser, defaultStaffUser)

    suspend fun populateInitialData(database: AppDatabase) = withContext(Dispatchers.IO) {
        val employeeDao = database.employeeDao()
        val shiftDao = database.shiftDao()
        val holidayDao = database.holidayDao()
        val leaveDao = database.leaveDao()

        val existingEmployees = employeeDao.getAllEmployees().firstOrNull()
        if (!existingEmployees.isNullOrEmpty()) {
            return@withContext
        }

        // 1. Core Shifts (Required for attendance calculation)
        val defaultShifts = listOf(
            Shift(
                id = 1,
                shiftName = "General Morning Shift",
                startTime = "09:00",
                endTime = "18:00",
                gracePeriodMinutes = 15,
                breakDurationMinutes = 45,
                isFlexible = false,
                weeklyOffDays = "Saturday,Sunday"
            ),
            Shift(
                id = 2,
                shiftName = "Flexible Hours (8h Target)",
                startTime = "08:00",
                endTime = "20:00",
                gracePeriodMinutes = 30,
                breakDurationMinutes = 60,
                isFlexible = true,
                weeklyOffDays = "Saturday,Sunday"
            )
        )
        shiftDao.insertShifts(defaultShifts)

        // 2. Clean starter users with role mapping (SUPERADMIN, ADMIN, and EMPLOYEE)
        employeeDao.insertEmployee(superAdminUser)
        employeeDao.insertEmployee(defaultInitialUser)
        employeeDao.insertEmployee(defaultStaffUser)

        // 3. Initialize fresh annual leave balances (0 used)
        leaveDao.insertOrUpdateBalance(
            LeaveBalance(
                employeeId = 1,
                casualLeaveTotal = 15,
                casualLeaveUsed = 0,
                sickLeaveTotal = 15,
                sickLeaveUsed = 0,
                earnedLeaveTotal = 25,
                earnedLeaveUsed = 0
            )
        )
        leaveDao.insertOrUpdateBalance(
            LeaveBalance(
                employeeId = 2,
                casualLeaveTotal = 12,
                casualLeaveUsed = 0,
                sickLeaveTotal = 10,
                sickLeaveUsed = 0,
                earnedLeaveTotal = 18,
                earnedLeaveUsed = 0
            )
        )
        leaveDao.insertOrUpdateBalance(
            LeaveBalance(
                employeeId = 3,
                casualLeaveTotal = 12,
                casualLeaveUsed = 0,
                sickLeaveTotal = 10,
                sickLeaveUsed = 0,
                earnedLeaveTotal = 18,
                earnedLeaveUsed = 0
            )
        )

        // 4. Preload standard calendar holidays
        val defaultHolidays = listOf(
            Holiday(1, "New Year's Day", "2026-01-01", "Thursday", "NATIONAL", "Global celebration marking the new year"),
            Holiday(2, "Republic Day", "2026-01-26", "Monday", "NATIONAL", "Constitution of India commemoration"),
            Holiday(3, "Holi Festival", "2026-03-04", "Wednesday", "FESTIVAL", "Festival of colors and spring"),
            Holiday(4, "Good Friday", "2026-04-03", "Friday", "PUBLIC", "Christian holiday commemorating the passion"),
            Holiday(5, "Eid-ul-Fitr", "2026-03-20", "Friday", "FESTIVAL", "Celebration marking the end of Ramadan"),
            Holiday(6, "Independence Day", "2026-08-15", "Saturday", "NATIONAL", "National Independence Day celebration"),
            Holiday(7, "Gandhi Jayanti", "2026-10-02", "Friday", "NATIONAL", "Birth anniversary of Mahatma Gandhi"),
            Holiday(8, "Diwali (Festival of Lights)", "2026-11-09", "Monday", "FESTIVAL", "Major festival celebrating prosperity & light"),
            Holiday(9, "Christmas Day", "2026-12-25", "Friday", "PUBLIC", "Christmas celebration and company holiday")
        )
        holidayDao.insertHolidays(defaultHolidays)
        // Note: Demo attendance records, demo leave applications and demo corrections are completely removed!
    }

    suspend fun clearAllAttendanceAndLeaveHistory(database: AppDatabase) = withContext(Dispatchers.IO) {
        val db = database.openHelper.writableDatabase
        db.execSQL("DELETE FROM attendance")
        db.execSQL("DELETE FROM leave_applications")
        db.execSQL("DELETE FROM attendance_corrections")
    }
}
