package com.assoft.peekster.domain

/**
 * An object which represents a single [HttpServer].
 */
data class HttpServer(
    var port: Int? = 0,
    var data: String? = "",
    var url: String? = ""
)