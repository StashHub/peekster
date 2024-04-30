package com.assoft.peekster.domain

/**
 * An object which represents a single [Audio].
 */
data class Audio(
    val path: String,
    val title: String,
    val album: String,
    val artist: String,
    val albumArt: String,
    val posterUrl: String,
    val duration: String
)