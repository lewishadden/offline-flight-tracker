package com.lewishadden.flighttracker.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.GlobalScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live online / offline / unmetered signal sourced from ConnectivityManager.
 * The repository uses this to decide whether to hit the network and whether
 * Wi-Fi-only refresh should suppress polling on cellular.
 */
data class NetworkStatus(
    val online: Boolean,
    val unmetered: Boolean,
)

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    val status: Flow<NetworkStatus> = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)

        fun publish() {
            val active = cm?.activeNetwork
            val caps = active?.let { cm.getNetworkCapabilities(it) }
            val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val unmetered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
            trySend(NetworkStatus(online = online, unmetered = unmetered))
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = publish()
            override fun onLost(network: Network) = publish()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = publish()
        }
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm?.registerNetworkCallback(req, callback)
        publish()
        awaitClose { cm?.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .shareIn(
            scope = GlobalScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1,
        )

    fun snapshot(): NetworkStatus {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val active = cm?.activeNetwork
        val caps = active?.let { cm.getNetworkCapabilities(it) }
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val unmetered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
        return NetworkStatus(online = online, unmetered = unmetered)
    }
}
