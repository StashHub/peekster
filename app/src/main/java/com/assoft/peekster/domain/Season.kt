package com.assoft.peekster.domain

import com.assoft.peekster.data.mediasource.TvShow

/**
 * An object which represents a single [Season].
 */
data class Season(
    /**
     * The season number associated with the [Season].
     */
    val seasonNumber: String? = "",

    /**
     * [List] of episodes associated with this [Season]
     */
    val episodes: List<TvShow>
)
