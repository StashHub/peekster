package com.assoft.peekster.data.mediasource

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import com.assoft.peekster.R
import com.assoft.peekster.util.ext.isTvShow
import com.assoft.peekster.util.ext.removeExtension
import com.assoft.peekster.util.ext.removeYear
import com.assoft.peekster.util.ext.replaced
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import java.util.concurrent.TimeUnit

class GetVideoContent(context: Context) {
    private val applicationContext: Context = context.applicationContext

    companion object {

        val externalContentUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val internalContentUri = MediaStore.Video.Media.INTERNAL_CONTENT_URI

        private lateinit var cursor: Cursor
        private var getVideoContent: GetVideoContent? = null

        fun getInstance(context: Context): GetVideoContent? {
            if (getVideoContent == null) {
                getVideoContent = GetVideoContent(context)
            }
            return getVideoContent
        }
    }

    /**
     * Returns a [List] of Movies
     */
    fun getAllMovieContent(contentLocation: Uri): List<Video> {
        val videoList = mutableListOf<Video>()

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
            val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
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
                if (optimized.isNullOrEmpty()) {

                    var contentName = name.removeExtension()
                    contentName = contentName.replaced()
                    contentName = contentName.removeYear()

                    // Stores column values and the contentUri in a local object
                    // that represents the media file.
                    videoList += Video(
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
        return videoList.toSet().toList() // Remove duplicates
    }

    /**
     * Returns a [List] of Movies by Folder
     */
    fun getAllMovieContentByFolder(contentLocation: Uri, folder: String): List<Video> {
        val videoList = mutableListOf<Video>()

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
                if (optimized.isNullOrEmpty()) {

                    var contentName = name.removeExtension()
                    contentName = contentName.replaced()
                    contentName = contentName.removeYear()

                    // Stores column values and the contentUri in a local object
                    // that represents the media file.
                    videoList += Video(
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
        return videoList.toSet().toList() // Remove duplicates
    }
}

/**
 * The adapter is called if imageUrl used for an [ImageView] object and imageUrl is a [Bitmap].
 */
@BindingAdapter("loadThumbnail")
fun ImageView.loadImage(source: String) {
    val option = RequestOptions()
        .skipMemoryCache(false)
        .placeholder(R.drawable.ic_default_img)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        .priority(Priority.HIGH)
        .dontAnimate()

    Glide.with(context)
        .setDefaultRequestOptions(option)
        .load(source.toUri())
        .into(this)
}

@BindingAdapter("loadAudioThumbnail")
fun ImageView.loadAudioArt(source: String) {
    val option = RequestOptions()
        .skipMemoryCache(false)
        .placeholder(R.drawable.ic_default_img)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        .priority(Priority.HIGH)
        .dontAnimate()

    Glide.with(context)
        .setDefaultRequestOptions(option)
        .load(Uri.parse(source))
        .into(this)
}