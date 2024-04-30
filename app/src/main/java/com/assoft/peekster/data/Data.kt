package com.assoft.peekster.data

import android.content.Context
import android.graphics.Bitmap
import com.assoft.peekster.domain.Audio
import com.assoft.peekster.domain.Category
import com.assoft.peekster.domain.FileDetail
import java.io.File
import java.io.FileOutputStream
import java.util.*

object Data {
    const val posterUrlMovie =
        "https://api.themoviedb.org/3/search/movie?api_key=15d2ea6d0dc1d476efbca3eba2b9bbfb&query="

    const val posterUrlTv =
        "https://api.themoviedb.org/3/search/tv?api_key=15d2ea6d0dc1d476efbca3eba2b9bbfb&query="

    const val imageUrl = "https://image.tmdb.org/t/p/original"

    val deezerSearchUrl = "https://api.deezer.com/search?q=track:"

    val genreIds = intArrayOf(
        28,
        12,
        80,
        99,
        18,
        10751,
        14,
        36,
        27,
        10402,
        9648,
        10749,
        878,
        10770,
        53,
        10752,
        37,
        35
    )
    var genre = arrayOf(
        "Action",
        "Adventure",
        "Crime",
        "Documentary",
        "Drama",
        "Family",
        "Fantasy",
        "History",
        "Horror",
        "Music",
        "Mystery",
        "Romance",
        "Science Fiction",
        "TV Movie",
        "Thriller",
        "War",
        "Western",
        "Comedy"
    )
    var selectedCategory: Category? = null

    var gridLayout = false

    val nowPlayingAudios: ArrayList<Audio>? = null

    val nowPlayingVideos: ArrayList<FileDetail>? = null

    var selectedInt = 0

    fun getAbsoluteDir(
        ctx: Context,
        optionalPath: String?
    ): String? {
        var rootPath: String = if (optionalPath != null && optionalPath != "") {
            ctx.getExternalFilesDir(optionalPath)!!.absolutePath
        } else {
            ctx.getExternalFilesDir(null)!!.absolutePath
        }
        // extraPortion is extra part of file path
        val extraPortion = "/Android/data"
        // Remove extraPortion
        rootPath = rootPath.substring(0, rootPath.indexOf(extraPortion))
        return rootPath
    }

    fun saveImageTo(context: Context, name: String, folder: String, bitmap: Bitmap) {
        Thread(Runnable {
            val root: String = getAbsoluteDir(context, "").toString()
            val dir = File("$root/Peekster/.$folder")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val fname = "$name.jpg"
            val file = File(dir, fname)
            if (file.exists()) file.delete()
            try {
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }).start()
    }
}