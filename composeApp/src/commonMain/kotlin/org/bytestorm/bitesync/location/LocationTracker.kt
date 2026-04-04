package org.bytestorm.bitesync.location

interface LocationTracker {
    suspend fun getCurrentLocation(): Pair<Double, Double>?
}
