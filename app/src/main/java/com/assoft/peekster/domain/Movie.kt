package com.assoft.peekster.domain

/**
 * An object which represents a single [Movie].
 */
data class Movie(
    var title: String? = "",
    var popularity: String? = "",
    var vote_count: String? = "",
    var poster_path: String? = "",
    var backdrop_path: String? = "",
    var original_language: String? = "",
    var genres: String? = "",
    var vote_average: String? = "",
    var overview: String? = "",
    var release_date: String? = "",
    var adult: String? = "",
    var file_path: String? = "",
    var file_name: String? = ""
)