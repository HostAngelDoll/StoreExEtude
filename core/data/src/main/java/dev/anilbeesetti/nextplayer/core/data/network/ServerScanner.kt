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
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val wifiSubnet = getSubnetForTransport(connectivityManager, NetworkCapabilities.TRANSPORT_WIFI)
        if (wifiSubnet != null) return wifiSubnet

        val ethernetSubnet = getSubnetForTransport(connectivityManager, NetworkCapabilities.TRANSPORT_ETHERNET)
        if (ethernetSubnet != null) return ethernetSubnet

        return null
    }

    private fun getSubnetForTransport(cm: ConnectivityManager, transport: Int): String? {
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network)
            if (caps?.hasTransport(transport) == true) {
                val lp = cm.getLinkProperties(network)
                lp?.linkAddresses?.forEach {
                    val addr = it.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress
                        if (ip != null) return ip.substringBeforeLast(".")
                    }
                }
            }
        }
        return null
    }
}
