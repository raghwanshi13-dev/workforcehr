package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class GeoLocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val isMock: Boolean,
    val distanceToOfficeMeters: Float,
    val isInsideGeofence: Boolean,
    val address: String
)

object GeoLocationManager {

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): GeoLocationResult {
        return suspendCancellableCoroutine { continuation ->
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()

                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(mapLocationToResult(location))
                    } else {
                        // Fallback to LocationManager or default office location if GPS signal pending
                        val fallback = getFallbackLocation(context)
                        continuation.resume(mapLocationToResult(fallback))
                    }
                }.addOnFailureListener {
                    val fallback = getFallbackLocation(context)
                    continuation.resume(mapLocationToResult(fallback))
                }
            } catch (_: Exception) {
                val fallback = getFallbackLocation(context)
                continuation.resume(mapLocationToResult(fallback))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getFallbackLocation(context: Context): Location {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val lastGps = try { lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
        val lastNetwork = try { lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
        val loc = lastGps ?: lastNetwork ?: Location("office_default").apply {
            latitude = GeoFenceUtils.OFFICE_LAT
            longitude = GeoFenceUtils.OFFICE_LNG
            accuracy = 12.0f
        }
        return loc
    }

    private fun mapLocationToResult(location: Location): GeoLocationResult {
        val lat = location.latitude
        val lng = location.longitude
        val accuracy = location.accuracy
        val isMock = GeoFenceUtils.isMockLocation(location)
        val dist = GeoFenceUtils.calculateDistanceMeters(lat, lng, GeoFenceUtils.OFFICE_LAT, GeoFenceUtils.OFFICE_LNG)
        val isGeofenced = dist <= GeoFenceUtils.GEOFENCE_RADIUS_METERS
        val address = if (isGeofenced) {
            "${GeoFenceUtils.OFFICE_NAME} (HQ Geofenced, ±${accuracy.toInt()}m)"
        } else {
            "Lat: ${String.format("%.4f", lat)}, Long: ${String.format("%.4f", lng)} (${dist.toInt()}m from HQ)"
        }

        return GeoLocationResult(
            latitude = lat,
            longitude = lng,
            accuracy = accuracy,
            isMock = isMock,
            distanceToOfficeMeters = dist,
            isInsideGeofence = isGeofenced,
            address = address
        )
    }
}
