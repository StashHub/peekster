package com.assoft.peekster.activity.shareable.bundle

import android.content.res.AssetFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

/**
 * An individual file for transfer
 *
 * Note that Android's Java doesn't include java.nio.file so only the
 * last_modified property is usable on the platform.
 */
class FileItem : Item {
    private lateinit var file: File

    private var assetFileDescriptor: AssetFileDescriptor? = null

    private var props: MutableMap<String, Any>
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    /**
     * Create a new file item using the provided properties
     * @param transferDirectory directory for the file
     * @param properties map of properties for the file
     * @param overwrite true to overwrite an existing file
     */
    constructor(
        transferDirectory: String,
        properties: MutableMap<String, Any>,
        overwrite: Boolean
    ) {
        props = properties
        val parentDirectory = File(transferDirectory)
        val filename = getStringProperty(
            NAME,
            true
        )
        filename?.let {
            file = File(parentDirectory, filename)
        }
        if (!overwrite) {
            var i = 2
            while (file.exists()) {
                val matcher = sRenamePattern.matcher(filename as CharSequence)
                if (!matcher.matches()) {
                    throw IOException("Unable to match regexp")
                }
                val newFilename =
                    String.format(
                        "%s_%d%s",
                        matcher.group(1),
                        i++,
                        if (matcher.group(2) == null) "" else matcher.group(2)
                    )
                file = File(parentDirectory, newFilename)
            }
        }
    }

    /**
     * Create a new file item from the specified file
     */
    @JvmOverloads
    constructor(file: File, filename: String = file.name) {
        this.file = file
        props = HashMap()
        props[TYPE] = TYPE_NAME
        props[NAME] = filename
        props[SIZE] = this.file.length().toString()
        props[READ_ONLY] = !this.file.canWrite()
        props[EXECUTABLE] = this.file.canExecute()
        props[LAST_MODIFIED] = this.file.lastModified().toString()

        // TODO: these are used for temporary compatibility with 0.3.x
        props["created"] = "0"
        props["last_read"] = "0"
        props["directory"] = false
    }

    /**
     * Create a file from the specified asset file descriptor and URI
     * @param assetFileDescriptor file descriptor
     * @param filename filename to use
     */
    constructor(assetFileDescriptor: AssetFileDescriptor?, filename: String) {
        this.assetFileDescriptor = assetFileDescriptor
        props = HashMap()
        props[TYPE] = TYPE_NAME
        props[NAME] = filename
        props[SIZE] = this.assetFileDescriptor?.length.toString()

        // TODO: these are used for temporary compatibility with 0.3.x
        props["created"] = "0"
        props["last_read"] = "0"
        props["last_modified"] = "0"
        props["directory"] = false
    }

    /**
     * Retrieve the underlying path for the item
     */
    val path: String
        get() = file.path

    override val properties: MutableMap<String, Any>
        get() = props

    @Throws(IOException::class)
    override fun open(mode: Mode?) {
        when (mode) {
            Mode.Read -> inputStream = FileInputStream(file)
            Mode.Write -> {
                file.parentFile?.mkdirs()
                outputStream = FileOutputStream(file)
            }
        }
    }

    @Throws(IOException::class)
    override fun read(data: ByteArray?): Int {
        var numBytes: Int = inputStream?.read(data as ByteArray) ?: 0
        if (numBytes == -1) {
            numBytes = 0
        }
        return numBytes
    }

    @Throws(IOException::class)
    override fun write(data: ByteArray?) {
        outputStream?.write(data as ByteArray)
    }

    @Throws(IOException::class)
    override fun close() {
        inputStream?.close()
        assetFileDescriptor?.close()
        outputStream?.let {
            outputStream?.close()
          //  getBooleanProperty(READ_ONLY, false)?.let { it1 -> file.setWritable(it1) }
           // getBooleanProperty(EXECUTABLE, false)?.let { it1 -> file.setExecutable(it1) }
//            val lastModified = getLongProperty(LAST_MODIFIED, false)
//            if (lastModified != 0L) {
//                if (lastModified != null) {
//                    file.setLastModified(lastModified)
//                }
//            }
        }
    }

    companion object {
        const val TYPE_NAME = "file"

        // Additional properties for files
        private const val READ_ONLY = "read_only"
        private const val EXECUTABLE = "executable"
        private const val LAST_MODIFIED = "last_modified"

        // Regexp for renaming files
        private val sRenamePattern =
            Pattern.compile("^(.*?)((?:\\.tar)?\\.[^/]*)?$")
    }
}