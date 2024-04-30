package com.assoft.peekster.data.mediasource

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.assoft.peekster.util.ext.isTvShow
import com.assoft.peekster.util.ext.removeExtension
import com.assoft.peekster.util.ext.removeYear
import com.assoft.peekster.util.ext.replaced
import java.util.concurrent.TimeUnit

class GetTvShowContent(context: Context) {
    private val applicationContext: Context = context.applicationContext

    companion object {

        val externalContentUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val internalContentUri = MediaStore.Video.Media.INTERNAL_CONTENT_URI

        private lateinit var cursor: Cursor
        private var getVideoContent: GetTvShowContent? = null

        fun getInstance(context: Context): GetTvShowContent? {
            if (getVideoContent == null) {
                getVideoContent = GetTvShowContent(context)
            }
            return getVideoContent
        }
    }

    /**
     * Returns a [List] of TvShows
     */
    fun getAllTvShowContent(contentLocation: Uri): List<TvShow> {
        val videoList = mutableListOf<TvShow>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.ALBUM,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.DESCRIPTION,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.LANGUAGE,
            MediaStore.Video.Media.CATEGORY,
            MediaStore.Video.Media.DATA
        )

        // Show only videos that are at least 5 minutes in duration.
        val selection = "${MediaStore.Video.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        val query = applicationContext.contentResolver.query(
            contentLocation,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            val descriptionColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DESCRIPTION)
            val resolutionColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
            val languageColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.LANGUAGE)
            val categoryColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.CATEGORY)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                if(nameColumn == -1) continue

                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val description = cursor.getString(descriptionColumn)
                val resolution = cursor.getString(resolutionColumn)
                val language = cursor.getString(languageColumn)
                val category = cursor.getString(categoryColumn)
                val filePath = cursor.getString(dataColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    externalContentUri,
                    id
                )

                val optimized = name.isTvShow()
                if (!optimized.isNullOrEmpty()) {
                    var contentName = optimized.removeExtension()
                    contentName = contentName.removeYear()

                    // Stores column values and the contentUri in a local object
                    // that represents the media file.
                    videoList += TvShow(
                        0L,
                        filePath,
                        contentUri.toString(),
                        contentName,
                        duration,
                        size,
                        resolution,
                        description,
                        language
                    )
                }
            }
        }
        return videoList.distinctBy { it.name }  // Remove duplicates
    }

    /**
     * Returns a [List] of Tv Shows by Folder
     */
    fun getAllTvShowContentByFolder(contentLocation: Uri, folder: String): List<TvShow> {
        val videoList = mutableListOf<TvShow>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.ALBUM,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.DESCRIPTION,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.LANGUAGE,
            MediaStore.Video.Media.CATEGORY,
            MediaStore.Video.Media.DATA
        )

        val selection = "${MediaStore.Video.Media.DATA} like ?"
        val selectionArgs = arrayOf("%$folder%")

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        val query = applicationContext.contentResolver.query(
            contentLocation,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            val descriptionColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DESCRIPTION)
            val resolutionColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
            val languageColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.LANGUAGE)
            val categoryColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.CATEGORY)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                if(nameColumn == -1) continue

                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val description = cursor.getString(descriptionColumn)
                val resolution = cursor.getString(resolutionColumn)
                val language = cursor.getString(languageColumn)
                val category = cursor.getString(categoryColumn)
                val filePath = cursor.getString(dataColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    contentLocation,
                    id
                )

                // If video content is not a tv show, we assume its a movie
                val optimized = name.isTvShow()
                if (!optimized.isNullOrEmpty()) {
                    var contentName = optimized.removeExtension()
                    contentName = contentName.removeYear()

                    // Stores column values and the contentUri in a local object
                    // that represents the media file.
                    videoList += TvShow(
                        cursor.position.toLong(),
                        filePath,
                        contentUri.toString(),
                        contentName,
                        duration,
                        size,
                        resolution,
                        description,
                        language
                    )
                }
            }
        }
        return videoList.distinctBy { it.name }// Remove duplicates
    }

    fun getAllTvShowEpisodes(
        contentLocation: Uri,
        currentName: String
    ): List<TvShow> {
        val videoList = mutableListOf<TvShow>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.ALBUM,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.DESCRIPTION,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.LANGUAGE,
            MediaStore.Video.Media.CATEGORY,
            MediaStore.Video.Media.DATA
        )

        // Show only videos that are at least 5 minutes in duration.
        val selection = "${MediaStore.Video.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        val query = applicationContext.contentResolver.query(
            contentLocation,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            val descriptionColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DESCRIPTION)
            val resolutionColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
            val languageColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.LANGUAGE)
            val categoryColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.CATEGORY)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                if(nameColumn == -1) continue

                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val description = cursor.getString(descriptionColumn)
                val resolution = cursor.getString(resolutionColumn)
                val language = cursor.getString(languageColumn)
                val category = cursor.getString(categoryColumn)
                val filePath = cursor.getString(dataColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    contentLocation,
                    id
                )

                // If video content is not a tv show, we assume its a movie
                val optimized = name.isTvShow()
                if (!optimized.isNullOrEmpty()) {
                    var contentName = optimized.removeExtension()
                    contentName = contentName.removeYear()

                    if (contentName.equals(currentName, true)) {
                        // Stores column values and the contentUri in a local object
                        // that represents the media file.
                        videoList += TvShow(
                            cursor.position.toLong(),
                            filePath,
                            contentUri.toString(),
                            name.removeExtension().replaced(),
                            duration,
                            size,
                            resolution,
                            description,
                            language
                        )
                    }
                }
            }
        }
        return videoList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it.name }))
    }
}