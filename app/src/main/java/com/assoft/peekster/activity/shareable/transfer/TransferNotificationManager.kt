package com.assoft.peekster.activity.shareable.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.assoft.peekster.R
import com.assoft.peekster.activity.shareable.ui.TransferActivity
import com.assoft.peekster.activity.shareable.util.Settings
import timber.log.Timber

/**
 * Manage notifications and service lifecycle
 *
 * A persistent notification is shown as long as the transfer service is
 * running. A notification is also shown for each transfer in progress,
 * enabling it to be individually cancelled or retried.
 */
class TransferNotificationManager(
    private val service: Service
) {

    private val notificationManager: NotificationManager = service.getSystemService(
        Service.NOTIFICATION_SERVICE
    ) as NotificationManager

    /**
     * Create the intent for opening the main activity
     */
    private val pendingIntent: PendingIntent =
        Intent(service, TransferActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                service, 0, notificationIntent,
                0
            )
        }

    private val builder: NotificationCompat.Builder = createBuilder(SERVICE_CHANNEL_ID)
        .setContentIntent(pendingIntent)
        .setContentTitle(service.getString(R.string.service_transfer_server_title))
        .setSmallIcon(R.mipmap.ic_launcher).also { builder ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.priority = NotificationManagerCompat.IMPORTANCE_MIN
            } else {
                builder.priority = NotificationCompat.PRIORITY_MIN
            }

        }

    private var listening = false
    private var numberOfTransfers = 0
    private var nextTransferId = 2

    private var settings: Settings = Settings(service)

    /**
     * Create a notification manager for the specified service
     */
    init {

        // Android O requires the notification channels to be created
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(
                SERVICE_CHANNEL_ID,
                R.string.channel_service_name,
                NotificationManager.IMPORTANCE_MIN,
                false
            )
            createChannel(
                TRANSFER_CHANNEL_ID,
                R.string.channel_transfer_name,
                NotificationManager.IMPORTANCE_LOW,
                false
            )
            createChannel(
                NOTIFICATION_CHANNEL_ID,
                R.string.channel_notification_name,
                NotificationManager.IMPORTANCE_DEFAULT,
                true
            )
        }
    }

    /**
     * Create and register a notification channel
     * @param channelId unique ID for the channel
     * @param nameResId string resource for the channel name
     * @param importance notification priority
     * @param flash true to enable lights and vibration
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createChannel(
        channelId: String,
        @StringRes nameResId: Int,
        importance: Int,
        flash: Boolean
    ) {
        val channel = NotificationChannel(
            channelId,
            service.getString(nameResId), importance
        )
        if (flash) {
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create a new notification using the method appropriate to the build
     * @return notification
     */
    private fun createBuilder(channelId: String): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(service, channelId)
        } else {
            NotificationCompat.Builder(service)
        }
    }

    /**
     * Retrieve the next unique integer for a transfer
     * @return new ID
     *
     * The notification with ID equal to 1 is for the persistent notification
     * shown while the service is active.
     */
    @Synchronized
    fun nextId(): Int {
        return nextTransferId++
    }

    /**
     * Indicate that the server is listening for transfers
     */
    @Synchronized
    fun startListening() {
        listening = true
        updateNotification()
    }

    /**
     * Indicate that the server has stopped listening for transfers
     */
    @Synchronized
    fun stopListening() {
        listening = false
        stop()
    }

    /**
     * Stop the service if no tasks are active
     */
    @Synchronized
    fun stopService() {
        stop()
    }

    /**
     * Remove a notification
     */
    fun removeNotification(id: Int) {
        notificationManager.cancel(id)
    }

    /**
     * Add a new transfer
     */
    @Synchronized
    fun addTransfer(transferStatus: TransferStatus) {
        numberOfTransfers++
        updateNotification()

        // Clear any existing notification (this shouldn't be necessary, but it is :P)
        removeNotification(transferStatus.id)
    }

    /**
     * Update a transfer in progress
     */
    @Synchronized
    fun updateTransfer(transferStatus: TransferStatus, intent: Intent?) {
        if (transferStatus.isFinished) {
            Timber.i(
                String.format("#%d finished", transferStatus.id)
            )

            // Close the ongoing notification (yes, again)
            notificationManager.cancel(transferStatus.id)

            // Do not show a notification for successful transfers that contain no content
            if (transferStatus.state != TransferStatus.State.Succeeded ||
                transferStatus.bytesTotal > 0
            ) {

                // Prepare an appropriate notification for the transfer
                val contentText: CharSequence
                val icon: Int
                if (transferStatus.state == TransferStatus.State.Succeeded) {
                    contentText = service.getString(
                        R.string.service_transfer_status_success,
                        transferStatus.remoteDeviceName
                    )
                    icon = R.drawable.ic_check_black_24dp
                } else {
                    contentText = service.getString(
                        R.string.service_transfer_status_error,
                        transferStatus.remoteDeviceName,
                        transferStatus.error
                    )
                    icon = R.drawable.ic_cross
                }

                // Build the notification
                val notifications: Boolean =
                    settings.transferNotification
                val builder: NotificationCompat.Builder =
                    createBuilder(NOTIFICATION_CHANNEL_ID)
                        .setDefaults(if (notifications) NotificationCompat.DEFAULT_ALL else 0)
                        .setContentIntent(pendingIntent)
                        .setContentTitle(service.getString(R.string.service_transfer_server_title))
                        .setContentText(contentText)
                        .setSmallIcon(icon)

                // For transfers that send files (and fail), it is possible to retry them
                if (transferStatus.state == TransferStatus.State.Failed &&
                    transferStatus.direction == TransferStatus.Direction.Send
                ) {

                    // Ensure the error notification is replaced by the next transfer (I have no idea
                    // why the first line is required but it works :P)
                    intent?.setClass(service, TransferService::class.java)
                    intent?.putExtra(TransferService.EXTRA_ID, transferStatus.id)

                    // Add the action
                    builder.addAction(
                        NotificationCompat.Action.Builder(
                            R.drawable.ic_autorenew_white_24dp,
                            service.getString(R.string.service_transfer_action_retry),
                            intent?.let {
                                PendingIntent.getService(
                                    service, transferStatus.id,
                                    it, PendingIntent.FLAG_ONE_SHOT
                                )
                            }
                        ).build()
                    )
                }

                // Show the notification
                notificationManager.notify(transferStatus.id, builder.build())
            }
            numberOfTransfers--

            // Stop the service if there are no active tasks
            if (stop()) {
                return
            }

            // Update the notification
            updateNotification()
        } else {

            // Prepare the appropriate text for the transfer
            val contentText: CharSequence
            val icon: Int
            if (transferStatus.direction == TransferStatus.Direction.Receive) {
                contentText = service.getString(
                    R.string.service_transfer_status_receiving,
                    transferStatus.remoteDeviceName
                )
                icon = R.drawable.ic_get_app_24px
            } else {
                contentText = service.getString(
                    R.string.service_transfer_status_sending,
                    transferStatus.remoteDeviceName
                )
                icon = R.drawable.ic_publish_24px
            }

            // Intent for stopping this particular service
            val stopIntent: Intent = Intent(service, TransferService::class.java)
                .setAction(TransferService.ACTION_STOP_TRANSFER)
                .putExtra(TransferService.EXTRA_TRANSFER, transferStatus.id)

            // Update the notification
            notificationManager.notify(
                transferStatus.id,
                createBuilder(TRANSFER_CHANNEL_ID)
                    .setContentIntent(pendingIntent)
                    .setContentTitle(service.getString(R.string.service_transfer_title))
                    .setContentText(contentText)
                    .setOngoing(true)
                    .setProgress(100, transferStatus.progress, false)
                    .setSmallIcon(icon)
                    .addAction(
                        NotificationCompat.Action.Builder(
                            R.drawable.ic_cancel_24px,
                            service.getString(R.string.service_transfer_action_stop),
                            PendingIntent.getService(
                                service,
                                transferStatus.id,
                                stopIntent,
                                0
                            )
                        ).build()
                    )
                    .build()
            )
        }
    }

    /**
     * Create a notification for URLs
     * @param url full URL
     */
    fun showUrl(url: String?) {
        val id = nextId()
        val pendingIntent = PendingIntent.getActivity(
            service,
            id,
            Intent(Intent.ACTION_VIEW, Uri.parse(url)),
            0
        )
        notificationManager.notify(
            id,
            createBuilder(NOTIFICATION_CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setContentTitle(service.getString(R.string.service_transfer_notification_url))
                .setContentText(url)
                .setSmallIcon(R.drawable.ic_link_24px)
                .build()
        )
    }

     fun updateNotification() {
        Timber.i(
            String.format("updating notification with %d transfer(s)...", numberOfTransfers)
        )
        if (numberOfTransfers == 0) {
            builder.setContentText(
                service.getString(
                    R.string.service_transfer_server_listening_text
                )
            )
        } else {
            builder.setContentText(
                service.resources.getQuantityString(
                    R.plurals.service_transfer_server_transferring_text,
                    numberOfTransfers, numberOfTransfers
                )
            )
        }
        service.startForeground(
            NOTIFICATION_ID,
            builder.build()
        )
    }

    private fun stop(): Boolean {
        if (!listening && numberOfTransfers == 0) {
            Timber.i(
                "Not listening and no transfers, shutting down..."
            )
            service.stopSelf()
            return true
        }
        return false
    }

    companion object {
        private const val SERVICE_CHANNEL_ID = "service_channel_id"
        private const val TRANSFER_CHANNEL_ID = "transfer_channel_id"
        private const val NOTIFICATION_CHANNEL_ID = "notification_channel_id"
        private const val NOTIFICATION_ID = 1
    }
}