package org.bytestorm.bitesync.network

import io.ktor.client.engine.HttpClientEngineFactory

expect fun httpClientEngine(): HttpClientEngineFactory<*>
