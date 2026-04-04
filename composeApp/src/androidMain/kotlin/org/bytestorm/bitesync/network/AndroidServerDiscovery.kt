package org.bytestorm.bitesync.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class AndroidServerDiscovery : ServerDiscovery {
    private val _discoveredServerUrl = MutableStateFlow<String?>(null)
    override val discoveredServerUrl = _discoveredServerUrl.asStateFlow()

    private var discoveryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun startDiscovery() {
        if (discoveryJob != null) return
        discoveryJob = scope.launch {
            try {
                val socket = DatagramSocket(8888, InetAddress.getByName("0.0.0.0"))
                socket.broadcast = true
                val buffer = ByteArray(1024)
                while (true) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()
                    if (message.startsWith("BITESYNC_SERVER:")) {
                        val port = message.substringAfter("BITESYNC_SERVER:").toIntOrNull() ?: 8080
                        val ip = packet.address.hostAddress
                        _discoveredServerUrl.value = "http://$ip:$port"
                        break 
                    }
                }
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
    }
}
