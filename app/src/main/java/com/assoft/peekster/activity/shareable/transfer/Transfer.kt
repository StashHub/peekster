package com.assoft.peekster.activity.shareable.transfer

import com.assoft.peekster.activity.shareable.bundle.Bundle
import com.assoft.peekster.activity.shareable.bundle.FileItem
import com.assoft.peekster.activity.shareable.bundle.Item
import com.assoft.peekster.activity.shareable.bundle.UrlItem
import com.assoft.peekster.domain.NetworkDevice
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.util.*

/**
 * Perform a transfer from one device to another
 *
 * This class takes care of communicating (via socket) with another device to
 * transfer a bundle (list of items) using packets.
 */
class Transfer : Runnable {
    /**
     * Listener for status changes
     */
    interface StatusChangedListener {
        fun onStatusChanged(transferStatus: TransferStatus)
    }

    /**
     * Listener for item received events
     */
    interface ItemReceivedListener {
        fun onItemReceived(item: Item)
    }

    /**
     * Transfer header
     */
    private inner class TransferHeader {
        var name: String? = null
        var count: String? = null
        var size: String? = null
    }

    // Internal state of the transfer
    private enum class InternalState {
        TransferHeader, ItemHeader, ItemContent, Finished
    }

    private var transferStatus: TransferStatus

    @Volatile
    private var stopTransfer = false

    private val statusChangedListeners: MutableList<StatusChangedListener> = mutableListOf()
    private val itemReceivedListeners: MutableList<ItemReceivedListener> = mutableListOf()

    private var device: NetworkDevice? = null
    private var bundle: Bundle? = null
    private var deviceName: String? = null
    private lateinit var transferDirectory: String
    private var overwrite = false
    private var socketChannel: SocketChannel
    private val channelSelector = Selector.open()
    private var internalState = InternalState.TransferHeader
    private var receivingPacket: Packet? = null
    private var sendingPacket: Packet? = null
    private var transferItems = 0
    private var transferBytesTotal: Long = 0
    private var bytesTransferred: Long = 0
    private lateinit var item: Item
    private var itemIndex = 0
    private var itemBytesRemaining: Long = 0

    /**
     * Create a transfer for receiving items
     * @param socketChannel incoming channel
     * @param transferDirectory directory for incoming files
     * @param overwrite true to overwrite existing files
     * @param unknownDeviceName device name shown before being received
     */
    constructor(
        socketChannel: SocketChannel,
        transferDirectory: String,
        overwrite: Boolean,
        unknownDeviceName: String?
    ) {
        transferStatus = TransferStatus(
            remoteDeviceName = unknownDeviceName,
            direction = TransferStatus.Direction.Receive, state = TransferStatus.State.Transferring
        )
        this.transferDirectory = transferDirectory
        this.overwrite = overwrite
        this.socketChannel = socketChannel
        this.socketChannel.configureBlocking(false)
    }

    /**
     * Create a transfer for sending items
     * @param device device to connect to
     * @param deviceName device name to send to the remote device
     * @param bundle bundle to transfer
     */
    constructor(device: NetworkDevice, deviceName: String, bundle: Bundle) {
        transferStatus = TransferStatus(
            remoteDeviceName = device.nick_name,
            direction = TransferStatus.Direction.Send, state = TransferStatus.State.Connecting
        )
        this.device = device
        this.bundle = bundle
        this.deviceName = deviceName
        socketChannel = SocketChannel.open()
        socketChannel.configureBlocking(false)
        transferItems = bundle.size
        transferBytesTotal = bundle.totalSize
        transferStatus.bytesTotal = transferBytesTotal
    }

    /**
     * Set the transfer ID
     */
    fun setId(id: Int) {
        synchronized(transferStatus) { transferStatus.id = id }
    }

    /**
     * Retrieve the current transfer status
     * @return copy of the current status
     */
    val status: TransferStatus
        get() {
            synchronized(transferStatus) {
                return TransferStatus(
                    id = transferStatus.id,
                    remoteDeviceName = transferStatus.remoteDeviceName,
                    direction = transferStatus.direction,
                    state = transferStatus.state,
                    progress = transferStatus.progress,
                    bytesTransferred = transferStatus.bytesTransferred,
                    bytesTotal = transferStatus.bytesTotal,
                    error = transferStatus.error
                )
            }
        }

    /**
     * Close the socket and wake the selector, effectively aborting the transfer
     */
    fun stop() {
        stopTransfer = true
        channelSelector.wakeup()
    }

    /**
     * Add a listener for status changes
     *
     * This method should not be invoked after starting the transfer.
     */
    fun addStatusChangedListener(statusChangedListener: StatusChangedListener) {
        statusChangedListeners.add(statusChangedListener)
    }

    /**
     * Add a listener for items being recieved
     *
     * This method should not be invoked after starting the transfer.
     */
    fun addItemReceivedListener(itemReceivedListener: ItemReceivedListener) {
        itemReceivedListeners.add(itemReceivedListener)
    }

    /**
     * Notify all listeners that the status has changed
     */
    private fun notifyStatusChangedListeners() {
        for (statusChangedListener in statusChangedListeners) {
            statusChangedListener.onStatusChanged(
                TransferStatus(
                    id = transferStatus.id,
                    remoteDeviceName = transferStatus.remoteDeviceName,
                    direction = transferStatus.direction,
                    state = transferStatus.state,
                    progress = transferStatus.progress,
                    bytesTransferred = transferStatus.bytesTransferred,
                    bytesTotal = transferStatus.bytesTotal,
                    error = transferStatus.error
                )
            )
        }
    }

    /**
     * Update current transfer progress
     */
    private fun updateProgress() {
        val newProgress =
            (100.0 * if (transferBytesTotal != 0L) bytesTransferred.toDouble() / transferBytesTotal.toDouble() else 0.0).toInt()
        if (newProgress != transferStatus.progress) {
            synchronized(transferStatus) {
                transferStatus.progress = newProgress
                transferStatus.bytesTransferred = bytesTransferred
                notifyStatusChangedListeners()
            }
        }
    }

    /**
     * Process the transfer header
     */
    @Throws(IOException::class)
    private fun processTransferHeader() {
        val transferHeader: TransferHeader
        try {
            transferHeader = mGson.fromJson(
                receivingPacket?.buffer?.array()?.let {
                    String(
                        it, Charset.forName("UTF-8")
                    )
                },
                TransferHeader::class.java
            )
            transferItems = transferHeader.count?.toInt() ?: 0
            transferBytesTotal = transferHeader.size?.toLong() ?: 0
        } catch (e: JsonSyntaxException) {
            throw IOException(e.message)
        } catch (e: NumberFormatException) {
            throw IOException(e.message)
        }
        internalState =
            if (itemIndex == transferItems) InternalState.Finished else InternalState.ItemHeader
        synchronized(transferStatus) {
            transferStatus.remoteDeviceName = transferHeader.name
            transferStatus.bytesTotal = transferBytesTotal
            notifyStatusChangedListeners()
        }
    }

    /**
     * Process the header for an individual item
     */
    @Throws(IOException::class)
    private fun processItemHeader() {
        val type = object :
            TypeToken<Map<String?, Any?>?>() {}.type
        val map: MutableMap<String, Any>
        map = try {
            mGson.fromJson<MutableMap<String, Any>>(
                receivingPacket?.buffer?.array()?.let {
                    String(
                        it,
                        Charset.forName("UTF-8")
                    )
                }, type
            )
        } catch (e: JsonSyntaxException) {
            throw IOException(e.message)
        }
        var itemType = map[Item.TYPE] as String?
        if (itemType == null) {
            itemType = FileItem.TYPE_NAME
        }
        item = when (itemType) {
            FileItem.TYPE_NAME -> FileItem(transferDirectory, map, overwrite)
            UrlItem.TYPE_NAME -> UrlItem(map)
            else -> throw IOException("Unrecognized item type")
        }
        val itemSize: Long? = item.getLongProperty(Item.SIZE, true)
        if (itemSize != 0L) {
            internalState = InternalState.ItemContent
            item.open(Item.Mode.Write)
            if (itemSize != null) {
                itemBytesRemaining = itemSize
            }
        } else {
            processNext()
        }
    }

    /**
     * Process item contents
     */
    @Throws(IOException::class)
    private fun processItemContent() {
        item.write(receivingPacket?.buffer?.array())
        val numBytes: Int? = receivingPacket?.buffer?.capacity()
        bytesTransferred += numBytes?.toLong() ?: 0
        itemBytesRemaining -= numBytes?.toLong() ?: 0
        updateProgress()
        if (itemBytesRemaining <= 0) {
            item.close()
            processNext()
        }
    }

    /**
     * Prepare to process the next item
     */
    private fun processNext() {
        itemIndex++
        internalState =
            if (itemIndex == transferItems) InternalState.Finished else InternalState.ItemHeader
        for (itemReceivedListener in itemReceivedListeners) {
            itemReceivedListener.onItemReceived(item)
        }
    }

    /**
     * Process the next packet by reading it and then invoking the correct method
     * @return true if there are more packets expected
     */
    @Throws(IOException::class)
    private fun processNextPacket(): Boolean {
        if (receivingPacket == null) {
            receivingPacket = Packet()
        }
        receivingPacket?.read(socketChannel)
        return if (receivingPacket?.isFull!!) {
            if (receivingPacket?.type == Packet.ERROR) {
                throw IOException(
                    receivingPacket?.buffer?.array()?.let {
                         String(
                            it,
                            Charset.forName("UTF-8")
                        )
                    }
                )
            }
            if (transferStatus.direction == TransferStatus.Direction.Receive) {
                if (internalState == InternalState.TransferHeader && receivingPacket?.type == Packet.JSON) {
                    processTransferHeader()
                } else if (internalState == InternalState.ItemHeader && receivingPacket?.type == Packet.JSON) {
                    processItemHeader()
                } else if (internalState == InternalState.ItemContent && receivingPacket?.type == Packet.BINARY) {
                    processItemContent()
                } else {
                    throw IOException("Unexpected packet")
                }
                receivingPacket = null
                internalState != InternalState.Finished
            } else {
                if (internalState == InternalState.Finished && receivingPacket?.type == Packet.SUCCESS) {
                    false
                } else {
                    throw IOException("Unexpected packet")
                }
            }
        } else {
            true
        }
    }

    /**
     * Send the transfer header
     */
    private fun sendTransferHeader() {
        val map: MutableMap<String, String?> =
            HashMap()
        map["name"] = this.deviceName
        map["count"] = this.bundle?.size.toString()
        map["size"] = this.bundle?.totalSize.toString()
        sendingPacket = Packet(
            Packet.JSON, mGson.toJson(map).toByteArray(
                Charset.forName("UTF-8")
            )
        )
        internalState =
            if (itemIndex == transferItems) InternalState.Finished else InternalState.ItemHeader
    }

    /**
     * Send the header for an individual item
     */
    @Throws(IOException::class)
    private fun sendItemHeader() {
        item = bundle?.get(itemIndex)!!
        sendingPacket = Packet(
            Packet.JSON, mGson.toJson(
                item.properties
            ).toByteArray(Charset.forName("UTF-8"))
        )
        val itemSize: Long? = item.getLongProperty(Item.SIZE, true)
        if (itemSize != 0L) {
            internalState = InternalState.ItemContent
            item.open(Item.Mode.Read)
            if (itemSize != null) {
                itemBytesRemaining = itemSize
            }
        } else {
            itemIndex++
            internalState =
                if (itemIndex == transferItems) InternalState.Finished else InternalState.ItemHeader
        }
    }

    /**
     * Send item contents
     */
    @Throws(IOException::class)
    private fun sendItemContent() {
        val buffer = ByteArray(CHUNK_SIZE)
        val numBytes: Int = item.read(buffer)
        sendingPacket = Packet(Packet.BINARY, buffer, numBytes)
        bytesTransferred += numBytes.toLong()
        itemBytesRemaining -= numBytes.toLong()
        updateProgress()
        if (itemBytesRemaining <= 0) {
            item.close()
            itemIndex++
            internalState =
                if (itemIndex == transferItems) InternalState.Finished else InternalState.ItemHeader
        }
    }

    /**
     * Send the next packet by evaluating the current state
     * @return true if there are more packets to send
     */
    @Throws(IOException::class)
    private fun sendNextPacket(): Boolean {
        if (sendingPacket == null) {
            if (transferStatus.direction == TransferStatus.Direction.Receive) {
                sendingPacket = Packet(Packet.SUCCESS)
            } else {
                when (internalState) {
                    InternalState.TransferHeader -> sendTransferHeader()
                    InternalState.ItemHeader -> sendItemHeader()
                    InternalState.ItemContent -> sendItemContent()
                    else -> throw IOException("Unreachable code")
                }
            }
        }
        socketChannel.write(sendingPacket?.buffer as ByteBuffer)
        if (sendingPacket?.isFull!!) {
            sendingPacket = null
            return internalState != InternalState.Finished
        }
        return true
    }

    /**
     * Perform the transfer until it completes or an error occurs
     */
    override fun run() {
        try {
            // Indicate which operations select() should select for
            val selectionKey = socketChannel.register(
                channelSelector,
                if (transferStatus.direction == TransferStatus.Direction.Receive) SelectionKey.OP_READ else SelectionKey.OP_CONNECT
            )

            // For a sending transfer, connect to the remote device
            if (transferStatus.direction == TransferStatus.Direction.Send) {
                socketChannel.connect(
                    InetSocketAddress(
                        device?.host,
                        device?.port!!
                    )
                )
            }
            while (true) {
                channelSelector.select()
                if (stopTransfer) {
                    break
                }
                if (selectionKey.isConnectable) {
                    socketChannel.finishConnect()
                    selectionKey.interestOps(SelectionKey.OP_READ or SelectionKey.OP_WRITE)
                    synchronized(transferStatus) {
                        transferStatus.state = TransferStatus.State.Transferring
                        notifyStatusChangedListeners()
                    }
                }
                if (selectionKey.isReadable) {
                    if (!processNextPacket()) {
                        if (transferStatus.direction == TransferStatus.Direction.Receive) {
                            selectionKey.interestOps(SelectionKey.OP_WRITE)
                        } else {
                            break
                        }
                    }
                }
                if (selectionKey.isWritable) {
                    if (!sendNextPacket()) {
                        if (transferStatus.direction == TransferStatus.Direction.Receive) {
                            break
                        } else {
                            selectionKey.interestOps(SelectionKey.OP_READ)
                        }
                    }
                }
            }

            // Close the socket
            socketChannel.close()

            // If interrupted, throw an error
            if (stopTransfer) {
                throw IOException("Transfer was cancelled")
            }

            // Indicate success
            synchronized(transferStatus) {
                transferStatus.state = TransferStatus.State.Succeeded
                notifyStatusChangedListeners()
            }
        } catch (e: IOException) {
            synchronized(transferStatus) {
                transferStatus.state = TransferStatus.State.Failed
                transferStatus.error = e.message
                notifyStatusChangedListeners()
            }
        }
    }

    companion object {
        private const val CHUNK_SIZE = 65536
        private val mGson = Gson()
    }
}
