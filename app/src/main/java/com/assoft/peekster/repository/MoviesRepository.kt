package com.assoft.peekster.repository

import android.os.AsyncTask
import com.assoft.peekster.database.dao.MovieDao
import com.assoft.peekster.database.entities.DatabaseMovie
import com.assoft.peekster.database.entities.asDatabaseModel
import com.assoft.peekster.database.entities.asDomainModel
import com.assoft.peekster.domain.Movie

class MoviesRepository(
    private val movieDao: MovieDao
) {

    fun findMovieByName(name: String) : AsyncTask<String, Unit, Movie>? {
        return FindMovieByNameMovieAsyncTask(
            movieDao
        ).execute(name)
    }

    private class FindMovieByNameMovieAsyncTask(val movieDao: MovieDao) :
        AsyncTask<String, Unit, Movie>() {

        override fun doInBackground(vararg name: String): Movie? {
            return movieDao.findByName(name = name[0])?.asDomainModel()
        }
    }

    fun insert(movie: Movie): AsyncTask<DatabaseMovie, Unit, Long>? {
        return InsertMovieAsyncTask(
            movieDao
        ).execute(movie.asDatabaseModel())
    }

    private class InsertMovieAsyncTask(val movieDao: MovieDao) :
        AsyncTask<DatabaseMovie, Unit, Long>() {

        override fun doInBackground(vararg movie: DatabaseMovie): Long? {
            return movieDao.insert(movie = movie[0])
        }
    }
}