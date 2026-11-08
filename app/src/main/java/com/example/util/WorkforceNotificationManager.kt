package com.example.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class AppNotification(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val type: String, // "LEAVE" or "SHIFT"
    val timestamp: String,
    val isRead: Boolean = false
)

object WorkforceNotificationManager {

    const val CHANNEL_LEAVE = "leave_updates_channel"
    const val CHANNEL_SHIFT = "shift_reminders_channel"

    private val _notificationHistory = MutableStateFlow<List<AppNotification>>(emptyList())
    val notificationHistory: StateFlow<List<AppNotification>> = _notificationHistory.asStateFlow()

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Leave Status Channel
            val leaveChannel = NotificationChannel(
                CHANNEL_LEAVE,
                "Leave Status Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for leave request approval or rejection"
                enableVibration(true)
            }

            // Shift Reminder Channel
            val shiftChannel = NotificationChannel(
                CHANNEL_SHIFT,
                "Shift Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when approaching scheduled shift start time"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(leaveChannel)
            notificationManager.createNotificationChannel(shiftChannel)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    @SuppressLint("MissingPermission")
    fun sendLeaveStatusNotification(
        context: Context,
        employeeName: String,
        leaveType: String,
        status: String,
        remarks: String?,
        startDate: String,
        endDate: String
    ) {
        initNotificationChannels(context)

        val title = if (status.equals("APPROVED", ignoreCase = true)) {
            "Leave Request Approved"
        } else {
            "Leave Request Rejected"
        }

        val noteText = if (!remarks.isNullOrBlank()) " Note: \"$remarks\"" else ""
        val message = "Your $leaveType leave ($startDate to $endDate) was $status by Manager.$noteText"

        val timeStr = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date())

        // Save to in-app notification center
        val notificationItem = AppNotification(
            title = title,
            message = message,
            type = "LEAVE",
            timestamp = timeStr
        )
        _notificationHistory.value = listOf(notificationItem) + _notificationHistory.value

        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_LEAVE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun sendShiftReminderNotification(
        context: Context,
        employeeName: String,
        shiftName: String,
        startTime: String,
        minutesUntilStart: Int
    ) {
        initNotificationChannels(context)

        val title = "Shift Alert: $shiftName Starts Soon"
        val message = "Hi $employeeName, your shift begins at $startTime (in $minutesUntilStart minutes). Please verify your face and GPS location to punch in on time."

        val timeStr = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date())

        // Save to in-app notification center
        val notificationItem = AppNotification(
            title = title,
            message = message,
            type = "SHIFT",
            timestamp = timeStr
        )
        _notificationHistory.value = listOf(notificationItem) + _notificationHistory.value

        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SHIFT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (_: Exception) {}
    }

    fun clearNotificationHistory() {
        _notificationHistory.value = emptyList()
    }
}
