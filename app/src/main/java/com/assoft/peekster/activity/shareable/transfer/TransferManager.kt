package com.assoft.peekster.activity.shareable.transfer

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.util.SparseArray
import com.assoft.peekster.activity.shareable.bundle.FileItem
import com.assoft.peekster.activity.shareable.bundle.Item
import com.assoft.peekster.activity.shareable.bundle.UrlItem
import timber.log.Timber
import java.io.IOException

/**
 * Manage active transfers
 */
class TransferManager constructor(
    private val context: Context,
    private val transferNotificationManager: TransferNotificationManager
) {
    val transfers: SparseArray<Transfer> = SparseArray<Transfer>()

    private val mediaScannerConnection: MediaScannerConnection =
        MediaScannerConnection(context, object : MediaScannerConnectionClient {
            override fun onMediaScannerConnected() {
                Timber.i("Connected to MediaScanner")
            }

            override fun onScanCompleted(path: String, uri: Uri) {}
        })

    /**
     * Broadcast the status of a transfer
     */
    private fun broadcastTransferStatus(transferStatus: TransferStatus) {
        val intent = Intent().apply {
            action = TRANSFER_UPDATED
            putExtra(EXTRA_STATUS, transferStatus)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Add a transfer to the list
     */
    fun addTransfer(transfer: Transfer, intent: Intent? = null) {

        // Grab the initial status
        val transferStatus: TransferStatus = transfer.status
        Timber.i(
            String.format("starting transfer #%d...", transferStatus.id)
        )

        // Add a listener for status change events
        transfer.addStatusChangedListener(object : Transfer.StatusChangedListener {
            override fun onStatusChanged(transferStatus: TransferStatus) {

                // Broadcast transfer status
                broadcastTransferStatus(transferStatus)

                // Update the transfer notification manager
                transferNotificationManager.updateTransfer(transferStatus, intent)
            }
        })

        // Add a listener for items being received
        transfer.addItemReceivedListener(object : Transfer.ItemReceivedListener {
            override fun onItemReceived(item: Item) {
                if (mediaScannerConnection.isConnected &&
                    item is FileItem
                ) {
                    val path: String = item.path
                    mediaScannerConnection.scanFile(path, null)
                } else if (item is UrlItem) {
                    try {
                        transferNotificationManager.showUrl(item.url)
                    } catch (e: IOException) {
                        Timber.e("Error showing URL %s", e.message)
                    }
                }
            }
        })

        // Add the transfer to the list
        synchronized(transfers) { transfers.append(transferStatus.id, transfer) }

        // Add the transfer to the notification manager and immediately update it
        transferNotificationManager.addTransfer(transferStatus)
        transferNotificationManager.updateTransfer(transferStatus, intent)

        // Create a new thread and run the transfer in it
        Thread(transfer).start()
    }

    /**
     * Stop the transfer with the specified ID
     */
    fun stopTransfer(id: Int) {
        mediaScannerConnection.disconnect()
        synchronized(transfers) {
            val transfer: Transfer? = transfers[id]
            if (transfer != null) {
                Timber.i(
                    String.format(
                        "Stopping transfer #%d...",
                        transfer.status.id
                    )
                )
                transfer.stop()
            }
        }
    }

    /**
     * Remove the transfer with the specified ID
     *
     * Transfers that are in progress cannot be removed and a warning is logged
     * if this is attempted.
     */
    fun removeTransfer(id: Int) {
        synchronized(transfers) {
            val transfer: Transfer? = transfers[id]
            if (transfer != null) {
                val transferStatus: TransferStatus = transfer.status
                if (!transferStatus.isFinished) {
                    Timber.w(
                        String.format(
                            "Cannot remove ongoing transfer #%d",
                            transferStatus.id
                        )
                    )
                    return
                }
                transfers.remove(id)
            }
        }
    }

    /**
     * Trigger a broadcast of all transfers
     */
    fun broadcastTransfers() {
        for (i in 0 until transfers.size()) {
            broadcastTransferStatus(transfers.valueAt(i).status)
        }
    }

    companion object {
        const val TRANSFER_UPDATED = "com.assoft.peekster.transfer_updated"
        const val EXTRA_STATUS = "com.assoft.peekster.status"
    }
}
