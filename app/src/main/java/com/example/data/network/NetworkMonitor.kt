package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.example.model.ConnectionType
import com.example.model.NetworkInfoState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observeNetworkState(): Flow<NetworkInfoState> = callbackFlow {
        fun getCurrentState(): NetworkInfoState {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

            if (activeNetwork == null || capabilities == null) {
                return NetworkInfoState(
                    connectionType = ConnectionType.NONE,
                    isConnected = false,
                    isInternetValidated = false,
                    ipAddress = "—",
                    linkDownstreamSpeedMbps = 0,
                    networkName = "Нет сети"
                )
            }

            val connectionType = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
                else -> ConnectionType.NONE
            }

            val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            val downstreamSpeedKbps = capabilities.linkDownstreamBandwidthKbps
            val downstreamMbps = if (downstreamSpeedKbps > 0) downstreamSpeedKbps / 1000 else 0

            val ipAddress = getLocalIpAddress() ?: "—"

            val networkName = when (connectionType) {
                ConnectionType.WIFI -> "Wi-Fi сеть"
                ConnectionType.CELLULAR -> "Мобильная сеть"
                ConnectionType.ETHERNET -> "Ethernet"
                ConnectionType.NONE -> "Не подключено"
            }

            return NetworkInfoState(
                connectionType = connectionType,
                isConnected = isConnected,
                isInternetValidated = isValidated,
                ipAddress = ipAddress,
                linkDownstreamSpeedMbps = downstreamMbps,
                networkName = networkName
            )
        }

        // Emit initial
        trySend(getCurrentState())

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getCurrentState())
            }

            override fun onLost(network: Network) {
                trySend(getCurrentState())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getCurrentState())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {
            }
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                val addresses = intf.inetAddresses ?: continue
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }
}
