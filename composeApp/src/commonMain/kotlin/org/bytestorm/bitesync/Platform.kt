package org.bytestorm.bitesync

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform