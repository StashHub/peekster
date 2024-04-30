package com.assoft.peekster.activity.shareable.dialog

import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.assoft.peekster.R
import com.assoft.peekster.activity.Activity
import com.assoft.peekster.activity.MainViewModel
import com.assoft.peekster.util.AppUtils

class ProfileEditorDialog(activity: Activity, viewModel: MainViewModel) :
    AlertDialog.Builder(activity) {
    private var mDialog: AlertDialog? = null

    private fun closeIfPossible() {
        if (mDialog != null) {
            if (mDialog!!.isShowing) mDialog!!.dismiss() else mDialog = null
        }
    }

    override fun show(): AlertDialog {
        return super.show().also { mDialog = it }
    }

    init {
        val view: View =
            LayoutInflater.from(activity).inflate(R.layout.layout_profile_editor, null, false)
        val image =
            view.findViewById<ImageView>(R.id.layout_profile_picture_image_default)
        val editImage =
            view.findViewById<ImageView>(R.id.layout_profile_picture_image_preferred)
        val editText = view.findViewById<EditText>(R.id.et_device_name)
        val deviceName: String = AppUtils.getLocalDeviceName(viewModel)
        editText.text.clear()
        editText.text.append(deviceName)
        activity.loadProfilePictureInto(deviceName, image)
        editText.requestFocus()
        editImage.setOnClickListener {
            activity.requestProfilePictureChange()
            closeIfPossible()
        }
        setView(view)
        setNegativeButton(R.string.action_remove) { _, _ ->
            activity.deleteFile("profilePicture")
            activity.notifyUserProfileChanged()
        }
        setPositiveButton(R.string.action_save) { _, _ ->
            viewModel.saveDeviceName(editText.text.toString())
            activity.notifyUserProfileChanged()
        }
        setNeutralButton(R.string.action_close, null)
    }
}