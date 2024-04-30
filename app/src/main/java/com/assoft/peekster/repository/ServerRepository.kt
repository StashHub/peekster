package com.assoft.peekster.repository

import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.assoft.peekster.database.dao.HttpServerDao
import com.assoft.peekster.database.entities.DatabaseHttpServer
import com.assoft.peekster.database.entities.asDatabaseModel
import com.assoft.peekster.database.entities.asDomainModel
import com.assoft.peekster.domain.HttpServer

class ServerRepository(
    private val httpServerDao: HttpServerDao
) {

    private val allServers: LiveData<DatabaseHttpServer?>? = httpServerDao.getServers()

    fun findServerByPort(port: Int): AsyncTask<Int, Unit, HttpServer>? {
        return FindServerByPortAsyncTask(
            httpServerDao
        ).execute(port)
    }

    private class FindServerByPortAsyncTask(val httpServerDao: HttpServerDao) :
        AsyncTask<Int, Unit, HttpServer>() {

        override fun doInBackground(vararg port: Int?): HttpServer? {
            return port[0]?.let { httpServerDao. findByPort(port = it)?.asDomainModel() }
        }
    }

    fun getServer() : LiveData<HttpServer?>? {
        return allServers?.map {
            it?.asDomainModel()
        }
    }

    fun insert(httpServer: HttpServer): AsyncTask<DatabaseHttpServer, Unit, Unit>? {
        return InsertServerAsyncTask(
            httpServerDao
        ).execute(httpServer.asDatabaseModel())
    }

    private class InsertServerAsyncTask(val httpServerDao: HttpServerDao) :
        AsyncTask<DatabaseHttpServer, Unit, Unit>() {

        override fun doInBackground(vararg server: DatabaseHttpServer): Unit? {
            return httpServerDao.insert(server = server[0])
        }
    }

    fun delete(port: Int): AsyncTask<Int, Unit, Unit>? {
        return DeleteServerAsyncTask(
            httpServerDao
        ).execute(port)
    }

    private class DeleteServerAsyncTask(val httpServerDao: HttpServerDao) :
        AsyncTask<Int, Unit, Unit>() {

        override fun doInBackground(vararg port: Int?): Unit? {
            return port[0]?.let { httpServerDao.delete(port = it) }
        }
    }
}