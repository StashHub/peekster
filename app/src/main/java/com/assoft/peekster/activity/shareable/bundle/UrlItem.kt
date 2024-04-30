package com.assoft.peekster.activity.shareable.bundle

import android.net.Uri
import java.io.IOException
import java.util.*

/**
 * A URL for transfer
 *
 * This type of item includes a single URL that can be sent to other devices
 * for either a notification or opening in the browser.
 */
class UrlItem : Item {

    private var mProperties: MutableMap<String, Any>

    /**
     * Create an item for receiving a URL
     * @param properties item properties
     */
    constructor(properties: MutableMap<String, Any>) {
        mProperties = properties
    }

    /**
     * Create a new item for the specified URI
     */
    constructor(uri: Uri) {
        mProperties = HashMap()
        mProperties[TYPE] = TYPE_NAME
        mProperties[NAME] = uri.toString()
        mProperties[SIZE] = 0
    }

    /**
     * Retrieve the URL
     */
    @get:Throws(IOException::class)
    val url: String?
        get() = getStringProperty(
            NAME,
            true
        )

    override val properties: MutableMap<String, Any>
        get() = mProperties

    @Throws(IOException::class)
    override fun open(mode: Mode?) {
    }

    @Throws(IOException::class)
    override fun read(data: ByteArray?): Int {
        return 0
    }

    @Throws(IOException::class)
    override fun write(data: ByteArray?) {
    }

    @Throws(IOException::class)
    override fun close() {
    }

    companion object {
        const val TYPE_NAME = "url"
    }
}