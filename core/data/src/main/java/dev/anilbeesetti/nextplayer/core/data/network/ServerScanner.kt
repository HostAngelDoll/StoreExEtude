package dev.anilbeesetti.nextplayer.core.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.anilbeesetti.nextplayer.core.common.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.net.Inet4Address
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: StoreEtudeClient
) {
    companion object {
        private const val TAG = "ServerScanner"
    }

    suspend fun scan(port: Int): String? = withContext(Dispatchers.IO) {
        val subnet = getSubnet() ?: return@withContext null
        val channel = Channel<String?>(Channel.CONFLATED)

        val scanJob = launch {
            (1..254).forEach { i ->
                launch {
                    val ip = "$subnet.$i"
                    val ping = try {
                        client.ping(ip, port)
                    } catch (e: Exception) {
                        null
                    }
                    if (ping?.name?.trim()?.equals("StoreEtude", ignoreCase = true) == true) {
                        Logger.logDebug(TAG, "Server found during scan: $ip")
                        channel.trySend(ip)
                    }
                }
            }
        }

        val result = withTimeoutOrNull(10000) {
            select<String?> {
                channel.onReceive { it }
                scanJob.onJoin { null }
            }
        }

        scanJob.cancel()
        result
    }

    private fun getSubnet(): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Strategy: Use the active network if it's Wi-Fi or Ethernet
        val activeNetwork = cm.activeNetwork
        if (activeNetwork != null) {
            val caps = cm.getNetworkCapabilities(activeNetwork)
            if (caps != null && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    val lp = cm.getLinkProperties(activeNetwork)
                    lp?.linkAddresses?.forEach {
                        val addr = it.address
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val ip = addr.hostAddress
                            if (ip != null) {
                                Logger.logDebug(TAG, "Subnet detected from active network: $ip")
                                return ip.substringBeforeLast(".")
                            }
                        }
                    }
                }
            }
        }

        // Fallback: Scan all networks for non-VPN Wi-Fi/Ethernet
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network)
            if (caps != null && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    val lp = cm.getLinkProperties(network)
                    lp?.linkAddresses?.forEach {
                        val addr = it.address
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val ip = addr.hostAddress
                            if (ip != null) {
                                Logger.logDebug(TAG, "Subnet detected from fallback network: $ip")
                                return ip.substringBeforeLast(".")
                            }
                        }
                    }
                }
            }
        }

        return null
    }
}
