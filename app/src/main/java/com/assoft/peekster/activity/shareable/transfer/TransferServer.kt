package com.assoft.peekster.activity.shareable.transfer

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.RegistrationListener
import android.net.nsd.NsdServiceInfo
import android.os.Environment
import com.assoft.peekster.R
import com.assoft.peekster.activity.shareable.util.Settings
import com.assoft.peekster.domain.NetworkDevice
import com.assoft.peekster.domain.toServiceInfo
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel

/**
 * Listen for new connections and create Transfers for them
 */
class TransferServer(
    var context: Context,
    var transferNotificationManager: TransferNotificationManager,
    var listener: Listener
) : Runnable {

    interface Listener {
        fun onNewTransfer(transfer: Transfer)
    }

    var serverThread: Thread = Thread(this)
    private var stopTransferServer = false

    private var settings: Settings = Settings(context)
    private val channelSelector = Selector.open()

    private val registrationListener: RegistrationListener = object : RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            Timber.i("Service registered")
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            Timber.i("Service unregistered")
        }

        override fun onRegistrationFailed(
            serviceInfo: NsdServiceInfo,
            errorCode: Int
        ) {
            Timber.e(
                String.format("Registration failed: %d", errorCode)
            )
        }

        override fun onUnregistrationFailed(
            serviceInfo: NsdServiceInfo,
            errorCode: Int
        ) {
            Timber.e(
                String.format("Unregistered failed: %d", errorCode)
            )
        }
    }

    /**
     * Start the server if it is not already running
     */
    fun start(): Boolean {
        if (!serverThread.isAlive) {
            stopTransferServer = false
            serverThread.start()
            return true
        }
        return false
    }

    /**
     * Stop the transfer server if it is running and wait for it to finish
     */
    fun stop() {
        if (serverThread.isAlive) {
            stopTransferServer = true
            channelSelector.wakeup()
            try {
                serverThread.join()
            } catch (e: InterruptedException) {
                Timber.e("Error, connection interrupted %s", e.message)
            }
        }
    }

    override fun run() {
        Timber.i("Starting server...")

        // Inform the notification manager that the server has started
        transferNotificationManager.startListening()
        var nsdManager: NsdManager? = null

        try {
            // Create a server and attempt to bind to a port
            val serverSocketChannel = ServerSocketChannel.open()
            serverSocketChannel.socket().bind(InetSocketAddress(40818))
            serverSocketChannel.configureBlocking(false)
            Timber.i(
                String.format(
                    "Server bound to port %d",
                    serverSocketChannel.socket().localPort
                )
            )

            // Register the service
            nsdManager =
                context.getSystemService(Context.NSD_SERVICE) as NsdManager
            val uuid = settings.deviceUUID
            settings.deviceUUID = uuid
            nsdManager.registerService(
                NetworkDevice(
                    nick_name = settings.deviceName,
                    device_id = uuid,
                    host = null,
                    port = 40818
                ).toServiceInfo(),
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )

            // Register the server with the selector
            val selectionKey = serverSocketChannel.register(
                channelSelector,
                SelectionKey.OP_ACCEPT
            )

            // Create Transfers as new connections come in
            while (true) {
                channelSelector.select()
                if (stopTransferServer) {
                    break
                }
                if (selectionKey.isAcceptable) {
                    Timber.i("Accepting incoming connection")
                    val socketChannel = serverSocketChannel.accept()
                    val unknownDeviceName = context.getString(
                        R.string.unknown
                    )

                    val storage = Environment.getExternalStorageDirectory()
                    val downloads = File(storage, "Download")
                    val directory = File(downloads, "Peekster")
                    if (!directory.exists()) directory.mkdir()
                    listener.onNewTransfer(
                        Transfer(
                            socketChannel,
                            directory.absolutePath,
                            settings.behaviorOverwrite,
                            unknownDeviceName
                        )
                    )
                }
            }

            // Close the server socket
            serverSocketChannel.close()
        } catch (e: IOException) {
            Timber.e("Error creating server %s", e.message)
        }

        // Unregister the service
        nsdManager?.unregisterService(registrationListener)

        // Inform the notification manager that the server has stopped
        transferNotificationManager.stopListening()
        Timber.i("Server stopped")
    }
}
