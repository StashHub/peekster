package com.assoft.peekster.activity

import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import com.assoft.peekster.R
import com.assoft.peekster.data.mediasource.Video
import com.assoft.peekster.databinding.ActivityMovieDetailsBinding
import com.assoft.peekster.domain.Movie
import com.assoft.peekster.util.Activities
import com.assoft.peekster.util.contentView
import com.assoft.peekster.util.ext.*
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import org.koin.android.viewmodel.ext.android.viewModel

/**
 * Display movie details
 */
class MovieDetailActivity : Activity() {

    /** Internal variable for obtaining the [ActivityMovieDetailsBinding] binding. */
    private val binding: ActivityMovieDetailsBinding by contentView(R.layout.activity_movie_details)

    /** Reference to the [MediaViewModel] used for media events */
    private val mediaViewModel: MediaViewModel by viewModel()

    private var movieData: Movie? = null

    private var adultContent = false

    /**
     * Called when the activity is first created. This is where
     * you should do all of your normal static set up: create
     * views, bind data to lists, etc.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.getParcelableExtra<Video>("data")

        val option = RequestOptions()
            .skipMemoryCache(false)
            .placeholder(R.drawable.ic_default_img)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .priority(Priority.HIGH)
            .dontAnimate()

        movieData = mediaViewModel.findMovieByName((data as Video).name)
        movieData?.let {
            binding.apply {
                movieTitle.text = it.title
                movieDescription.text = it.overview
                language.text = it.original_language?.language()
                movieRating.text = it.vote_average
                favourite.text = it.popularity
                tvGenre.text = it.genres
                textViewResolution.text = data.resolution?.format()
                movieDuration.text = getString(R.string.video_duration, data.duration.duration())
                it.release_date?.let {
                    textViewReleaseDate.text = it
                } ?: run {
                    textViewReleaseDate.text = getString(R.string.unknown)
                }

                Glide.with(this@MovieDetailActivity)
                    .setDefaultRequestOptions(option)
                    .load(it.poster_path)
                    .into(moviePoster)

                Glide.with(this@MovieDetailActivity)
                    .setDefaultRequestOptions(option)
                    .load(it.backdrop_path)
                    .into(movieBackdrop)

                if (it.poster_path.isNullOrEmpty()) {
                    Glide.with(this@MovieDetailActivity)
                        .setDefaultRequestOptions(option)
                        .load(data.thumbnail.toUri())
                        .into(moviePoster)
                }
                if (it.backdrop_path.isNullOrEmpty()) {
                    Glide.with(this@MovieDetailActivity)
                        .setDefaultRequestOptions(option)
                        .load(data.thumbnail.toUri())
                        .into(movieBackdrop)
                }

                adultContent = it.adult.equals("true")
            }
        } ?: run {
            binding.apply {
                movieTitle.text = data.name
                textViewResolution.text = data.resolution?.format()
                textViewReleaseDate.text = getString(R.string.unknown)
                movieDescription.visibility = View.GONE
                movieDuration.text = getString(R.string.video_duration, data.duration.duration())
                language.text = getString(R.string.unknown)
                tvGenre.text = getString(R.string.unknown)
                Glide.with(this@MovieDetailActivity)
                    .setDefaultRequestOptions(option)
                    .load(data.thumbnail)
                    .into(moviePoster)

                Glide.with(this@MovieDetailActivity)
                    .setDefaultRequestOptions(option)
                    .load(data.thumbnail)
                    .into(movieBackdrop)
            }
        }


        binding.apply {
            backButton.setOnClickListener {
                onBackPressed()
            }

            playButton.setOnClickListener {
                movieData?.let {
                    data.name = it.title.toString()
                }
                mediaViewModel.playVideo(data)
            }
        }

        mediaViewModel.navigateToVideoPlayer.observe(this, Observer { movie ->
            if (mediaViewModel.isChileModeEnabled()) {
                if (excludeMovie()) {
                    toast("Child Mode: Permission Denied!")
                } else {
                    navigateToMediaPlayer(movie)
                }
            } else {
                navigateToMediaPlayer(movie)
            }
        })
    }

    private fun excludeMovie(): Boolean {
        movieData?.genres?.let {
            return adultContent || it.contains("Action") || it.contains("Horror")
        }
        return adultContent
    }

    private fun navigateToMediaPlayer(data: Video?) {
        Activities.MediaPlayer.dynamicStart?.let { intent ->
            startActivity(intent, false, extras = hashMapOf("data" to data))
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
}