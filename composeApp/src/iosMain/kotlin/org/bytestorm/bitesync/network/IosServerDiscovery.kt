package org.bytestorm.bitesync.network

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
class IosServerDiscovery : ServerDiscovery {
    private val _discoveredServerUrl = MutableStateFlow<String?>(null)
    override val discoveredServerUrl = _discoveredServerUrl.asStateFlow()

    private var discoveryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun startDiscovery() {
        if (discoveryJob != null) return
        discoveryJob = scope.launch {
            val sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
            if (sock < 0) return@launch

            memScoped {
                val addr = alloc<sockaddr_in>()
                addr.sin_family = AF_INET.toUByte()
                val port = 8888
                addr.sin_port = ((port shl 8) or (port shr 8)).toUShort() 
                addr.sin_addr.s_addr = INADDR_ANY

                if (bind(sock, addr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt()) < 0) {
                    close(sock)
                    return@launch
                }

                val buffer = ByteArray(1024)
                while (isActive) {
                    val senderAddr = alloc<sockaddr_in>()
                    val senderAddrLen = alloc<socklen_tVar>()
                    senderAddrLen.value = sizeOf<sockaddr_in>().toUInt()
                    
                    val received = recvfrom(sock, buffer.refTo(0), buffer.size.toULong(), 0, senderAddr.ptr.reinterpret(), senderAddrLen.ptr)
                    if (received > 0L) {
                        val message = buffer.decodeToString(endIndex = received.toInt()).trim()
                        if (message.startsWith("BITESYNC_SERVER:")) {
                            val portNum = message.substringAfter("BITESYNC_SERVER:").toIntOrNull() ?: 8080
                            val s_addr = senderAddr.sin_addr.s_addr
                            val ip = "${s_addr and 0xFFu}.${(s_addr shr 8) and 0xFFu}.${(s_addr shr 16) and 0xFFu}.${(s_addr shr 24) and 0xFFu}"
                            _discoveredServerUrl.value = "http://$ip:$portNum"
                            break
                        }
                    }
                    yield()
                }
            }
            close(sock)
        }
    }

    override fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
    }
}
