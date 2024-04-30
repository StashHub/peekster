package com.assoft.peekster.activity.shareable.dialog

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.assoft.peekster.R
import com.assoft.peekster.activity.Activity
import com.assoft.peekster.activity.MainActivity.Companion.REQUEST_PERMISSION_ALL

class RationalePermissionRequest(
    activity: Activity,
    var mPermissionQueue: PermissionRequest,
    killActivityOtherwise: Boolean
) : AlertDialog.Builder(activity) {

    class PermissionRequest @JvmOverloads constructor(
        permission: String,
        title: String,
        message: String,
        required: Boolean = true
    ) {
        var permissionRequest: String = permission
        var requestTitle: String = title
        var requestMessage: String = message
        var requestRequired = required

        constructor(
            context: Context,
            permission: String,
            titleRes: Int,
            messageRes: Int
        ) : this(permission, context.getString(titleRes), context.getString(messageRes))

    }

    companion object {
        fun requestIfNecessary(
            activity: Activity,
            permissionQueue: PermissionRequest,
            killActivityOtherwise: Boolean
        ): AlertDialog? {
            return if (ActivityCompat.checkSelfPermission(
                    activity,
                    permissionQueue.permissionRequest
                ) == PackageManager.PERMISSION_GRANTED
            ) null else RationalePermissionRequest(
                activity,
                permissionQueue,
                killActivityOtherwise
            ).show()
        }
    }

    init {
        setCancelable(false)
        setTitle(mPermissionQueue.requestTitle)
        setMessage(mPermissionQueue.requestMessage)
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                mPermissionQueue.permissionRequest
            )
        ) setNeutralButton(R.string.action_settings) { _, _ ->
            val intent = Intent()
                .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", activity.packageName, null))
            activity.startActivity(intent)
        }
        setPositiveButton(R.string.action_ask, DialogInterface.OnClickListener { _, _ ->
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(mPermissionQueue.permissionRequest),
                REQUEST_PERMISSION_ALL
            )
        })
        if (killActivityOtherwise) setNegativeButton(R.string.action_reject) { _, _ ->
            activity.finish()
        } else setNegativeButton(
            R.string.action_close,
            null
        )
    }
}