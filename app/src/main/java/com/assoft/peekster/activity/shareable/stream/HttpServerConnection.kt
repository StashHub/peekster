package com.assoft.peekster.activity.shareable.stream

import android.util.Log
import timber.log.Timber
import java.io.*
import java.net.Socket
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class HttpServerConnection(
    private val fileUriZ: ArrayList<UriInterpretation>?,
    private val connectionSocket: Socket,
    private val launcherActivity: StreamActivity
) : Runnable {
    private var theUriInterpretation: UriInterpretation? = null
    private var ipAddress = ""

    override fun run() {
        ipAddress = clientIpAddress
        launcherActivity.sendConnectionStartMessage(ipAddress)
        try {
            val theInputStream: InputStream
            theInputStream = try {
                connectionSocket.getInputStream()
            } catch (e1: IOException) {
                s("Error getting the InputString from connection socket.")
                e1.printStackTrace()
                return
            }
            val theOutputStream: OutputStream
            theOutputStream = try {
                connectionSocket.getOutputStream()
            } catch (e1: IOException) {
                s("Error getting the OutputStream from connection socket.")
                e1.printStackTrace()
                return
            }
            val input = BufferedReader(
                InputStreamReader(
                    theInputStream
                )
            )
            val output = DataOutputStream(theOutputStream)
            httpHandler(input, output)
            try {
                output.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } finally {
            s("Closing connection.")
            launcherActivity.sendConnectionEndMessage(ipAddress)
        }
    }

    private val clientIpAddress: String
        get() {
            val client = connectionSocket.inetAddress
            return client.hostName
        }

    // our implementation of the hypertext transfer protocol
    // its very basic and stripped down
    private fun httpHandler(
        input: BufferedReader,
        output: DataOutputStream
    ) {
        val header: String
        header = try {
            input.readLine()
        } catch (e1: IOException) {
            e1.printStackTrace()
            return
        }
        val upperCaseHeader = header.toUpperCase(Locale.getDefault())
        var sendOnlyHeader = false
        if (upperCaseHeader.startsWith("HEAD")) {
            sendOnlyHeader = true
        } else {
            if (!upperCaseHeader.startsWith("GET")) {
                dealWithUnsupportedMethod(output)
                return
            }
        }
        val path = getRequestedFilePath(header)
        if (path == "") {
            s("path is null")
            return
        }
        if (fileUriZ == null) {
            s("fileUri is null")
            return
        }
        val fileUriStr =
            if (fileUriZ.size == 1) fileUriZ[0].uri.toString() else fileUriZ.toString()

        s("Client requested: [$path][$fileUriStr]")

        if (path == "/favicon.ico") { // we have no favicon
            shareFavIcon(output)
            return
        }
        if (fileUriStr.startsWith("http://")
            || fileUriStr.startsWith("https://")
            || fileUriStr.startsWith("ftp://")
            || fileUriStr.startsWith("maito:")
            || fileUriStr.startsWith("callto:")
            || fileUriStr.startsWith("skype:")
        ) {
            // we will work as a simple URL redirector
            redirectToFinalPath(output, fileUriStr)
            return
        }
        theUriInterpretation = try {
            fileUriZ[0]
        } catch (e: SecurityException) {
            e.printStackTrace()
            s("Share Via HTTP has no permission to read such file.")
            return
        }
        if (path == "/") {
            shareRootUrl(output)
            return
        }
        shareOneFile(output, sendOnlyHeader, fileUriStr)
    }

    private fun shareOneFile(
        output: DataOutputStream,
        sendOnlyHeader: Boolean,
        fileUriStr: String
    ) {
        Timber.i("Streaming : ${theUriInterpretation?.name}")
        var requestedFile: InputStream? = null
        if (theUriInterpretation?.isDirectory != true) {
            try {
                requestedFile = theUriInterpretation?.inputStream
            } catch (e: FileNotFoundException) {
                try {
                    s("I couldn't locate file. I am sending the input as text/plain")
                    // instead of sending a 404, we will send the contact as text/plain
                    output.writeBytes(constructHttpHeader(200, "text/plain"))
                    output.writeBytes(fileUriStr)

                    // if you could not open the file send a 404
                    //s("Sending HTTP ERROR 404:" + e.getMessage());
                    //output.writeBytes(constructHttpHeader(404, null));
                    return
                } catch (e2: IOException) {
                    s("errorX:" + e2.message)
                    return
                }
            } // print error to gui
        }
        // happy day scenario
        val outputString = constructHttpHeader(
            200,
            theUriInterpretation?.mime
        )
        try {
            output.writeBytes(outputString)

            // if it was a HEAD request, we don't print any BODY
            if (!sendOnlyHeader) {
                if (theUriInterpretation?.isDirectory == true || fileUriZ!!.size > 1) {
                    val zz =
                        FileZipper(output, fileUriZ, launcherActivity.contentResolver)
                    zz.run()
                } else {
                    val buffer = ByteArray(4096)
                    var n = 0
                    while (requestedFile?.read(buffer).also {
                            if (it != null) {
                                n = it
                            }
                        } != -1) {
                        output.write(buffer, 0, n)
                    }
                }
            }
            requestedFile?.close()
        } catch (e: IOException) {
        }
    }

    private fun redirectToFinalPath(
        output: DataOutputStream,
        thePath: String
    ) {
        val redirectOutput = constructHttpHeader(302, null, thePath)
        try {
            // if you could not open the file send a 404
            output.writeBytes(redirectOutput)
            // close the stream
        } catch (e2: IOException) {
        }
    }

    private fun shareRootUrl(output: DataOutputStream) {
        if (theUriInterpretation?.isDirectory == true) {
            redirectToFinalPath(output, theUriInterpretation?.name.toString() + ".ZIP")
            return
        }
        if (fileUriZ!!.size == 1) {
            theUriInterpretation?.name?.let { redirectToFinalPath(output, it) }
        } else {
            val format = SimpleDateFormat("yyyy-MM-dd_HH_mm_ss", Locale.getDefault())
            redirectToFinalPath(
                output,
                "PeeksterBundle-" + format.format(Date()) + ".ZIP"
            )
        }
    }

    private fun shareFavIcon(output: DataOutputStream) {
        try {
            // if you could not open the file send a 404
            output.writeBytes(constructHttpHeader(404, null))
            // close the stream
        } catch (e2: IOException) {
        }
    }

    private fun getRequestedFilePath(inputHeader: String): String {
        val path: String

        // tmp contains "GET /index.html HTTP/1.0 ......."
        // find first space
        // find next space
        // copy whats between minus slash, then you get "index.html"
        // it's a bit of dirty code, but bear with me...
        var start = 0
        var end = 0
        for (a in inputHeader.indices) {
            if (inputHeader[a] == ' ' && start != 0) {
                end = a
                break
            }
            if (inputHeader[a] == ' ' && start == 0) {
                start = a
            }
        }
        path = inputHeader.substring(start + 1, end) // fill in the path
        return path
    }

    private fun dealWithUnsupportedMethod(output: DataOutputStream) {
        try {
            output.writeBytes(constructHttpHeader(501, null))
        } catch (e3: Exception) { // if some error happened catch it
            s("_error:" + e3.message)
        } // and display error
    }

    private fun s(s2: String) { // an alias to avoid typing so much!
        Timber.i("[$ipAddress] $s2")
    }

    // it is not always possible to get the file size :(
    private val fileSizeHeader: String
        get() {
            if (theUriInterpretation == null) {
                return ""
            }
            return if (fileUriZ!!.size == 1 && theUriInterpretation?.size!! > 0) {
                """
     Content-Length: ${theUriInterpretation?.size.toString()}
     
     """.trimIndent()
            } else ""
        }

    private fun generateRandomFileNameForTextPlainContent(): String {
        return "StringContent-" + (Math.random() * 100000000).roundToInt() + ".txt"
    }

    // This method makes the HTTP header for the response
    // the headers job is to tell the browser the result of the request
    // among if it was successful or not.
    private fun constructHttpHeader(
        return_code: Int, mime: String?,
        location: String? = null
    ): String {
        var mim = mime
        var loc = location
        val output = StringBuilder()
        output.append("HTTP/1.0 ")
        output.append(
            """
                ${httpReturnCodeToString(return_code)}
                
                """.trimIndent()
        )
        output.append(fileSizeHeader)
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.getDefault())
        output.append(
            """
                Date: ${format.format(Date())}
                
                """.trimIndent()
        )
        output.append("Connection: close\r\n") // we can't handle persistent
        // connections
        output.append("Server: ").append("Peekster").append(" ").append("\r\n")
        if (loc == null && return_code == 302) {
            loc = generateRandomFileNameForTextPlainContent()
        }
        if (loc != null) {
            // we don't want cache for the root URL
            try {
                val pos = loc.indexOf("://")
                if (pos in 1..9) {
                    // so russians can download their files as well :)
                    // but if a protocol like http://, than we may as well redirect
                    loc = URLEncoder.encode(loc, "UTF-8")
                    s("after urlencode location:$loc")
                }
            } catch (e: UnsupportedEncodingException) {
                s(Log.getStackTraceString(e))
            }
            output.append("Location: ").append(loc).append("\r\n") // server name
            output.append("Expires: Tue, 03 Jul 2001 06:00:00 GMT\r\n")
            output.append("Cache-Control: no-store, no-cache, must-revalidate, max-age=0\r\n")
            output.append("Cache-Control: post-check=0, pre-check=0\r\n")
            output.append("Pragma: no-cache\r\n")
        }
        if (mim != null) {
            if (fileUriZ!!.size > 1) {
                mim = "multipart/x-zip"
            }
            output.append("Content-Type: ").append(mim).append("\r\n")
        }
        output.append("\r\n")
        return output.toString()
    }

    companion object {
        private fun httpReturnCodeToString(return_code: Int): String {
            return when (return_code) {
                200 -> "200 OK"
                302 -> "302 Moved Temporarily"
                400 -> "400 Bad Request"
                403 -> "403 Forbidden"
                404 -> "404 Not Found"
                500 -> "500 Internal Server Error"
                501 -> "501 Not Implemented"
                else -> "501 Not Implemented"
            }
        }
    }
}