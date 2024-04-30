package com.assoft.peekster.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.assoft.peekster.R
import com.assoft.peekster.adapter.EpisodesAdapter
import com.assoft.peekster.data.mediasource.GetTvShowContent
import com.assoft.peekster.data.mediasource.MediaSource
import com.assoft.peekster.data.mediasource.TvShow
import com.assoft.peekster.databinding.ActivityTvDetailsBinding
import com.assoft.peekster.domain.Season
import com.assoft.peekster.util.Activities
import com.assoft.peekster.util.contentView
import com.assoft.peekster.util.ext.*
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*
import com.assoft.peekster.domain.TvShow as TvShowData

/**
 * Display Tv Show details
 */
class TvShowDetailActivity : Activity() {

    /** Internal variable for obtaining the [ActivityTvDetailsBinding] binding. */
    private val binding: ActivityTvDetailsBinding by contentView(R.layout.activity_tv_details)

    /** Reference to the [MediaViewModel] used for media events */
    private val mediaViewModel: MediaViewModel by viewModel()

    private var tvData: TvShowData? = null

    private var seasons = mutableListOf<Season>()

    private var foundSeasons = mutableListOf<String>()

    private lateinit var currentData: TvShow

    /**
     * Called when the activity is first created. This is where
     * you should do all of your normal static set up: create
     * views, bind data to lists, etc.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentData = intent.getParcelableExtra("data") as TvShow

        val option = RequestOptions()
            .skipMemoryCache(false)
            .placeholder(R.drawable.ic_default_img)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .priority(Priority.HIGH)
            .dontAnimate()

        tvData = mediaViewModel.findTvShowByName(currentData.name)
        tvData?.let {
            binding.apply {
                tvTitle.text = it.title
                tvDescription.text = it.overview
                language.text = it.original_language?.language()
                tvRating.text = it.vote_average
                favourite.text = it.popularity
                tvGenre.text = it.genres
                textViewCountry.text = it.country
                textViewResolution.text = currentData.resolution?.format()
                tvDuration.text =
                    getString(R.string.video_duration, currentData.duration.duration())

                Glide.with(this@TvShowDetailActivity)
                    .setDefaultRequestOptions(option)
                    .load(it.poster_path)
                    .into(moviePoster)

                Glide.with(this@TvShowDetailActivity)
                    .setDefaultRequestOptions(option)
                    .load(it.backdrop_path)
                    .into(tvBackdrop)

                if (it.poster_path.isNullOrEmpty()) {
                    Glide.with(this@TvShowDetailActivity)
                        .setDefaultRequestOptions(option)
                        .load(currentData.thumbnail.toUri())
                        .into(moviePoster)
                }
                if (it.backdrop_path.isNullOrEmpty()) {
                    Glide.with(this@TvShowDetailActivity)
                        .setDefaultRequestOptions(option)
                        .load(currentData.thumbnail.toUri())
                        .into(tvBackdrop)
                }
                loadSeason()
            }
        } ?: run {
            binding.apply {
                tvTitle.text = currentData.name
                tvDescription.visibility = View.GONE
                language.text = getString(R.string.unknown)
                textViewResolution.text = currentData.resolution?.format()
                textViewCountry.text = getString(R.string.unknown)
                tvDuration.text =
                    getString(R.string.video_duration, currentData.duration.duration())
                tvGenre.text = getString(R.string.unknown)
                Glide.with(this@TvShowDetailActivity)
                    .setDefaultRequestOptions(option)
                    .load(currentData.thumbnail)
                    .into(moviePoster)

                Glide.with(this@TvShowDetailActivity)
                    .setDefaultRequestOptions(option)
                    .load(currentData.thumbnail)
                    .into(tvBackdrop)

                loadSeason()
            }
        }

        binding.apply {
            backButton.setOnClickListener {
                onBackPressed()
            }

            playButton.setOnClickListener {
                val fullName = currentData.path?.substringAfterLast("/")
                val filName = fullName?.substringBeforeLast(".")
                if (filName != null) {
                    currentData.name = filName.replaced().removeYear()
                }
                mediaViewModel.playTvShow(currentData, 0)
            }
        }

        mediaViewModel.navigateToTvShowPlayer.observe(this, Observer {
            if (mediaViewModel.isChileModeEnabled()) {
                if (excludeTvShow()) {
                    toast("Child Mode: Permission Denied!")
                } else {
                    navigateToMediaPlayer(it.keys.first(), it.values.first())
                }
            } else {
                navigateToMediaPlayer(it.keys.first(), it.values.first())
            }
        })
    }

    private fun excludeTvShow(): Boolean {
        tvData?.genres?.let {
            return it.contains("Action") || it.contains("Horror")
        }
        return true
    }

    private fun navigateToMediaPlayer(data: TvShow?, position: Int = 0) {
        Activities.TvPlayer.dynamicStart?.let { intent ->
            intent.addExtra("position", position)
            startActivity(intent, false, extras = hashMapOf("data" to data))
        }
    }

    private fun loadSeason() {
        for (i in 1..MAX_SEASON) {
            val number = String.format("%02d", i)
            val episodes = populateSeason("s$number")
            if (episodes.size > 0) {
                val season = Season("Season $number", episodes)
                foundSeasons.add("Season $number")
                seasons.add(season)
            }
        }

        ArrayAdapter<String?>(
            this, R.layout.simple_spinner_item,
            foundSeasons as List<String?>
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.seasonSpinner.adapter = adapter
        }

        binding.apply {
            seasonSpinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>?,
                    selectedItemView: View,
                    position: Int,
                    id: Long
                ) {
                    recyclerviewEpisodes.apply {
                        val dataAdapter = (this.adapter as? EpisodesAdapter
                            ?: EpisodesAdapter(mediaViewModel)).apply {
                            tvShow = seasons[position].episodes
                        }
                        adapter.apply {
                            layoutManager =
                                LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                            itemAnimator = DefaultItemAnimator()
                            adapter = dataAdapter
                        }
                    }
                }

                override fun onNothingSelected(parentView: AdapterView<*>?) {}
            }
        }
    }

    private fun populateSeason(name: String): MutableList<TvShow> {
        val seasonAllEpisodes = mutableListOf<TvShow>()
        val data: List<TvShow> =
            MediaSource.withTvContext(this)
                ?.getAllTvShowEpisodes(
                    GetTvShowContent.externalContentUri,
                    tvData?.file_name.toString()
                ) as List<TvShow>
        for (i in data.indices) {
            var fileName: String = data[i].name.toLowerCase(Locale.getDefault())
            fileName = fileName.replaced() // replaces [-._] with spaces
            if (fileName.contains(name)) {
                seasonAllEpisodes.add(data[i])
            }
        }
        return seasonAllEpisodes
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

    companion object {
        const val MAX_SEASON = 53
    }
}