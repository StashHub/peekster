package com.assoft.peekster.domain

import android.net.nsd.NsdServiceInfo
import com.assoft.peekster.domain.NetworkDevice.Companion.SERVICE_TYPE
import java.io.Serializable
import java.net.InetAddress

/**
 * An object which represents a single [NetworkDevice].
 */
class NetworkDevice(
    var device_id: String? = "",
    var nick_name: String? = "",
    var host: InetAddress? = null,
    var port: Int = 0,
    var brand: String? = "",
    var model: String? = "",
    var version_name: String? = "",
    var version_number: Int? = 0
) : Serializable {
    companion object {
        const val SERVICE_TYPE = "_peekster._tcp."
    }
}

fun NetworkDevice.toServiceInfo(): NsdServiceInfo {
    val serviceInfo = NsdServiceInfo()
    serviceInfo.serviceType = SERVICE_TYPE
    serviceInfo.serviceName = nick_name
    serviceInfo.port = port
    return serviceInfo
}