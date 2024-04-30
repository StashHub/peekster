package com.assoft.peekster.activity.shareable.bundle

import java.io.IOException

/**
 * Individual item for transfer
 *
 * Every individual file, URL, etc. for transfer must be an instance of a
 * class that implements Item. Items can have any number of properties, but
 * they must implement TYPE, NAME, and SIZE at a minimum.
 *
 * If items contain content (SIZE is nonzero), the I/O functions are used to
 * read and write the contents.
 */
abstract class Item {
    /**
     * Mode for opening items
     */
    enum class Mode {
        Read, Write
    }

    /**
     * Retrieve a map of properties
     * @return property map
     */
    abstract val properties: MutableMap<String, Any>

    /**
     * Retrieve the value of the specified property
     * @param key property to retrieve
     * @param defaultValue default value or null if required
     * @param class_ type for conversion
     * @param <T> type of value
     * @return value of the key
    </T> */
    @Throws(IOException::class)
    private fun <T> getProperty(
        key: String,
        defaultValue: T?,
        class_: Class<T>
    ): T? {
        return try {
            var value = class_.cast(properties[key])
            if (value == null) {
                value =
                    defaultValue ?: throw IOException(String.format("missing \"%s\" property", key))
            }
            value
        } catch (e: ClassCastException) {
            throw IOException(String.format("cannot read \"%s\" property", key))
        }
    }

    /**
     * Retrieve the value of a string property
     * @param key property to retrieve
     * @param required true to require a value
     * @return value of the key
     */
    @Throws(IOException::class)
    open fun getStringProperty(key: String, required: Boolean): String? {
        return getProperty(
            key,
            if (required) "" else null,
            String::class.java
        )
    }

    /**
     * Retrieve the value of a long property
     * @param key property to retrieve
     * @return value of the key
     */
    @Throws(IOException::class)
    fun getLongProperty(key: String, required: Boolean): Long? {
        return try {
            getProperty(
                key,
                if (required) "0" else null,
                String::class.java
            )?.toLong()
        } catch (e: NumberFormatException) {
            throw IOException(String.format("\"%s\" is not an integer", key))
        }
    }

    /**
     * Retrieve the value of a boolean property
     * @param key property to retrieve
     * @param required true to require a value
     * @return value of the key
     */
    @Throws(IOException::class)
    fun getBooleanProperty(key: String, required: Boolean): Boolean? {
        return getProperty(
            key,
            if (required) null else false,
            Boolean::class.java
        )
    }

    /**
     * Open the item for reading or writing
     * @param mode open mode
     */
    @Throws(IOException::class)
    abstract fun open(mode: Mode?)

    /**
     * Read data from the item
     * @param data array of bytes to read
     * @return number of bytes read
     * @throws IOException
     *
     * This method is invoked multiple times until all content has been read.
     */
    @Throws(IOException::class)
    abstract fun read(data: ByteArray?): Int

    /**
     * Write data to the item
     * @param data array of bytes to write
     */
    @Throws(IOException::class)
    abstract fun write(data: ByteArray?)

    /**
     * Close the item
     */
    @Throws(IOException::class)
    abstract fun close()

    companion object {
        /**
         * Unique identifier for the type of item
         */
        const val TYPE = "type"

        /**
         * Name of the item
         *
         * This value is displayed in some clients during transfer. Files, for
         * example, also use this property for the relative filename.
         */
        const val NAME = "name"

        /**
         * Size of the item content during transmission
         *
         * This number is sent over-the-wire as a string to avoid problems with
         * large integers in JSON. This number can be zero if there is no payload.
         */
        const val SIZE = "size"
    }
}