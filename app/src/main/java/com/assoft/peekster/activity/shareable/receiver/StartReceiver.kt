package com.assoft.peekster.activity.shareable.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.assoft.peekster.activity.shareable.transfer.TransferService
import com.assoft.peekster.activity.shareable.util.Settings
import com.assoft.peekster.util.AppUtils

/**
 * Starts the transfer service
 */
class StartReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        if (Settings(context).behaviorReceive) {
            AppUtils.startForegroundService(
                context, Intent(context, TransferService::class.java)
                    .setAction(TransferService.ACTION_SERVICE_STATUS)
                    .putExtra(TransferService.EXTRA_STATUS_STARTED, true)
            )
        }
    }
}