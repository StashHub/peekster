package com.assoft.peekster.data.mediasource

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Container for information about each Audio.
 */
@Parcelize
data class Audio(
    val id: Long,
    val path: String,
    val thumbnail: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val size: Int
) : Parcelable