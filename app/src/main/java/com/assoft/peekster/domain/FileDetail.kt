package com.assoft.peekster.domain

/**
 * An object which represents a single [FileDetail].
 */
data class FileDetail(
    val name: String? = "",
    val path: String? = "",
    val optimizedName: String? = ""
)