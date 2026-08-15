package com.prakash.pexplorer.data.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PHubDiscoveryService(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    suspend fun discover(timeoutMillis: Long = 4_000L): Result<List<DiscoveredService>> =
        withContext(Dispatchers.Main.immediate) {
            runCatching {
                suspendCancellableCoroutine { continuation ->
                    val services = linkedMapOf<String, DiscoveredService>()
                    val finished = AtomicBoolean(false)
                    var listener: NsdManager.DiscoveryListener? = null
                    val timeout = Runnable {
                        if (finished.compareAndSet(false, true)) {
                            listener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
                            continuation.resume(services.values.toList())
                        }
                    }
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    listener = object : NsdManager.DiscoveryListener {
                        override fun onDiscoveryStarted(regType: String) {
                            handler.postDelayed(timeout, timeoutMillis)
                        }

                        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                            if (!serviceInfo.serviceType.contains(SERVICE_TYPE)) return
                            nsdManager.resolveService(
                                serviceInfo,
                                object : NsdManager.ResolveListener {
                                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit

                                    override fun onServiceResolved(info: NsdServiceInfo) {
                                        val host = info.host?.hostAddress ?: return
                                        services["$host:${info.port}"] = DiscoveredService(
                                            name = info.serviceName,
                                            host = host,
                                            port = info.port
                                        )
                                    }
                                }
                            )
                        }

                        override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
                        override fun onDiscoveryStopped(serviceType: String) = Unit
                        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                            if (finished.compareAndSet(false, true)) {
                                runCatching { nsdManager.stopServiceDiscovery(this) }
                                continuation.resumeWithException(
                                    IllegalStateException("NSD discovery failed: $errorCode")
                                )
                            }
                        }

                        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
                    }
                    continuation.invokeOnCancellation {
                        handler.removeCallbacks(timeout)
                        listener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
                    }
                    nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
                }
            }
        }

    data class DiscoveredService(
        val name: String,
        val host: String,
        val port: Int
    )

    private companion object {
        const val SERVICE_TYPE = "_phub._tcp"
    }
}
