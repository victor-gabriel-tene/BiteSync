package org.bytestorm.bitesync.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse

class IosLocationTracker : LocationTracker {
    private val locationManager = CLLocationManager()

    init {
        locationManager.requestWhenInUseAuthorization()
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getCurrentLocation(): Pair<Double, Double>? {
        val status = CLLocationManager.authorizationStatus()
        if (status != kCLAuthorizationStatusAuthorizedWhenInUse &&
            status != kCLAuthorizationStatusAuthorizedAlways
        ) {
            return null
        }

        val location = locationManager.location ?: return null
        return location.coordinate.useContents {
            Pair(latitude, longitude)
        }
    }
}
