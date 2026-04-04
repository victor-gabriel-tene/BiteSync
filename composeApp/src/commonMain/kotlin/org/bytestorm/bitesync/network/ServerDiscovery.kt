package org.bytestorm.bitesync.network

import kotlinx.coroutines.flow.StateFlow

interface ServerDiscovery {
    val discoveredServerUrl: StateFlow<String?>
    fun startDiscovery()
    fun stopDiscovery()
}
