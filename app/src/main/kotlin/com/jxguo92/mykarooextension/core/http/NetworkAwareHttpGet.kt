package com.jxguo92.mykarooextension.core.http

fun interface WifiStatus {
    fun isConnected(): Boolean
}

/**
 * New Karoo has no cellular radio, so the app's own network stack only works on Wi-Fi.
 * Without Wi-Fi, requests go through the Karoo System, which can relay them over
 * Bluetooth to the companion app where supported.
 */
class NetworkAwareHttpGet(
    private val wifi: WifiStatus,
    private val direct: HttpGet,
    private val karoo: HttpGet,
) : HttpGet {
    override suspend fun get(request: HttpRequest): HttpResponse {
        val client = if (wifi.isConnected()) direct else karoo
        return client.get(request)
    }
}
