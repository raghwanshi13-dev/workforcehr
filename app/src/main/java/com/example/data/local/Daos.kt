package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY id ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getEmployeeById(id: Long): Employee?

    @Query("SELECT * FROM employees WHERE empCode = :code LIMIT 1")
    suspend fun getEmployeeByCode(code: String): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<Employee>)

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("UPDATE employees SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: Long, isActive: Boolean)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY id ASC")
    fun getAllShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getShiftById(id: Long): Shift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShifts(shifts: List<Shift>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: Shift): Long

    @Update
    suspend fun updateShift(shift: Shift)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY date DESC, id DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE employeeId = :empId ORDER BY date DESC, id DESC")
    fun getAttendanceForEmployee(empId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE employeeId = :empId AND date = :date LIMIT 1")
    suspend fun getTodayAttendance(empId: Long, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance WHERE id = :id")
    suspend fun getAttendanceById(id: Long): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceBatch(records: List<AttendanceRecord>)

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Query("SELECT COUNT(*) FROM attendance WHERE date = :date AND status = 'PRESENT'")
    suspend fun getPresentCountToday(date: String): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE date = :date AND status = 'LATE'")
    suspend fun getLateCountToday(date: String): Int
}

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_applications ORDER BY id DESC")
    fun getAllLeaveApplications(): Flow<List<LeaveApplication>>

    @Query("SELECT * FROM leave_applications WHERE employeeId = :empId ORDER BY id DESC")
    fun getLeaveApplicationsForEmployee(empId: Long): Flow<List<LeaveApplication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaveApplication(leave: LeaveApplication): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaveApplications(leaves: List<LeaveApplication>)

    @Update
    suspend fun updateLeaveApplication(leave: LeaveApplication)

    @Query("UPDATE leave_applications SET status = :status, reviewedBy = :reviewedBy, managerRemarks = :remarks WHERE id = :id")
    suspend fun updateLeaveStatus(id: Long, status: String, reviewedBy: String, remarks: String)

    @Query("SELECT * FROM leave_balances WHERE employeeId = :empId")
    suspend fun getLeaveBalance(empId: Long): LeaveBalance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBalance(balance: LeaveBalance)
}

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidays(): Flow<List<Holiday>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<Holiday>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: Holiday): Long

    @Delete
    suspend fun deleteHoliday(holiday: Holiday)
}

@Dao
interface AttendanceCorrectionDao {
    @Query("SELECT * FROM attendance_corrections ORDER BY id DESC")
    fun getAllCorrections(): Flow<List<AttendanceCorrection>>

    @Query("SELECT * FROM attendance_corrections WHERE employeeId = :empId ORDER BY id DESC")
    fun getCorrectionsForEmployee(empId: Long): Flow<List<AttendanceCorrection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: AttendanceCorrection): Long

    @Query("UPDATE attendance_corrections SET status = :status, reviewedBy = :reviewedBy WHERE id = :id")
    suspend fun updateCorrectionStatus(id: Long, status: String, reviewedBy: String)
}
