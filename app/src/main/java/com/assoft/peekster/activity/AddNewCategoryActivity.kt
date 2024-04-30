package com.assoft.peekster.activity

import android.os.Bundle
import com.assoft.peekster.R
import com.assoft.peekster.data.mediasource.GetAudioContent
import com.assoft.peekster.data.mediasource.GetVideoContent
import com.assoft.peekster.databinding.ActivityNewCategoryBinding
import com.assoft.peekster.util.contentView
import com.assoft.peekster.database.entities.Category
import com.assoft.peekster.util.ext.onTextChanged
import com.assoft.peekster.util.ext.validate
import com.codekidlabs.storagechooser.Content
import com.codekidlabs.storagechooser.StorageChooser
import org.koin.android.viewmodel.ext.android.viewModel

class AddNewCategoryActivity : Activity() {
    /** Internal variable for obtaining the [ActivityNewCategoryBinding] binding. */
    private val binding: ActivityNewCategoryBinding by contentView(R.layout.activity_new_category)

    /** Reference to the [MediaViewModel] used for media events */
    private val mediaViewModel: MediaViewModel by viewModel()

    /**
     * Called when the activity is first created. This is where
     * you should do all of your normal static set up: create
     * views, bind data to lists, etc.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.getStringExtra("category")

        binding.apply {
            viewModel = mediaViewModel
            lifecycleOwner = this@AddNewCategoryActivity

            toolbar.setNavigationOnClickListener {
                userInteractionNavigation = true
                finish()
            }

            etCategory.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    textInputCategory.validate(
                        { mediaViewModel.newCategory.value.isNullOrEmpty() },
                        "Enter a valid Category"
                    )
                }
            }

            etDirectory.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    textInputCategory.validate(
                        { mediaViewModel.directory.value.isNullOrEmpty() },
                        "Select a Directory"
                    )
                }
            }

            etCategory.onTextChanged {
                textInputCategory.error = null
            }

            etDirectory.onTextChanged {
                textInputDirectory.error = null
            }

            etDirectory.setOnClickListener {
                val c = Content().apply {
                    createLabel = "Create"
                    cancelLabel = "Cancel"
                    selectLabel = "Select"
                }

                // Initialize Builder
                val chooser = StorageChooser.Builder()
                    .withActivity(this@AddNewCategoryActivity)
                    .withFragmentManager(fragmentManager)
                    .withMemoryBar(true)
                    .withContent(c)
                    .setTheme(getScTheme())
                    .allowCustomPath(true)
                    .setType(StorageChooser.DIRECTORY_CHOOSER)
                    .build()

                chooser.show()

                chooser.setOnSelectListener { path ->
                    mediaViewModel.directory.value = path
                }
            }

            floatingActionButton.setOnClickListener {
                if (mediaViewModel.newCategory.value.isNullOrEmpty()) {
                    binding.textInputCategory.error = "Enter a valid Category"
                    return@setOnClickListener
                }
                if (mediaViewModel.directory.value.isNullOrEmpty()) {
                    binding.textInputDirectory.error = "Select a Directory"
                    return@setOnClickListener
                }

                mediaViewModel.apply {
                    insert(
                        Category(
                            name = newCategory.value.toString(),
                            type = data!!,
                            path = if (data != "Music") GetVideoContent.externalContentUri.toString() else GetAudioContent.externalContentUri.toString(),
                            folder = directory.value.toString(),
                            fixed = false
                        )
                    )
                    finish()
                }
            }
        }
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
}