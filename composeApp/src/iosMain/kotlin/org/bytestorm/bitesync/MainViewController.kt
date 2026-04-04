package org.bytestorm.bitesync

import androidx.compose.ui.window.ComposeUIViewController
import org.bytestorm.bitesync.location.IosLocationTracker

fun MainViewController() = ComposeUIViewController { App(locationTracker = IosLocationTracker()) }