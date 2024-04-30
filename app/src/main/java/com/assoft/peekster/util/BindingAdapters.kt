package com.assoft.peekster.util

import android.view.View
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.assoft.peekster.R
import com.assoft.peekster.adapter.HorizontalMovieAdapter
import com.assoft.peekster.adapter.HorizontalMusicAdapter
import com.assoft.peekster.adapter.HorizontalTvShowAdapter
import com.assoft.peekster.data.mediasource.Audio
import com.assoft.peekster.data.mediasource.MediaSource
import com.assoft.peekster.data.mediasource.TvShow
import com.assoft.peekster.data.mediasource.Video
import com.assoft.peekster.database.entities.Category
import com.assoft.peekster.domain.Movie
import com.assoft.peekster.domain.TvShow as DatabaseShow
import com.assoft.peekster.activity.MediaViewModel

@BindingAdapter("hideOptionUnless")
fun hideOptionUnless(view: View, predicate: Boolean) {
    view.visibility = if (predicate) View.VISIBLE else View.GONE
}

@BindingAdapter("viewModel", "goneUnless", requireAll = false)
fun goneUnless(view: View, viewModel: MediaViewModel, category: Category) {
    when (category.name) {
        in "Movie", "Tv Show", "Music" -> {
            when (category.type) {
                "Movie" -> {
                    val data: List<Video> =
                        MediaSource.withVideoContext(view.context)
                            ?.getAllMovieContent(category.path.toUri()) as List<Video>
                    hideMoviesIfEmpty(view, viewModel, data)
                }
                "Tv Show" -> {
                    val data: List<TvShow> =
                        MediaSource.withTvContext(view.context)
                            ?.getAllTvShowContent(category.path.toUri()) as List<TvShow>
                    hideTvShowIfEmpty(view, viewModel, data)

                }
                "Music" -> {
                    val data: List<Audio> =
                        MediaSource.withAudioContext(view.context)
                            ?.getAllAudioContent(category.path.toUri()) as List<Audio>

                    view.visibility = if (data.isNotEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        else -> {
            when (category.type) {
                "Movie" -> {
                    val data: List<Video> =
                        MediaSource.withVideoContext(view.context)
                            ?.getAllMovieContentByFolder(
                                category.path.toUri(),
                                category.folder
                            ) as List<Video>

                    hideMoviesIfEmpty(view, viewModel, data)
                }
                "Music" -> {
                    val data: List<Audio> =
                        MediaSource.withAudioContext(view.context)
                            ?.getAllAudioContentByFolder(
                                category.path.toUri(),
                                category.folder
                            ) as List<Audio>

                    view.visibility = if (data.isNotEmpty()) View.VISIBLE else View.GONE
                }
                "Tv Show" -> {
                    val data: List<TvShow> =
                        MediaSource.withTvContext(view.context)
                            ?.getAllTvShowContentByFolder(
                                category.path.toUri(),
                                category.folder
                            ) as List<TvShow>
                    hideTvShowIfEmpty(view, viewModel, data)
                }
            }
        }
    }
}

private fun hideMoviesIfEmpty(view: View, viewModel: MediaViewModel, data: List<Video>) {
    val allMovies: MutableList<Movie> = mutableListOf()
    if (viewModel.isChileModeEnabled()) {
        for (index in data.indices) {
            val movie = viewModel.findMovieByName(data[index].name)
            val excludeMovie = movie?.genres?.let {
                it.contains("Action") || it.contains("Horror") || movie.adult.equals(
                    "true"
                )
            } ?: run {
                movie?.adult.equals("true")
            }

            if (!excludeMovie) {
                if (movie != null) {
                    allMovies.add(movie)
                }
            }
        }
        view.visibility = if (allMovies.isNotEmpty()) View.VISIBLE else View.GONE

    } else {
        view.visibility = if (data.isNotEmpty()) View.VISIBLE else View.GONE
    }
}

private fun hideTvShowIfEmpty(view: View, viewModel: MediaViewModel, data: List<TvShow>) {
    val allShows: MutableList<DatabaseShow> = mutableListOf()
    if (viewModel.isChileModeEnabled()) {
        for (index in data.indices) {
            val tv = viewModel.findTvShowByName(data[index].name)
            val excludeTvShow = tv?.genres?.let {
                it.contains("Action") || it.contains("Horror")
            } ?: run {
                false
            }

            if (!excludeTvShow) {
                if (tv != null) {
                    allShows.add(tv)
                }
            }
        }
        view.visibility = if (allShows.isNotEmpty()) View.VISIBLE else View.GONE

    } else {
        view.visibility = if (data.isNotEmpty()) View.VISIBLE else View.GONE
    }
}


@BindingAdapter("viewModel", "category", requireAll = true)
fun mediaContent(
    recyclerView: RecyclerView,
    viewModel: MediaViewModel,
    category: Category
) {
    val itemDecoration =
        RightSpacingItemDecoration(recyclerView.resources.getDimensionPixelSize(R.dimen.grid_0_75))
    if (recyclerView.itemDecorationCount == 0) recyclerView.addItemDecoration(itemDecoration)
    when (category.name) {
        "Movie" -> {

            // Get Video files from device
            val data: List<Video> =
                MediaSource.withVideoContext(recyclerView.context)
                    ?.getAllMovieContent(category.path.toUri()) as List<Video>

            if (data.isNotEmpty()) {
                recyclerView.visibility = View.VISIBLE
                recyclerView.apply {
                    val dataAdapter = (recyclerView.adapter as? HorizontalMovieAdapter
                        ?: HorizontalMovieAdapter(context, viewModel)).apply {
                        movies = data as MutableList<Video>
                    }

                    adapter.apply {
                        if (this?.hasObservers() == false) {
                            this.setHasStableIds(true)
                        }
                        layoutManager =
                            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false).apply {
                                supportsPredictiveItemAnimations()
                            }
                        itemAnimator = DefaultItemAnimator()
                        adapter = dataAdapter
                    }
                }
            } else {
                recyclerView.visibility = View.GONE
            }
        }
        "Music" -> {
            val data: List<Audio> =
                MediaSource.withAudioContext(recyclerView.context)
                    ?.getAllAudioContent(category.path.toUri()) as List<Audio>

            if (data.isNotEmpty()) {
                recyclerView.visibility = View.VISIBLE
                recyclerView.apply {
                    val dataAdapter = (recyclerView.adapter as? HorizontalMusicAdapter
                        ?: HorizontalMusicAdapter(context, viewModel)).apply {
                        music = data
                    }
                    adapter.apply {
                        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                        itemAnimator = DefaultItemAnimator()
                        adapter = dataAdapter
                    }
                }
            } else {
                recyclerView.visibility = View.GONE
            }
        }
        "Tv Show" -> {
            val data: List<TvShow> =
                MediaSource.withTvContext(recyclerView.context)
                    ?.getAllTvShowContent(category.path.toUri()) as List<TvShow>

            if (data.isNotEmpty()) {
                recyclerView.visibility = View.VISIBLE
                recyclerView.apply {
                    val dataAdapter = (recyclerView.adapter as? HorizontalTvShowAdapter
                        ?: HorizontalTvShowAdapter(context, viewModel)).apply {
                        tvShow = data
                    }
                    adapter.apply {
                        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                        itemAnimator = DefaultItemAnimator()
                        adapter = dataAdapter
                    }
                }
            } else {
                recyclerView.visibility = View.GONE
            }
        }
        else -> {
            when (category.type) {
                "Movie" -> {
                    val data: List<Video> =
                        MediaSource.withVideoContext(recyclerView.context)
                            ?.getAllMovieContentByFolder(
                                category.path.toUri(),
                                category.folder
                            ) as List<Video>

                    if (data.isNotEmpty()) {
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.apply {
                            val dataAdapter = (recyclerView.adapter as? HorizontalMovieAdapter
                                ?: HorizontalMovieAdapter(context, viewModel)).apply {
                                movies = data as MutableList<Video>
                            }
                            adapter.apply {
                                if (this?.hasObservers() == false) {
                                    this.setHasStableIds(true)
                                }
                                layoutManager =
                                    LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                                itemAnimator = DefaultItemAnimator()
                                adapter = dataAdapter
                            }
                        }
                    } else {
                        recyclerView.visibility = View.GONE
                    }
                }
                "Music" -> {
                    val data: List<Audio> =
                        MediaSource.withAudioContext(recyclerView.context)
                            ?.getAllAudioContentByFolder(
                                category.path.toUri(),
                                category.folder
                            ) as List<Audio>

                    if (data.isNotEmpty()) {
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.apply {
                            val dataAdapter = (recyclerView.adapter as? HorizontalMusicAdapter
                                ?: HorizontalMusicAdapter(context, viewModel)).apply {
                                music = data
                            }
                            adapter.apply {
                                layoutManager =
                                    LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                                itemAnimator = DefaultItemAnimator()
                                adapter = dataAdapter
                            }
                        }
                    } else {
                        recyclerView.visibility = View.GONE
                    }
                }
                "Tv Show" -> {
                    val data: List<TvShow> =
                        MediaSource.withTvContext(recyclerView.context)
                            ?.getAllTvShowContentByFolder(
                                category.path.toUri(),
                                category.folder
                            ) as List<TvShow>

                    if (data.isNotEmpty()) {
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.apply {
                            val dataAdapter = (recyclerView.adapter as? HorizontalTvShowAdapter
                                ?: HorizontalTvShowAdapter(context, viewModel)).apply {
                                tvShow = data
                            }
                            adapter.apply {
                                layoutManager =
                                    LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                                itemAnimator = DefaultItemAnimator()
                                adapter = dataAdapter
                            }
                        }
                    } else {
                        recyclerView.visibility = View.GONE
                    }
                }
            }
        }
    }
}
