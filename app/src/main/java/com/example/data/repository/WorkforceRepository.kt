package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class WorkforceRepository(private val database: AppDatabase) {

    // Employees
    val allEmployees: Flow<List<Employee>> = database.employeeDao().getAllEmployees()

    suspend fun getEmployeeById(id: Long): Employee? = database.employeeDao().getEmployeeById(id)

    suspend fun registerEmployee(employee: Employee): Long = database.employeeDao().insertEmployee(employee)

    suspend fun updateEmployee(employee: Employee) = database.employeeDao().updateEmployee(employee)

    suspend fun toggleEmployeeActive(id: Long, isActive: Boolean) = database.employeeDao().updateActiveStatus(id, isActive)

    // Shifts
    val allShifts: Flow<List<Shift>> = database.shiftDao().getAllShifts()

    suspend fun getShiftById(id: Long): Shift? = database.shiftDao().getShiftById(id)

    suspend fun addShift(shift: Shift): Long = database.shiftDao().insertShift(shift)

    suspend fun updateShift(shift: Shift) = database.shiftDao().updateShift(shift)

    // Attendance
    val allAttendance: Flow<List<AttendanceRecord>> = database.attendanceDao().getAllAttendance()

    fun getAttendanceForEmployee(empId: Long): Flow<List<AttendanceRecord>> =
        database.attendanceDao().getAttendanceForEmployee(empId)

    suspend fun getTodayAttendance(empId: Long, date: String): AttendanceRecord? =
        database.attendanceDao().getTodayAttendance(empId, date)

    suspend fun recordAttendance(record: AttendanceRecord): Long =
        database.attendanceDao().insertAttendance(record)

    suspend fun updateAttendance(record: AttendanceRecord) =
        database.attendanceDao().updateAttendance(record)

    // Leaves
    val allLeaves: Flow<List<LeaveApplication>> = database.leaveDao().getAllLeaveApplications()

    fun getLeavesForEmployee(empId: Long): Flow<List<LeaveApplication>> =
        database.leaveDao().getLeaveApplicationsForEmployee(empId)

    suspend fun applyLeave(leave: LeaveApplication): Long = database.leaveDao().insertLeaveApplication(leave)

    suspend fun reviewLeave(id: Long, status: String, reviewedBy: String, remarks: String) =
        database.leaveDao().updateLeaveStatus(id, status, reviewedBy, remarks)

    suspend fun getLeaveBalance(empId: Long): LeaveBalance? = database.leaveDao().getLeaveBalance(empId)

    suspend fun updateLeaveBalance(balance: LeaveBalance) = database.leaveDao().insertOrUpdateBalance(balance)

    // Holidays
    val allHolidays: Flow<List<Holiday>> = database.holidayDao().getAllHolidays()

    suspend fun addHoliday(holiday: Holiday): Long = database.holidayDao().insertHoliday(holiday)

    suspend fun deleteHoliday(holiday: Holiday) = database.holidayDao().deleteHoliday(holiday)

    // Corrections
    val allCorrections: Flow<List<AttendanceCorrection>> = database.correctionDao().getAllCorrections()

    fun getCorrectionsForEmployee(empId: Long): Flow<List<AttendanceCorrection>> =
        database.correctionDao().getCorrectionsForEmployee(empId)

    suspend fun submitCorrection(correction: AttendanceCorrection): Long =
        database.correctionDao().insertCorrection(correction)

    suspend fun reviewCorrection(id: Long, status: String, reviewedBy: String) =
        database.correctionDao().updateCorrectionStatus(id, status, reviewedBy)
}
