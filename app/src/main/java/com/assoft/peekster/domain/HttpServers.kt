package com.assoft.peekster.domain

import com.assoft.peekster.activity.shareable.stream.HttpStreamServer
import com.assoft.peekster.activity.shareable.stream.UriInterpretation
import java.io.Serializable
import java.net.ServerSocket

/**
 * An object which represents a single [HttpServers].
 */
class HttpServers(
    var id: Int = 0, // port
    var name: String? = "", // name of media content
    var item: ArrayList<UriInterpretation>,
    var serverUrl: String? = "", // external url
    var httpStreamServer: HttpStreamServer? = null, // httpServer
    var socketChannel: ServerSocket? = null // socket
) : Serializable