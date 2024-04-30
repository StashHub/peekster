package com.assoft.peekster.data.mediasource

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Container for information about each video.
 */
@Parcelize
data class Video(
    val id: Long,
    val path: String,
    val thumbnail: String,
    var name: String,
    val duration: Int,
    val size: Int,
    val resolution: String?,
    val description: String?,
    val language: String?
) : Parcelable
