package com.assoft.peekster.repository

import android.os.AsyncTask
import com.assoft.peekster.database.dao.TvShowDao
import com.assoft.peekster.database.entities.DatabaseTvShow
import com.assoft.peekster.database.entities.asDatabaseModel
import com.assoft.peekster.database.entities.asDomainModel
import com.assoft.peekster.domain.TvShow

class TvShowRepository(
    private val tvShowDao: TvShowDao
) {

    fun findTvShowByName(name: String): AsyncTask<String, Unit, TvShow>? {
        return FindTvShowByNameMovieAsyncTask(
            tvShowDao
        ).execute(name)
    }

    private class FindTvShowByNameMovieAsyncTask(val tvShowDao: TvShowDao) :
        AsyncTask<String, Unit, TvShow>() {

        override fun doInBackground(vararg name: String): TvShow? {
            return tvShowDao.findByName(name = name[0])?.asDomainModel()
        }
    }

    fun insert(tv: TvShow): AsyncTask<DatabaseTvShow, Unit, Long>? {
        return InsertTvShowAsyncTask(
            tvShowDao
        ).execute(tv.asDatabaseModel())
    }

    private class InsertTvShowAsyncTask(val tvShowDao: TvShowDao) :
        AsyncTask<DatabaseTvShow, Unit, Long>() {

        override fun doInBackground(vararg tvShow: DatabaseTvShow): Long? {
            return tvShowDao.insert(tvShow = tvShow[0])
        }
    }
}