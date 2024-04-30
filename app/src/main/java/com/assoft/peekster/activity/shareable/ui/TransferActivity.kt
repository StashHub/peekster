package com.assoft.peekster.activity.shareable.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.assoft.peekster.R
import com.assoft.peekster.activity.Activity
import com.assoft.peekster.activity.shareable.ui.ShareActivity.Companion.ACTION_SEND_SINGLE
import com.assoft.peekster.databinding.ActivityTransferBinding
import com.assoft.peekster.util.contentView
import com.assoft.peekster.util.ext.inTransaction
import com.codekidlabs.storagechooser.Content
import com.codekidlabs.storagechooser.StorageChooser
import timber.log.Timber
import java.io.File

class TransferActivity : Activity() {

    /** Internal variable for obtaining the [ActivityTransferBinding] binding. */
    private val binding: ActivityTransferBinding by contentView(R.layout.activity_transfer)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.apply {
            toolbar.setNavigationOnClickListener {
                userInteractionNavigation = true
                finish()
            }
            fab.setOnClickListener {
                val c = Content().apply {
                    createLabel = "Create"
                    cancelLabel = "Cancel"
                    selectLabel = "Select"
                }

                // Initialize Builder
                val chooser = StorageChooser.Builder()
                    .withActivity(this@TransferActivity)
                    .withFragmentManager(fragmentManager)
                    .withMemoryBar(true)
                    .withContent(c)
                    .setTheme(getScTheme())
                    .allowCustomPath(true)
                    .setType(StorageChooser.FILE_PICKER)
                    .build()

                chooser.show()

                chooser.setOnMultipleSelectListener {
                    val uris = ArrayList<Uri>()
                    it.forEach { file ->
                        uris.add(Uri.fromFile(File(file)))
                    }
                    sendMultipleFiles(uris)
                }

                chooser.setOnSelectListener {
                    val file = Uri.fromFile(File(it))
                    sendSingleFile(file)
                }
            }

            // Add the transfer fragment
            supportFragmentManager.inTransaction {
                add(
                    listContainer.id,
                    TransferFragment()
                )
            }
        }
    }

    private fun sendMultipleFiles(files: ArrayList<Uri>) {
        val shareIntent = Intent(this@TransferActivity, ShareActivity::class.java)
            .setAction("android.intent.action.SEND_MULTIPLE")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)

        startActivityForResult(
            shareIntent,
            SHARE_REQUEST
        )
    }

    private fun sendSingleFile(file: Uri) {
        val shareIntent = Intent(this@TransferActivity, ShareActivity::class.java)
            .setAction(ACTION_SEND_SINGLE)
            .putExtra(Intent.EXTRA_STREAM, file)
        startActivityForResult(
            shareIntent,
            SHARE_REQUEST
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        userInteractionNavigation = true
    }

    override fun onPause() {
        super.onPause()
        if (userInteractionNavigation)
            return else userInteractionNavigation = false
    }

    private fun getScTheme(isChecked: Boolean = true): StorageChooser.Theme? {
        val theme = StorageChooser.Theme(applicationContext)
        theme.scheme =
            if (isChecked) resources.getIntArray(R.array.peekster_theme) else theme.defaultScheme
        return theme
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SHARE_REQUEST) {
            if (resultCode == RESULT_OK) {
                Timber.i("Share request returned")
                // no op
            }
        }
    }

    companion object {
        private const val SHARE_REQUEST = 1
    }

}