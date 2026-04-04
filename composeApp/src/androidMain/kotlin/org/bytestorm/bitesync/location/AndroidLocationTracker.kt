package org.bytestorm.bitesync.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidLocationTracker(private val context: Context) : LocationTracker {
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // Try to get the last known location from network or GPS
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            
            val bestLocation = gpsLocation ?: networkLocation
            
            if (bestLocation != null) {
                cont.resume(Pair(bestLocation.latitude, bestLocation.longitude))
            } else {
                cont.resume(null)
            }
        } catch (e: Exception) {
            // Permission might not be granted, or location is disabled
            cont.resume(null)
        }
    }
}
