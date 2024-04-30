package com.assoft.peekster.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.assoft.peekster.activity.shareable.dialog.ProfileEditorDialog
import com.assoft.peekster.activity.shareable.dialog.RationalePermissionRequest
import com.assoft.peekster.activity.shareable.transfer.TransferService
import com.assoft.peekster.activity.shareable.util.Settings
import com.assoft.peekster.di.injectFeature
import com.assoft.peekster.util.Activities
import com.assoft.peekster.util.AppUtils
import com.assoft.peekster.util.ext.startActivity
import com.assoft.peekster.util.ext.toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.*
import java.net.ServerSocket

abstract class Activity : AppCompatActivity() {

    /** Reference to the [MainViewModel] used for main events */
    val mainViewModel: MainViewModel by viewModel()

    /**
     * Internal [AlertDialog] used to display the ongoing request
     */
    private var ongoingRequest: AlertDialog? = null

    /**
     * Internal variable used to determine whether user decided to skip permission
     */
    var skipPermissionRequest = false

    /**
     * Internal variable used to determine whether welcome page is not allowed
     */
    var welcomePageDisallowed = false

    private var exitAppRequested = false

    lateinit var settings: Settings

    var userInteractionNavigation = false

    private lateinit var serverThread: Thread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectFeature()
        settings = Settings(this)
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume")
        if (!mainViewModel.introductionShown() && !welcomePageDisallowed) {
            Activities.WelcomeActivity.dynamicStart?.let { intent ->
                startActivity(intent, true)
            }
        } else if (!AppUtils.checkRunningConditions(this)) {
            if (!skipPermissionRequest) requestRequiredPermissions(true)
        } else {
            AppUtils.startForegroundService(
                this, Intent(this, TransferService::class.java)
                    .setAction(TransferService.ACTION_SERVICE_STATUS)
                    .putExtra(TransferService.EXTRA_STATUS_STARTED, settings.behaviorReceive)
            )

            // Listen for incoming streams
            listenForStreams()
        }

        exitAppRequested = false
    }

    override fun onPause() {
        super.onPause()
        if (!exitAppRequested && !userInteractionNavigation) {
            Timber.i("Request to stop service")
            AppUtils.startForegroundService(
                this, Intent(this, TransferService::class.java)
                    .setAction(TransferService.ACTION_SERVICE_STATUS)
                    .putExtra(TransferService.EXTRA_STATUS_STARTED, false)
            )
        }
    }

    private fun listenForStreams() {
        serverThread = Thread(Runnable {
            try {
                val serverSocket = ServerSocket(40820)
                while (true) {
                    Timber.i("Ready, Waiting for requests...\n")
                    val socketChannel = serverSocket.accept()

                    val theInputStream: InputStream
                    theInputStream = try {
                        socketChannel.getInputStream()
                    } catch (e1: IOException) {
                        Timber.i(
                            "Error getting the InputString from connection socket, %s",
                            e1.message
                        )
                        e1.printStackTrace()
                        return@Runnable
                    }

                    val inputStreamReader = BufferedReader(
                        InputStreamReader(
                            theInputStream
                        )
                    )

                    val bufferedReader = BufferedReader(inputStreamReader)
                    val serverUrl = bufferedReader.readLine()
                    runOnUiThread {
                        val data = serverUrl.split(';')
                        if (data[0] == settings.deviceName) { // Just a precaution to make sure the right device responds to the message
                            Activities.StreamMediaPlayer.dynamicStart?.let { intent ->
                                startActivity(
                                    intent,
                                    false,
                                    extras = hashMapOf("data" to data[1])
                                )
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Timber.e("Exception %s", e.message)
                e.printStackTrace()
            } catch (e: NullPointerException) {
                Timber.e("Exception %s", e.message)
                e.printStackTrace()
            }
        })

        if (!serverThread.isAlive) serverThread.start()
    }

    /**
     * Exits app closing all the active services and connections.
     * This will also prevent this activity from notifying [TransferService]
     * as the user leaves to the state of [Activity.onPause]
     */
    open fun exitApp() {
        exitAppRequested = true
        stopService(Intent(this, TransferService::class.java))
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (AppUtils.checkRunningConditions(this)) {
            // AppUtils.startForegroundService(this, Intent(this, TransferService::class.java))
        } else requestRequiredPermissions(!skipPermissionRequest)
    }

    open fun requestRequiredPermissions(killActivityOtherwise: Boolean): Boolean {
        ongoingRequest?.let { dialog ->
            if (dialog.isShowing) return false
        }
        for (request in AppUtils.getRequiredPermissions(this))
            if (RationalePermissionRequest.requestIfNecessary(
                    this,
                    request,
                    killActivityOtherwise
                ).also { ongoingRequest = it } != null
            ) return false
        return true
    }

    open fun loadProfilePictureInto(
        deviceName: String?,
        imageView: ImageView
    ) {
        try {
            val inputStream = openFileInput("profilePicture")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            Glide.with(this)
                .load(bitmap)
                .circleCrop()
                .into(imageView)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            imageView.setImageDrawable(deviceName?.let {
                AppUtils.getDefaultIconBuilder(this)?.buildRound(
                    it
                )
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_PROFILE_PHOTO && data != null) {
            val chosenImageUri = data.data
            if (chosenImageUri != null) {
                Glide.with(this)
                    .load(data.data)
                    .centerCrop()
                    .override(200, 200)
                    .into(object : Target<Drawable> {
                        override fun onLoadStarted(placeholder: Drawable?) {}
                        override fun onLoadFailed(errorDrawable: Drawable?) {}
                        override fun getSize(cb: SizeReadyCallback) {}
                        override fun getRequest(): Request? = null
                        override fun onStop() {}
                        override fun setRequest(request: Request?) {}
                        override fun removeCallback(cb: SizeReadyCallback) {}
                        override fun onLoadCleared(placeholder: Drawable?) {}
                        override fun onStart() {}
                        override fun onDestroy() {}

                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            try {
                                val bitmap = Bitmap.createBitmap(
                                    100,
                                    100,
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas =
                                    Canvas(bitmap)
                                val outputStream = openFileOutput(
                                    "profilePicture",
                                    Context.MODE_PRIVATE
                                )
                                resource.setBounds(0, 0, canvas.width, canvas.height)
                                resource.draw(canvas)
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                outputStream.close()
                                notifyUserProfileChanged()
                            } catch (error: Exception) {
                                error.printStackTrace()
                            }
                        }

                    })
            }
        }
    }

    open fun onUserProfileUpdated() {}

    open fun startProfileEditor() {
        ProfileEditorDialog(this, mainViewModel).show()
    }

    open fun notifyUserProfileChanged() {
        if (!isFinishing) runOnUiThread { onUserProfileUpdated() }
    }

    open fun requestProfilePictureChange() {
        startActivityForResult(
            Intent(Intent.ACTION_PICK).setType("image/*"),
            REQUEST_PICK_PROFILE_PHOTO
        )
    }

    companion object {
        const val REQUEST_PICK_PROFILE_PHOTO = 1000
    }
}