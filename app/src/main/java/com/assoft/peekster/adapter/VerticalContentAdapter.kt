package com.assoft.peekster.adapter

import android.view.View
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.assoft.peekster.data.mediasource.Audio
import com.assoft.peekster.data.mediasource.MediaSource
import com.assoft.peekster.data.mediasource.TvShow
import com.assoft.peekster.data.mediasource.Video
import com.assoft.peekster.database.entities.Category
import com.assoft.peekster.activity.MediaViewModel

@BindingAdapter("model", "cat", requireAll = true)
fun content(
    recyclerView: RecyclerView,
    viewModel: MediaViewModel,
    category: Category
) {
    when (category.name) {
        "Movie" -> {
            val data: List<Video> =
                MediaSource.withVideoContext(recyclerView.context)
                    ?.getAllMovieContent(category.path.toUri()) as List<Video>

            val allMovies = filterParentalMovies(viewModel, data)

            if (allMovies.isNotEmpty()) {
                recyclerView.visibility = View.VISIBLE
                recyclerView.apply {
                    val dataAdapter = (recyclerView.adapter as? HorizontalMovieAdapter
                        ?: HorizontalMovieAdapter(context, viewModel)).apply {
                        movies = allMovies
                    }
                    adapter.apply {
                        if (this?.hasObservers() == false) {
                            this.setHasStableIds(true)
                        }
                        layoutManager = GridLayoutManager(recyclerView.context, 3)
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
                        layoutManager = GridLayoutManager(recyclerView.context, 3)
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

            val allTvShows = filterParentalTvShows(viewModel, data)

            if (allTvShows.isNotEmpty()) {
                recyclerView.visibility = View.VISIBLE
                recyclerView.apply {
                    val dataAdapter = (recyclerView.adapter as? HorizontalTvShowAdapter
                        ?: HorizontalTvShowAdapter(context, viewModel)).apply {
                        tvShow = allTvShows
                    }
                    adapter.apply {
                        layoutManager = GridLayoutManager(recyclerView.context, 3)
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

                    val allMovies = filterParentalMovies(viewModel, data)

                    if (allMovies.isNotEmpty()) {
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.apply {
                            val dataAdapter = (recyclerView.adapter as? HorizontalMovieAdapter
                                ?: HorizontalMovieAdapter(context, viewModel)).apply {
                                movies = allMovies
                            }
                            adapter.apply {
                                if (this?.hasObservers() == false) {
                                    this.setHasStableIds(true)
                                }
                                layoutManager = GridLayoutManager(recyclerView.context, 3)
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
                                layoutManager = GridLayoutManager(recyclerView.context, 3)
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

                    val allTvShows = filterParentalTvShows(viewModel, data)

                    if (allTvShows.isNotEmpty()) {
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.apply {
                            val dataAdapter = (recyclerView.adapter as? HorizontalTvShowAdapter
                                ?: HorizontalTvShowAdapter(context, viewModel)).apply {
                                tvShow = allTvShows
                            }
                            adapter.apply {
                                layoutManager = GridLayoutManager(recyclerView.context, 3)
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

private fun filterParentalMovies(
    viewModel: MediaViewModel,
    data: List<Video>
): MutableList<Video> {
    val allMovies = mutableListOf<Video>()
    if (viewModel.isChileModeEnabled()) {
        for (index in data.indices) {
            val movie = viewModel.findMovieByName(data[index].name)
            val excludeMovie = movie?.genres?.let {
                it.contains("Action") || it.contains("Horror") || movie.adult.equals("true")
            } ?: run {
                movie?.adult.equals("true")
            }
            if (!excludeMovie) {
                if (movie != null) {
                    allMovies.add(data[index])
                }
            }
        }
    } else {
        allMovies.addAll(data)
    }
    return allMovies
}

private fun filterParentalTvShows(
    viewModel: MediaViewModel,
    data: List<TvShow>
): MutableList<TvShow> {
    val allTvShows = mutableListOf<TvShow>()
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
                    allTvShows.add(data[index])
                }
            }
        }
    } else {
        allTvShows.addAll(data)
    }
    return allTvShows
}