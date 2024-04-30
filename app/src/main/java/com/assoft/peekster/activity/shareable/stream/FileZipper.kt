package com.assoft.peekster.activity.shareable.stream

import android.content.ContentResolver
import android.net.Uri
import timber.log.Timber
import java.io.*
import java.util.*
import java.util.zip.Adler32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileZipper (
    private val dest: OutputStream,
    private val inputUriInterpretations: ArrayList<UriInterpretation>?,
    private val contentResolver: ContentResolver
) : Runnable {

    private fun s(s2: String) { // an alias to avoid typing so much!
        Timber.i(s2)
    }

    private var atLeastOneDirectory = false
    private val fileNamesAlreadyUsed = HashSet<String>()

    override fun run() {
        val buffer = 4096
        try {
            s("Initializing ZIP")
            val checksum = CheckedOutputStream(dest, Adler32())
            val out = ZipOutputStream(
                BufferedOutputStream(
                    checksum
                )
            )

            val data = ByteArray(buffer)
            if (inputUriInterpretations != null) {
                for (thisUriInterpretation in inputUriInterpretations) {
                    addFileOrDirectory(buffer, out, data, thisUriInterpretation)
                }
            }
            out.close()
            s("Zip Done. Checksum: " + checksum.checksum.value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(FileNotFoundException::class, IOException::class)
    fun addFileOrDirectory(
        BUFFER: Int,
        out: ZipOutputStream,
        data: ByteArray?,
        uriFile: UriInterpretation
    ) {
        if (uriFile.isDirectory) {
            addDirectory(BUFFER, out, data, uriFile)
        } else {
            addFile(BUFFER, out, data, uriFile)
        }
    }

    @Throws(FileNotFoundException::class, IOException::class)
    fun addDirectory(
        BUFFER: Int,
        out: ZipOutputStream,
        data: ByteArray?,
        uriFile: UriInterpretation
    ) {
        atLeastOneDirectory = true
        var directoryPath: String = uriFile.uri.path!!
        if (directoryPath[directoryPath.length - 1] != File.separatorChar) {
            directoryPath += File.separatorChar
        }
        val entry = ZipEntry(directoryPath.substring(1))
        out.putNextEntry(entry)
        s("Adding Directory: $directoryPath")
        val f = File(directoryPath)
        val theFiles = f.list()
        if (theFiles != null) {
            for (aFilePath in theFiles) {
                if (aFilePath != "." && aFilePath != "..") {
                    val fixedFileName = ("file://" + directoryPath
                            + aFilePath)
                    val aFileUri = Uri.parse(fixedFileName)
                    val uriFile2 = UriInterpretation(aFileUri, contentResolver)
                    addFileOrDirectory(BUFFER, out, data, uriFile2)
                }
            }
        }
    }

    @Throws(FileNotFoundException::class, IOException::class)
    fun addFile(
        BUFFER: Int, out: ZipOutputStream, data: ByteArray?,
        uriFile: UriInterpretation
    ) {
        s("Adding File: " + uriFile.uri.path.toString() + " -- " + uriFile.name)
        val origin: BufferedInputStream = BufferedInputStream(uriFile.inputStream!!, BUFFER)
        val entry = ZipEntry(getFileName(uriFile))
        out.putNextEntry(entry)
        var count: Int
        while (origin.read(data!!, 0, BUFFER).also { count = it } != -1) {
            out.write(data, 0, count)
        }
        origin.close()
    }

    fun getFileName(uriFile: UriInterpretation): String {
        /*  The Android Galaxy sends us two files with the same name, we must make them unique or we get a
	    *  "java.util.zip.ZipException: duplicate entry: x.gif" exception
		*/
        var fileName = getFileNameHelper(uriFile)
        while (!fileNamesAlreadyUsed.add(fileName)) {
            // fileNamesAlreadyUsed.add returns TRUE if the file was added to the HashSet
            // false it it was already there.
            // in this case we must change the filename, to keep it unique
            fileName = "_$fileName"
        }
        return fileName
    }

    private fun getFileNameHelper(uriFile: UriInterpretation): String {
        /*	Galaxy Sends uri.getPath() with values like /external/images/media/16458
		 *  while urlFile.name returns IMG_20120427_120038.jpg
		 *
		 *  since such name has no directory info, that would break real directories
		 */
        return (if (atLeastOneDirectory) {
            uriFile.uri.path?.substring(1)
        } else uriFile.name).toString()
    }
}
