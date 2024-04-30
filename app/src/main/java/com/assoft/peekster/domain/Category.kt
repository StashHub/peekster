package com.assoft.peekster.domain

/**
 * An object which represents a single [Category].
 */
data class Category(
    val name: String = "",
    val type: String = "",
    val path: String = "",
    val id: String = ""
)