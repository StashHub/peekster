package com.assoft.peekster.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.assoft.peekster.database.entities.DatabaseTvShow

/**
 * The Data Access Object for the [DatabaseTvShow] class.
 */
@Dao
interface TvShowDao {

    @Query("SELECT * FROM tv WHERE id IN (:ids)")
    fun loadAllByIds(ids: IntArray): List<DatabaseTvShow>

    @Query("SELECT * FROM tv WHERE genres = :genre")
    fun loadAllByGenre(genre: String): List<DatabaseTvShow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tvShow: List<DatabaseTvShow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tvShow: DatabaseTvShow) : Long

    @Query("SELECT * FROM tv")
    fun getTvShows(): LiveData<List<DatabaseTvShow>>

    @Query("SELECT * FROM tv WHERE file_name = :name")
    fun findByName(name: String): DatabaseTvShow?

    @Delete
    fun delete(tvShow: DatabaseTvShow)
}