package com.assoft.peekster.domain

/**
 * An object which represents a single [TvShow].
 */
data class TvShow(
    var country: String? = "",
    var title: String? = "",
    var filepath: String? = "",
    var popularity: String? = "",
    var vote_count: String? = "",
    var poster_path: String? = "",
    var backdrop_path: String? = "",
    var original_language: String? = "",
    var genres: String? = "",
    var vote_average: String? = "",
    var overview: String? = "",
    var release_date: String? = "",
    var file_name: String? = ""
)