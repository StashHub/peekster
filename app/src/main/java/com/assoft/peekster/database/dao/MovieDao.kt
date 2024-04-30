package com.assoft.peekster.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.assoft.peekster.database.entities.DatabaseMovie

/**
 * The Data Access Object for the [DatabaseMovie] class.
 */
@Dao
interface MovieDao {

    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    fun loadAllByIds(ids: IntArray): List<DatabaseMovie>

    @Query("SELECT * FROM movies WHERE genres = :genre")
    fun loadAllByGenre(genre: String): List<DatabaseMovie>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(movies: List<DatabaseMovie>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(movie: DatabaseMovie) : Long

    @Query("SELECT * FROM movies")
    fun getMovies(): LiveData<List<DatabaseMovie>>

    @Query("SELECT * FROM movies WHERE file_name = :name")
    fun findByName(name: String): DatabaseMovie?

    @Delete
    fun delete(movie: DatabaseMovie)
}