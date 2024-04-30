package com.assoft.peekster.database

import androidx.room.Database
import androidx.room.TypeConverters
import android.content.Context
import android.os.AsyncTask
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.assoft.peekster.data.mediasource.GetAudioContent
import com.assoft.peekster.data.mediasource.GetVideoContent
import com.assoft.peekster.database.converter.Converters
import com.assoft.peekster.database.dao.CategoryDao
import com.assoft.peekster.database.dao.HttpServerDao
import com.assoft.peekster.database.dao.MovieDao
import com.assoft.peekster.database.dao.TvShowDao
import com.assoft.peekster.database.entities.Category
import com.assoft.peekster.database.entities.DatabaseHttpServer
import com.assoft.peekster.database.entities.DatabaseMovie
import com.assoft.peekster.database.entities.DatabaseTvShow

/**
 * The [Room] database for Peekster.
 */
@Database(
    entities = [
        DatabaseMovie::class,
        DatabaseTvShow::class,
        DatabaseHttpServer::class,
        Category::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PeeksterDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao
    abstract fun tvShowDao(): TvShowDao
    abstract fun categoryDao(): CategoryDao
    abstract fun httpServerDao(): HttpServerDao

    companion object {
        private const val databaseName = "peekster-db"

        private lateinit var INSTANCE: PeeksterDatabase

        fun getInstance(context: Context): PeeksterDatabase {
            if (!::INSTANCE.isInitialized) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    PeeksterDatabase::class.java, databaseName
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(roomCallback)
                    .build()
            }
            return INSTANCE
        }

        private val roomCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                PopulateDbAsyncTask(INSTANCE)
                    .execute()
            }
        }
    }

    class PopulateDbAsyncTask(db: PeeksterDatabase?) : AsyncTask<Unit, Unit, Unit>() {
        private val categoryDao = db?.categoryDao()

        override fun doInBackground(vararg p0: Unit?) {
            categoryDao?.insertAll(
                Category(
                    name = "Movie",
                    type = "Movie",
                    path = GetVideoContent.externalContentUri.toString(),
                    folder = "",
                    fixed = true
                ),
                Category(
                    name = "Tv Show",
                    type = "Tv Show",
                    path = GetVideoContent.externalContentUri.toString(),
                    folder = "",
                    fixed = true
                ),
                Category(
                    name = "Music",
                    type = "Music",
                    path = GetAudioContent.externalContentUri.toString(),
                    folder = "",
                    fixed = true
                )
            )
        }
    }
}