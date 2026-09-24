package com.jxguo92.mykarooextension.core.http

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Checks the default network, because that is the one [UrlConnectionHttpGet] uses.
 * VALIDATED is not required: captive-portal probes can fail on networks that still
 * reach the target API.
 */
class ConnectivityWifiStatus(context: Context) : WifiStatus {
    private val connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override fun isConnected(): Boolean {
        val manager = connectivity ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
