/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.assoft.peekster.activity

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.assoft.peekster.data.mediasource.Audio
import com.assoft.peekster.data.mediasource.TvShow
import com.assoft.peekster.data.mediasource.Video
import com.assoft.peekster.repository.CategoryRepository
import com.assoft.peekster.database.entities.Category
import com.assoft.peekster.domain.Movie
import com.assoft.peekster.domain.TvShow as DomainShow
import com.assoft.peekster.domain.usecase.ParentControlUseCase
import com.assoft.peekster.domain.usecase.ParentalControlActionUseCase
import com.assoft.peekster.repository.MoviesRepository
import com.assoft.peekster.repository.TvShowRepository

class MediaViewModel(
    private val categoryRepository: CategoryRepository,
    private val moviesRepository: MoviesRepository,
    private val tvShowRepository: TvShowRepository,
    private val parentalControlActionUseCase: ParentalControlActionUseCase,
    val parentalControlUseCase: ParentControlUseCase
) : ViewModel(), MainEventListener {

    private val _parentControlData = MutableLiveData<Boolean>()
    val parentControlData: LiveData<Boolean> get() = _parentControlData

    private val _showPopupMenu = MutableLiveData<HashMap<View, Category>>()
    val showPopupMenu: LiveData<HashMap<View, Category>> get() = _showPopupMenu

    private val _navigateToMovieDetail = MutableLiveData<Video>()
    val navigateToMovieDetail: LiveData<Video> get() = _navigateToMovieDetail

    private val _navigateToTvShowDetail = MutableLiveData<TvShow>()
    val navigateToTvShowDetail: LiveData<TvShow> get() = _navigateToTvShowDetail

    private val _navigateToVideoPlayer = MutableLiveData<Video>()
    val navigateToVideoPlayer: LiveData<Video> get() = _navigateToVideoPlayer

    private val _navigateToTvShowPlayer = MutableLiveData<HashMap<TvShow, Int>>()
    val navigateToTvShowPlayer: LiveData<HashMap<TvShow, Int>> get() = _navigateToTvShowPlayer

    private val _navigateToAudioPlayer = MutableLiveData<Audio>()
    val navigateToAudioPlayer: LiveData<Audio> get() = _navigateToAudioPlayer

    private val _navigateToShowAllCategory = MutableLiveData<Category>()
    val navigateToShowAllCategory: LiveData<Category> get() = _navigateToShowAllCategory

    private var allCategories: LiveData<List<Category>> = categoryRepository.getAllCategories()

    /** [String] [MutableLiveData] variable used to keep track of the new category to add */
    val newCategory = MutableLiveData<String>()

    /** [String] [MutableLiveData] variable used to keep track of the selected directory */
    val directory = MutableLiveData<String>()

    init {
        val parentalControlResult = parentalControlUseCase()
        _parentControlData.postValue(parentalControlResult)
    }

    fun setParentalControl() {
        val locked = !parentalControlUseCase()
        parentalControlActionUseCase(active = locked)
        _parentControlData.postValue(locked)
    }

    fun isChileModeEnabled(): Boolean {
        return parentalControlUseCase()
    }

    fun insert(category: Category) {
        categoryRepository.insert(category)
    }

    override fun insert(
        name: String,
        title: String,
        popularity: String,
        voteCount: String,
        poster: String,
        adult: String,
        cover: String,
        originalLanguage: String,
        genres: String?,
        voteAverage: String,
        overview: String,
        releaseDate: String,
        filePath: String?
    ): Long? {
        return moviesRepository.insert(
            Movie(
                file_name = name,
                title = title,
                popularity = popularity,
                vote_count = voteCount,
                poster_path = poster,
                adult = adult,
                backdrop_path = cover,
                original_language = originalLanguage,
                genres = genres,
                vote_average = voteAverage,
                overview = overview,
                release_date = releaseDate,
                file_path = filePath
            )
        )?.get()
    }

    override fun insertTvShow(
        name: String,
        title: String,
        popularity: String,
        voteCount: String,
        poster: String,
        country: String,
        cover: String,
        originalLanguage: String,
        genres: String?,
        voteAverage: String,
        overview: String,
        releaseDate: String,
        filePath: String?
    ): Long? {
        return tvShowRepository.insert(
            DomainShow(
                file_name = name,
                title = title,
                popularity = popularity,
                vote_count = voteCount,
                poster_path = poster,
                country = country,
                backdrop_path = cover,
                original_language = originalLanguage,
                genres = genres,
                vote_average = voteAverage,
                overview = overview,
                release_date = releaseDate,
                filepath = filePath
            )
        )?.get()
    }

    fun updateCategory(category: Category) {
        categoryRepository.update(category)
    }

    fun deleteCategory(category: Category) {
        categoryRepository.delete(category)
    }

    fun deleteAllCategories() {
        categoryRepository.deleteAllCategories()
    }

    fun getAllCategories(): LiveData<List<Category>> {
        return allCategories
    }

    fun findMovieByName(name: String): Movie? {
        return moviesRepository.findMovieByName(name)?.get()
    }

    fun findTvShowByName(name: String): DomainShow? {
        return tvShowRepository.findTvShowByName(name)?.get()
    }

    override fun showMovieDetail(movie: Video) {
        _navigateToMovieDetail.postValue(movie)
    }

    override fun showTvShowDetail(tvShow: TvShow) {
        _navigateToTvShowDetail.postValue(tvShow)
    }

    override fun playVideo(movie: Video) {
        _navigateToVideoPlayer.postValue(movie)
    }

    override fun playTvShow(tvShow: TvShow, position: Int) {
        _navigateToTvShowPlayer.value = hashMapOf(tvShow to position)
    }

    override fun playAudio(audio: Audio) {
        _navigateToAudioPlayer.postValue(audio)
    }

    override fun showAll(category: Category) {
        _navigateToShowAllCategory.postValue(category)
    }

    override fun filter() {
//        context.toast("Filter clicked")
    }

    override fun openPopupMenu(view: View, category: Category) {
        _showPopupMenu.value = hashMapOf(view to category)
    }
}

interface MainEventListener {
    fun showAll(category: Category)
    fun showMovieDetail(movie: Video)
    fun showTvShowDetail(tvShow: TvShow)
    fun playTvShow(tvShow: TvShow, position: Int)
    fun playVideo(movie: Video)
    fun playAudio(audio: Audio)
    fun filter()
    fun openPopupMenu(view: View, category: Category)
    fun insert(
        name: String,
        title: String,
        popularity: String,
        voteCount: String,
        poster: String,
        adult: String,
        cover: String,
        originalLanguage: String,
        genres: String?,
        voteAverage: String,
        overview: String,
        releaseDate: String,
        filePath: String?
    ): Long?

    fun insertTvShow(
        name: String,
        title: String,
        popularity: String,
        voteCount: String,
        poster: String,
        country: String,
        cover: String,
        originalLanguage: String,
        genres: String?,
        voteAverage: String,
        overview: String,
        releaseDate: String,
        filePath: String?
    ): Long?
}