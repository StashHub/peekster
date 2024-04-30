package com.assoft.peekster.activity.shareable.stream

import timber.log.Timber
import java.io.IOException
import java.net.*
import java.util.*
import java.util.concurrent.Executors

class HttpStreamServer(
    private var port: Int,
    private var fileUris: ArrayList<UriInterpretation>,
    private var launcherActivity: StreamActivity
) : Thread() {
    private var webServerLoop = true

    private fun getServerUrl(ipAddress: String): String {
        var ip = ipAddress
        if (port == 80) {
            return "http://$ipAddress/"
        }
        if (ipAddress.indexOf(":") >= 0) {
            // IPv6
            val pos = ipAddress.indexOf("%")
            // java insists in adding %wlan and %p2p0 to everything
            if (pos > 0) {
                ip = ipAddress.substring(0, pos)
            }
            return "http://[$ip]:$port/"
        }
        return "http://$ip:$port/"
    }

    @Synchronized
    fun stopServer() {
        Timber.i("Closing server...")
        webServerLoop = false
        serverSocket?.let {
            try {
                serverSocket?.close()
                serverSocket = null
            } catch (e: IOException) {
                Timber.e("Error stopping server : %s, ", e.message)
                e.printStackTrace();
            }
        }
    }

    fun getSocket() = serverSocket

    fun listOfIpAddresses(): Array<CharSequence> {
        val arrayOfIps = ArrayList<String>()
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val nextInterface = en.nextElement()
                Timber.i("Interface : %s", nextInterface.displayName)
                val iNetAddress = nextInterface.inetAddresses

                while (iNetAddress.hasMoreElements()) {
                    val nextAddress = iNetAddress.nextElement()
                    val theIpTemp = nextAddress.hostAddress
                    val theIp = getServerUrl(theIpTemp)
                    if (nextAddress is Inet6Address
                        || nextAddress.isLoopbackAddress
                    ) {
                        arrayOfIps.add(theIp)
                        continue
                    }
                    arrayOfIps.add(0, theIp) // we prefer non local IPv4
                }
            }
            if (arrayOfIps.size == 0) {
                val firstIp = getServerUrl("0.0.0.0")
                arrayOfIps.add(firstIp)
            }
        } catch (ex: SocketException) {
            Timber.e("httpServer %s", ex.toString())
        }
        return arrayOfIps.toTypedArray()
    }

    private fun normalBind(thePort: Int): Boolean {
        Timber.i("Attempting to bind on port $thePort")
        serverSocket = try {
            ServerSocket()
        } catch (e: Exception) {
            Timber.e("Fatal Error: %s", e.message)
            return false
        }
        serverSocket?.reuseAddress = true
        serverSocket?.bind(InetSocketAddress(thePort))
        port = thePort
        Timber.i("Binding was OK!")
        return true
    }

    override fun run() {
        Timber.i("Starting Server...")
        if (!normalBind(port)) {
            return
        }

        // go in a infinite loop, wait for connections, process request, send
        // response
        while (true) {
            Timber.i("Ready, Waiting for requests...\n")
            try {
                val connectionSocket = serverSocket?.accept()
                val theHttpConnection = HttpServerConnection(
                    fileUris,
                    connectionSocket!!,
                    launcherActivity
                )
                threadPool.submit(theHttpConnection)
            } catch (e: IOException) {
                Timber.e("Exception %s", e.message)
                e.printStackTrace()
                break
            } catch (e: NullPointerException) {
                Timber.e("Exception %s", e.message)
                e.printStackTrace()
                break
            }
        }
    }

    companion object {
        private val threadPool = Executors.newCachedThreadPool()
        private var serverSocket: ServerSocket? = null

        val localIpAddress: String
            get() {
                try {
                    var localAddress: InetAddress? = null
                    var ipv6: InetAddress? = null
                    val en = NetworkInterface
                        .getNetworkInterfaces()
                    while (en.hasMoreElements()) {
                        val intf = en.nextElement()
                        val enumIpAddr = intf
                            .inetAddresses
                        while (enumIpAddr.hasMoreElements()) {
                            val inetAddress = enumIpAddr.nextElement()
                            if (inetAddress is Inet6Address) {
                                ipv6 = inetAddress
                                continue
                            }
                            if (inetAddress.isLoopbackAddress) {
                                localAddress = inetAddress
                                continue
                            }
                            return inetAddress.hostAddress.toString()
                        }
                    }
                    return ipv6?.hostAddress?.toString() ?: (localAddress?.hostAddress?.toString()
                        ?: "0.0.0.0")
                } catch (ex: SocketException) {
                    Timber.e("httpServer %s", ex.toString())
                }
                return "0.0.0.0"
            }
    }

    init {
        if (serverSocket == null) {
            start()
        }
    }
}