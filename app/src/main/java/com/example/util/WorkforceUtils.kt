package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

object GeoFenceUtils {
    // Enterprise Headquarters coordinates
    const val OFFICE_LAT = 28.613939
    const val OFFICE_LNG = 77.209021
    const val OFFICE_NAME = "Enterprise Cyber Park, Tower B"
    const val GEOFENCE_RADIUS_METERS = 250.0f

    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }

    fun isWithinGeofence(lat: Double, lng: Double, radiusMeters: Float = GEOFENCE_RADIUS_METERS): Boolean {
        return calculateDistanceMeters(lat, lng, OFFICE_LAT, OFFICE_LNG) <= radiusMeters
    }

    fun isMockLocation(location: Location?): Boolean {
        if (location == null) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }
}

object DeviceUtils {
    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return "$manufacturer $model"
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return if (!androidId.isNullOrEmpty()) "DEV-$androidId" else "DEV-${Build.SERIAL.take(8)}"
    }
}

object NetworkUtils {
    fun getLocalIpAddress(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress ?: "192.168.1.108"
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "192.168.1.108"
    }

    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

object DateTimeUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun getTodayDate(): String = dateFormat.format(Date())

    fun getCurrentTime(): String = timeFormat.format(Date())

    fun getCurrentDisplayTime(): String = displayTimeFormat.format(Date())

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun calculateLateMinutes(shiftStartTimeStr: String, punchTimeStr: String, gracePeriod: Int = 15): Int {
        try {
            val parser = SimpleDateFormat("HH:mm", Locale.getDefault())
            val shiftStart = parser.parse(shiftStartTimeStr)?.time ?: return 0
            val punch = parser.parse(punchTimeStr.take(5))?.time ?: return 0
            val diffMinutes = ((punch - shiftStart) / (1000 * 60)).toInt()
            return if (diffMinutes > gracePeriod) diffMinutes else 0
        } catch (_: Exception) {
            return 0
        }
    }

    fun calculateWorkingMinutes(checkInTimeStr: String, checkOutTimeStr: String): Int {
        try {
            val parser = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val inTime = parser.parse(checkInTimeStr)?.time ?: return 0
            val outTime = parser.parse(checkOutTimeStr)?.time ?: return 0
            val diff = ((outTime - inTime) / (1000 * 60)).toInt()
            return max(0, diff)
        } catch (_: Exception) {
            return 0
        }
    }
}
