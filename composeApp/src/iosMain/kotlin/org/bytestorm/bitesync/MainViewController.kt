package org.bytestorm.bitesync

import androidx.compose.ui.window.ComposeUIViewController
import org.bytestorm.bitesync.location.IosLocationTracker
import org.bytestorm.bitesync.network.IosServerDiscovery

fun MainViewController() = ComposeUIViewController {
    App(
        serverDiscovery = IosServerDiscovery(),
        locationTracker = IosLocationTracker()
    )
}