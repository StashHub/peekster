package com.assoft.peekster.data.mediasource

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.assoft.peekster.util.ext.removeExtension
import com.assoft.peekster.util.ext.removeYear
import com.assoft.peekster.util.ext.replaced
import java.util.concurrent.TimeUnit

class GetAudioContent(context: Context) {
    private val applicationContext: Context = context.applicationContext

    companion object {

        val externalContentUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val internalContentUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI

        private lateinit var cursor: Cursor
        private var getAudioContent: GetAudioContent? = null

        fun getInstance(context: Context): GetAudioContent? {
            if (getAudioContent == null) {
                getAudioContent = GetAudioContent(context)
            }
            return getAudioContent
        }
    }

    /**
     * Returns a [List] of Movies
     */
    fun getAllAudioContent(contentLocation: Uri): List<Audio> {
        val audioList = mutableListOf<Audio>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.IS_MUSIC
        )

        // Show only audio that are music
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES).toString()
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Media.DURATION} ASC"

        val query = applicationContext.contentResolver.query(
            contentLocation,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val title = cursor.getString(titleColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val albumId = cursor.getString(albumIdColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistColumn)
                val filePath = cursor.getString(dataColumn)

                val artwork = Uri.parse("content://media/external/audio/albumart")
                val imageUri = Uri.withAppendedPath(artwork, albumId.toString())

                var contentTitle = title.removeExtension()
                contentTitle = contentTitle.replaced()
                contentTitle = contentTitle.removeYear()

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                audioList += Audio(
                    cursor.position.toLong(),
                    filePath,
                    imageUri.toString(),
                    contentTitle,
                    artist,
                    album,
                    duration,
                    size
                )
            }
        }
        return audioList.toSet().toList() // Remove duplicates
    }

    /**
     * Returns a [List] of Audio by Folder
     */
    fun getAllAudioContentByFolder(contentLocation: Uri, folder: String): List<Audio> {
        val audioList = mutableListOf<Audio>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.IS_MUSIC
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} AND ${MediaStore.Audio.Media.DATA} like ?"
        val selectionArgs = arrayOf("%$folder%")

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Media.DURATION} ASC"

        val query = applicationContext.contentResolver.query(
            contentLocation,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val title = cursor.getString(titleColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val albumId = cursor.getString(albumIdColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistColumn)
                val filePath = cursor.getString(dataColumn)

                val artwork = Uri.parse("content://media/external/audio/albumart")
                val imageUri = Uri.withAppendedPath(artwork, albumId.toString())

                var contentTitle = title.removeExtension()
                contentTitle = contentTitle.replaced()
                contentTitle = contentTitle.removeYear()

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                audioList += Audio(
                    cursor.position.toLong(),
                    filePath,
                    imageUri.toString(),
                    contentTitle,
                    artist,
                    album,
                    duration,
                    size
                )
            }
        }
        return audioList.toSet().toList() // Remove duplicates
    }
}
