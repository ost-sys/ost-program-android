package com.ost.application.core.share

import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object SubnetScanner {
    private const val TAG = "SubnetScanner"

    fun getAllLocalIpv4AndSubnets(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return emptyList()
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        val lastDot = host.lastIndexOf('.')
                        if (lastDot > 0) {
                            val prefix = host.substring(0, lastDot + 1)
                            result.add(Pair(host, prefix))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get local IPv4s: ${e.message}")
        }
        return result
    }

    fun getLocalIpv4AndSubnet(): Pair<String, String>? {
        return getAllLocalIpv4AndSubnets().firstOrNull()
    }

    fun startSubnetScan(
        scope: CoroutineScope,
        selfDeviceName: String,
        selfFingerprint: String,
        selfPort: Int,
        onDeviceFound: (DiscoveredDevice) -> Unit
    ): Job {
        return scope.launch(Dispatchers.IO) {
            val selfInfo = LocalSendInfo(
                alias = selfDeviceName,
                version = "2.1",
                deviceModel = Build.MODEL,
                deviceType = LocalSendDeviceType.MOBILE.value,
                fingerprint = selfFingerprint,
                port = selfPort,
                protocol = "https",
                download = false
            )

            val client = LocalSendClient()
            val semaphore = Semaphore(40)

            while (isActive) {
                val subnets = getAllLocalIpv4AndSubnets()
                if (subnets.isNotEmpty()) {
                    val jobs = mutableListOf<Job>()
                    for ((selfIp, prefix) in subnets) {
                        for (i in 1..254) {
                            val targetIp = "$prefix$i"
                            if (targetIp == selfIp) continue
                            if (!isActive) break

                            val job = launch {
                                semaphore.withPermit {
                                    if (!isActive) return@withPermit
                                    val remoteInfo = client.fetchInfo(targetIp, 53317)
                                    if (remoteInfo != null && remoteInfo.fingerprint != selfFingerprint) {
                                        val device = DiscoveredDevice(
                                            id = remoteInfo.fingerprint.ifBlank { "$targetIp:${remoteInfo.port}" },
                                            name = remoteInfo.alias,
                                            type = remoteInfo.deviceType ?: "desktop",
                                            isResolved = true,
                                            ipAddress = InetAddress.getByName(targetIp),
                                            port = remoteInfo.port,
                                            isResolving = false
                                        )
                                        onDeviceFound(device)
                                        client.register(targetIp, remoteInfo.port, selfInfo)
                                    }
                                }
                            }
                            jobs.add(job)
                        }
                    }
                    jobs.joinAll()
                }
                delay(8000)
            }
        }
    }
}
