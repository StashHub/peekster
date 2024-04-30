package com.assoft.peekster.activity.shareable.transfer

import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import com.assoft.peekster.activity.shareable.bundle.Bundle
import com.assoft.peekster.activity.shareable.bundle.FileItem
import com.assoft.peekster.activity.shareable.bundle.UrlItem
import com.assoft.peekster.activity.shareable.util.Settings
import com.assoft.peekster.domain.NetworkDevice
import com.assoft.peekster.util.AppUtils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

/**
 * Receive incoming transfers and initiate outgoing transfers
 *
 * This service listens for new connections and instantiates Transfer instances
 * to process them. It will also initiate a transfer when the appropriate
 * intent is supplied.
 */
@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class TransferService : Service() {

    private lateinit var transferNotificationManager: TransferNotificationManager
    private lateinit var transferServer: TransferServer
    private lateinit var transferManager: TransferManager
    private lateinit var settings: Settings

    override fun onCreate() {
        super.onCreate()

        transferNotificationManager = TransferNotificationManager(this)
        try {
            transferServer =
                TransferServer(this, transferNotificationManager, object : TransferServer.Listener {
                    override fun onNewTransfer(transfer: Transfer) {
                        transfer.setId(transferNotificationManager.nextId())
                        transferManager.addTransfer(transfer)
                    }
                })
        } catch (e: IOException) {
            Timber.e("Error creating Transfer Server %s", e.message)
        }

        settings = Settings(this)
        transferManager = TransferManager(this, transferNotificationManager)
        transferNotificationManager.updateNotification()

        if (!AppUtils.checkRunningConditions(this)
            || !transferServer.start()
        ) stopSelf()
    }

    private fun hasOngoingTasks(): Boolean {
        return transferManager.transfers.size() > 0
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.let {
            Timber.i("Service starting, %s", intent.action)

            if (AppUtils.checkRunningConditions(this)) {
                if (ACTION_SERVICE_STATUS == intent.action && intent.hasExtra(EXTRA_STATUS_STARTED)) {
                    val startRequested = intent.getBooleanExtra(EXTRA_STATUS_STARTED, false)
                    val destroyApproved =
                        !startRequested && !hasOngoingTasks()  // if service start is not requested and there are no ongoing transfer, cancel service
                    Timber.i("Stop service, $destroyApproved")
                    if (destroyApproved) {
                        Handler(Looper.getMainLooper())
                            .postDelayed({
                                if (destroyApproved
                                    && !hasOngoingTasks()
                                ) {
                                    stopSelf() // Terminate Service
                                    Timber.d(
                                        "onStartCommand(): Destroy state has been applied"
                                    )
                                }
                            }, 1500) // execute this after 1.5 seconds
                        return START_NOT_STICKY
                    }
//                    transferServer.start()
                    return START_REDELIVER_INTENT
                } else if (ACTION_START_TRANSFER == intent.action) {
                    // Build the parameters needed to start the transfer
                    val device: NetworkDevice =
                        intent.getSerializableExtra(EXTRA_DEVICE_ID) as NetworkDevice

                    // Add each of the items to the bundle and send it
                    try {
                        val bundle = createBundle(
                            intent.getParcelableArrayListExtra(EXTRA_URIS)
                        )
                        var nextId = intent.getIntExtra(EXTRA_ID, 0)
                        if (nextId == 0) {
                            nextId = transferNotificationManager.nextId()
                        }
                        val transfer = Transfer(
                            device,
                            settings.deviceName
                                ?: Build.MODEL.toUpperCase(Locale.getDefault()), bundle
                        )
                        transfer.setId(nextId)
                        transferManager.addTransfer(transfer, intent)
                    } catch (e: IOException) {
                        Timber.e("Error creating bundle %s", e.message)
                        transferNotificationManager.stopService()
                    }
                    return START_NOT_STICKY
                } else if (ACTION_STOP_TRANSFER == intent.action) {
                    return stopTransfer(intent)
                } else if (ACTION_REMOVE_TRANSFER == intent.action) {
                    // Remove (dismiss) a transfer that has completed
                    val id = intent.getIntExtra(EXTRA_TRANSFER, -1)
                    transferManager.removeTransfer(id)
                    transferNotificationManager.removeNotification(id)
                    transferNotificationManager.stopService()
                    return START_NOT_STICKY
                } else if (ACTION_BROADCAST == intent.action) {
                    return broadcast()
                }
            }
        }

        // If we get killed, after returning from here, don't restart
        return START_STICKY
    }

    /**
     * Stop a transfer in progress
     */
    private fun stopTransfer(intent: Intent): Int {
        transferManager.stopTransfer(intent.getIntExtra(EXTRA_TRANSFER, -1))
        return START_NOT_STICKY
    }

    /**
     * Trigger a broadcast for all transfers
     */
    private fun broadcast(): Int {
        transferManager.broadcastTransfers()
        transferNotificationManager.stopService()
        return START_NOT_STICKY
    }

    /**
     * Create a bundle from the list of URIs
     * @param uriList list of URIs to add
     * @return newly created bundle
     */
    @Throws(IOException::class)
    private fun createBundle(uriList: ArrayList<Uri>?): Bundle {
        val bundle = Bundle()
        if (uriList != null) {
            for (parcelable in uriList) {
                when (parcelable.scheme) {
                    ContentResolver.SCHEME_ANDROID_RESOURCE, ContentResolver.SCHEME_CONTENT -> bundle.addItem(
                        FileItem(
                            getAssetFileDescriptor(parcelable),
                            getFilename(parcelable)
                        )
                    )
                    ContentResolver.SCHEME_FILE -> {
                        val file = File(parcelable.path!!)
                        if (file.isDirectory) {
                            traverseDirectory(file, bundle)
                        } else {
                            bundle.addItem(FileItem(file))
                        }
                    }
                }
            }
        }
        return bundle
    }

    /**
     * Traverse a directory tree and add all files to the bundle
     * @param root the directory to which all filenames will be relative
     * @param bundle target for all files that are found
     */
    @Throws(IOException::class)
    private fun traverseDirectory(root: File, bundle: Bundle) {
        val stack = Stack<File>()
        stack.push(root)
        while (!stack.empty()) {
            val topOfStack = stack.pop()
            for (f in topOfStack.listFiles()) {
                if (f.isDirectory) {
                    stack.push(f)
                } else {
                    val relativeFilename = f.absolutePath.substring(
                        root.parentFile.absolutePath.length + 1
                    )
                    bundle.addItem(FileItem(f, relativeFilename))
                }
            }
        }
    }

    /**
     * Determine the appropriate filename for a URI
     * @param uri URI to use for filename
     * @return filename
     */
    private fun getFilename(uri: Uri): String {
        var filename = uri.lastPathSegment
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME
        )
        val query = applicationContext.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )
        query?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn)
                filename = File(name).name
            }
        }
        return filename as String
    }

    /**
     * Attempt to resolve the provided URI
     * @param uri URI to resolve
     * @return file descriptor
     */
    @Throws(IOException::class)
    private fun getAssetFileDescriptor(uri: Uri): AssetFileDescriptor? {
        return try {
            contentResolver.openAssetFileDescriptor(uri, "r")
        } catch (e: FileNotFoundException) {
            throw IOException(
                String.format(
                    "unable to resolve \"%s\"",
                    uri.toString()
                )
            )
        }
            ?: throw IOException(
                String.format(
                    "no file descriptor for \"%s\"",
                    uri.toString()
                )
            )
    }

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("Service destroyed")
        transferServer.stop()
        transferNotificationManager.stopService()
        stopForeground(true)

    }

    companion object {
        const val ACTION_SERVICE_STATUS = "com.assoft.peekster.SERVICE_STATUS"
        const val ACTION_START_TRANSFER = "com.assoft.peekster.START_TRANSFER"
        const val ACTION_STOP_TRANSFER = "com.assoft.peekster.STOP_TRANSFER"
        const val ACTION_REMOVE_TRANSFER = "com.assoft.peekster.REMOVE_TRANSFER"
        const val ACTION_BROADCAST = "com.assoft.peekster.BROADCAST"

        const val EXTRA_STATUS_STARTED = "extraStatusStarted"
        const val EXTRA_DEVICE_ID = "extraDeviceId"
        const val EXTRA_URIS = "com.assoft.peekster.URLS"
        const val EXTRA_ID = "com.assoft.peekster.ID"
        const val EXTRA_TRANSFER = "com.assoft.peekster.transfer"

        /**
         * Start or stop the service
         * @param context context to use for sending the intent
         * @param start true to start the service; false to stop it
         */
        fun startStopService(
            context: Context,
            start: Boolean
        ) {
            val intent = Intent(context, TransferService::class.java)
            if (start) {
                Timber.i("Sending intent to start service")
                intent.action = ACTION_SERVICE_STATUS
            } else {
                Timber.i("Sending intent to stop service")
                intent.action = ACTION_SERVICE_STATUS
            }

            // Android O doesn't allow certain broadcasts to start services as per usual
            AppUtils.startForegroundService(context, intent)
        }
    }
}