package com.assoft.peekster.activity.shareable.transfer

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel

/**
 * Individual packet of information in a transfer
 *
 * Transfers are (at a high level) essentially a stream of packets being
 * exchanged back and forth. The packet format is described here:
 * https://goo.gl/fL890p
 */
class Packet {
    /**
     * Retrieve the packet type
     * @return packet type
     */
    var type = 0
        private set

    /**
     * Retrieve the buffer for the packet
     * @return byte array
     */
    var buffer: ByteBuffer
        private set

    private var haveSize = false

    /**
     * Determine if the buffer is full
     * @return true if full
     */
    val isFull: Boolean
        get() = buffer.position() == buffer.capacity()

    /**
     * Create an empty packet
     */
    constructor() {
        buffer = ByteBuffer.allocate(5)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
    }

    /**
     * Create a packet of the specified type with the specified data
     */
    @JvmOverloads
    constructor(
        type: Int,
        data: ByteArray? = null,
        length: Int = data?.size ?: 0
    ) {
        buffer = ByteBuffer.allocate(5 + length)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(length + 1).put(type.toByte())
        if (data != null) {
            buffer.put(data, 0, length)
        }
        buffer.flip()
    }

    /**
     * Read a packet from a socket channel
     */
    @Throws(IOException::class)
    fun read(socketChannel: SocketChannel) {

        // If the 32-bit size hasn't yet been read, do so
        if (!haveSize) {
            socketChannel.read(buffer)
            if (buffer.position() != 5) {
                return
            }

            // Remaining data is 8-bit type and data
            buffer.flip()
            val size = buffer.int - 1
            type = buffer.get().toInt()
            buffer = ByteBuffer.allocate(size)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            haveSize = true
        }

        // The size is known, read data into the buffer
        socketChannel.read(buffer)
    }

    companion object {
        /**
         * Transfer succeeded
         *
         * This packet is sent by the receiver to indicate the transfer succeeded
         * and the connection may be closed.
         */
        const val SUCCESS = 0

        /**
         * Transfer failed
         *
         * This packet is sent by either end to indicate an error. The content of
         * packet describes the error.
         */
        const val ERROR = 1

        /**
         * JSON data
         */
        const val JSON = 2

        /**
         * Binary data
         */
        const val BINARY = 3
    }
}
