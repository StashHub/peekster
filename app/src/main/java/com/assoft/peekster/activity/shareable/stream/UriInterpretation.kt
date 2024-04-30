package com.assoft.peekster.activity.shareable.stream

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.assoft.peekster.util.ext.removeExtension
import com.assoft.peekster.util.ext.removeYear
import com.assoft.peekster.util.ext.replaced
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.Serializable
import java.net.URLDecoder
import java.util.*

class UriInterpretation(val uri: Uri, private val contentResolver: ContentResolver) : Serializable {
    var size: Long = -1
        private set
    var name: String? = null
    var path: String? = null
    var thumbnail: String? = null
    var mime: String? = null
        private set
    var isDirectory = false
        private set

    @get:Throws(FileNotFoundException::class)
    val inputStream: InputStream?
        get() = contentResolver.openInputStream(uri)

    private fun getFileSize(uri: Uri) {
        if (size <= 0) {
            var uriString = uri.toString()
            if (uriString.startsWith("file://")) {
                var f = File(uriString.substring("file://".length))
                isDirectory = f.isDirectory
                if (isDirectory) {
                    size = 0
                    return
                }
                size = f.length()
                if (size == 0L) {
                    uriString = URLDecoder.decode(uriString).substring(
                        "file://".length
                    )
                    f = File(uriString)
                    size = f.length()
                }
            } else {
                try {
                    val f = File(uriString)
                    isDirectory = f.isDirectory
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Timber.v("Not a file: $uriString")
            }
        }
    }

    private fun getMime(uri: Uri, contentResolver: ContentResolver) {
        mime = contentResolver.getType(uri)
        if (mime == null || name == null) {
            mime = "application/octet-stream"
            if (name == null) {
                return
            }
        }
        if (mime == "application/octet-stream") {
            // we can do better than that
            val pos = name?.lastIndexOf('.')
            if (pos != null) {
                if (pos < 0) return
            }
            val extension = name?.substring(pos!!)?.toLowerCase(Locale.getDefault())
            if (extension == ".jpg") {
                mime = "image/jpeg"
                return
            }
            if (extension == ".png") {
                mime = "image/png"
                return
            }
            if (extension == ".gif") {
                mime = "image/gif"
                return
            }
            if (extension == ".mp4") {
                mime = "video/mp4"
                return
            }
            if (extension == ".avi") {
                mime = "video/avi"
                return
            }
            if (extension == ".mov") {
                mime = "video/mov"
                return
            }
            if (extension == ".vcf") {
                mime = "text/x-vcard"
                return
            }
            if (extension == ".txt") {
                mime = "text/plain"
                return
            }
            if (extension == ".html") {
                mime = "text/html"
                return
            }
            if (extension == ".json") {
                mime = "application/json"
                return
            }
            if (extension == ".epub") {
                mime = "application/epub+zip"
                return
            }
        }
    }

    init {
        getMime(uri, contentResolver)

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val query = contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentName = cursor.getString(nameColumn)

                name = contentName
                path = contentName
                size = cursor.getInt(sizeColumn).toLong()

                val contentUri: Uri = ContentUris.withAppendedId(
                    uri,
                    id
                )
                thumbnail = contentUri.toString()
            }
        }
        if (name == null) {
            name = uri.lastPathSegment
            path = uri.toString()
        }
        getMime(uri, contentResolver)
        getFileSize(uri)
    }
}
