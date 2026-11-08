package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.sql.PhpMysqlSchemaGenerator
import com.example.util.DateTimeUtils
import com.example.util.GeoFenceUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WorkforceHR", appName)
    }

    @Test
    fun `test geofence calculation within office radius`() {
        // Office Lat: 28.613939, Long: 77.209021
        val isInside = GeoFenceUtils.isWithinGeofence(28.613940, 77.209020, 250f)
        assertTrue(isInside)
    }

    @Test
    fun `test late entry minutes calculation`() {
        val lateMins = DateTimeUtils.calculateLateMinutes("09:00", "09:35:00", gracePeriod = 15)
        assertEquals(35, lateMins)
    }

    @Test
    fun `test working minutes calculation`() {
        val workingMins = DateTimeUtils.calculateWorkingMinutes("09:00:00", "17:30:00")
        assertEquals(510, workingMins)
    }

    @Test
    fun `test sql schema generation contains core tables`() {
        val sql = PhpMysqlSchemaGenerator.generateFullMySqlSchema()
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `employees`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `attendance`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `leave_applications`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `shifts`"))
    }

    @Test
    fun `test leave push notification dispatch`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.util.WorkforceNotificationManager.sendLeaveStatusNotification(
            context = context,
            employeeName = "Aarav Sharma",
            leaveType = "CASUAL",
            status = "APPROVED",
            remarks = "Approved by Manager",
            startDate = "2026-11-20",
            endDate = "2026-11-22"
        )
        val history = com.example.util.WorkforceNotificationManager.notificationHistory.value
        assertTrue(history.isNotEmpty())
        assertEquals("Leave Request Approved", history.first().title)
    }

    @Test
    fun `test shift alert push notification dispatch`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.util.WorkforceNotificationManager.sendShiftReminderNotification(
            context = context,
            employeeName = "Aarav Sharma",
            shiftName = "General Morning Shift",
            startTime = "09:00",
            minutesUntilStart = 15
        )
        val history = com.example.util.WorkforceNotificationManager.notificationHistory.value
        assertTrue(history.any { it.type == "SHIFT" })
    }

    @Test
    fun `test superadmin user mapping from database`() {
        val users = com.example.data.local.DatabaseInitializer.getDefaultEmployees()
        assertTrue(users.any { it.email.equals("supradmin@gmail.com", ignoreCase = true) })
        val superAdmin = users.first { it.email.equals("supradmin@gmail.com", ignoreCase = true) }
        assertTrue(superAdmin.isSuperAdmin())
        assertTrue(superAdmin.isAdministrator())
        assertEquals("SUPERADMIN", superAdmin.getRoleDisplayName())
    }

    @Test
    fun `test default seed user has requested gmail and admin role`() {
        val users = com.example.data.local.DatabaseInitializer.getDefaultEmployees()
        assertTrue(users.any { it.email.equals("shikharvilas@gmail.com", ignoreCase = true) })
        val shikhar = users.first { it.email.equals("shikharvilas@gmail.com", ignoreCase = true) }
        assertTrue(shikhar.isActive)
        assertEquals("Operations", shikhar.department)
        assertEquals("ADMIN", shikhar.getRoleDisplayName())
        assertTrue(shikhar.isAdministrator())
    }

    @Test
    fun `test staff user role mapping to employee dashboard`() {
        val users = com.example.data.local.DatabaseInitializer.getDefaultEmployees()
        val staff = users.first { it.email.equals("aarav.sharma@workforce.com", ignoreCase = true) }
        assertEquals("STAFF", staff.getRoleDisplayName())
        org.junit.Assert.assertFalse(staff.isAdministrator())
    }
}

