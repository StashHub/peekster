package com.assoft.peekster.activity

import android.os.Bundle
import androidx.lifecycle.Observer
import com.assoft.peekster.R
import com.assoft.peekster.data.Data.gridLayout
import com.assoft.peekster.databinding.ActivityCategoryBinding
import com.assoft.peekster.util.Activities
import com.assoft.peekster.util.contentView
import com.assoft.peekster.util.ext.startActivity
import com.assoft.peekster.database.entities.Category
import org.koin.android.viewmodel.ext.android.viewModel

class CategoryActivity : Activity() {

    /** Internal variable for obtaining the [ActivityCategoryBinding] binding. */
    private val binding: ActivityCategoryBinding by contentView(R.layout.activity_category)

    /** Reference to the [MediaViewModel] used for media events */
    private val mediaViewModel: MediaViewModel by viewModel()

    /**
     * Called when the activity is first created. This is where
     * you should do all of your normal static set up: create
     * views, bind data to lists, etc.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gridLayout = true
        val category = intent.getParcelableExtra<Category>("category")

        binding.apply {
            this.viewModel = mediaViewModel
            this.category = category
            this.listener = mediaViewModel
            executePendingBindings()
        }

        mediaViewModel.navigateToMovieDetail.observe(this, Observer { movie ->
            // Navigate to Movie Detail
            Activities.MovieDetail.dynamicStart?.let { intent ->
                startActivity(intent, false, extras = hashMapOf("data" to movie))
            }
        })

        mediaViewModel.navigateToTvShowDetail.observe(this, Observer { tv ->
            // Navigate to TvShow Detail
            Activities.TvShowDetail.dynamicStart?.let { intent ->
                startActivity(intent, false, extras = hashMapOf("data" to tv))
            }
        })

        mediaViewModel.navigateToAudioPlayer.observe(this, Observer { audio ->
            Activities.AudioPlayer.dynamicStart?.let { intent ->
                startActivity(intent, false, extras = hashMapOf("data" to audio))
            }
        })
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
}