package com.assoft.peekster.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.assoft.peekster.database.entities.DatabaseHttpServer

/**
 * The Data Access Object for the [DatabaseHttpServer] class.
 */
@Dao
interface HttpServerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(servers: List<DatabaseHttpServer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(server: DatabaseHttpServer)

    @Query("SELECT * FROM server")
    fun getServers(): LiveData<DatabaseHttpServer?>

    @Query("SELECT * FROM server WHERE port = :port")
    fun findByPort(port: Int): DatabaseHttpServer?

    @Query("DELETE FROM server WHERE port = :port")
    fun delete(port: Int)
}