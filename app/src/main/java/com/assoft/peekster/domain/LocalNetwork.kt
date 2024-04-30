package com.assoft.peekster.domain

import org.videolan.libvlc.interfaces.IMedia
import java.io.Serializable

/**
 * An object which represents a single [LocalNetwork].
 */
class LocalNetwork(
    var title: String? = "",
    var smb: String? = "",
    var media: IMedia? = null
) : Serializable
