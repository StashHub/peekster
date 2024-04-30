package com.assoft.peekster.activity.shareable.stream

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Intent.CATEGORY_OPENABLE
import android.content.Intent.EXTRA_ALLOW_MULTIPLE
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.lifecycle.Observer
import com.assoft.peekster.R
import com.assoft.peekster.activity.Activity
import com.assoft.peekster.activity.shareable.stream.StreamShareActivity.Companion.ACTION_SEND_SINGLE
import com.assoft.peekster.databinding.ActivityLocalNetworkBinding
import com.assoft.peekster.domain.HttpServers
import com.assoft.peekster.util.contentView
import com.assoft.peekster.util.ext.toast
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

class StreamActivity : Activity() {

    /** Internal variable for obtaining the [ActivityLocalNetworkBinding] binding. */
    private val binding: ActivityLocalNetworkBinding by contentView(R.layout.activity_local_network)

    /**
     * Internal reference to the [Handler] used to send/receive messages from the thread's message queue
     */
    private lateinit var handler: Handler

    /**
     * Internal reference to the [HttpStreamServer] used to stream the media content
     */
    private var httpServer: HttpStreamServer? = null

    /**
     * Internal reference to the preferred server URL
     */
    private lateinit var preferredServerUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(inputMessage: Message) {
                when (inputMessage.what) {
                    HANDLER_CONNECTION_START -> {
                        val msg = String.format(
                            getString(R.string.connected_ip),
                            inputMessage.obj as String
                        )
                        runOnUiThread {
                            toast(msg)
                        }
                    }
                    HANDLER_CONNECTION_END -> {
                        val msg2 = String.format(
                            getString(R.string.disconnected_ip),
                            inputMessage.obj as String
                        )
                        runOnUiThread {
                            toast(msg2)
                        }
                    }
                    else -> super.handleMessage(inputMessage)
                }
            }
        }
        binding.apply {
            toolbar.setNavigationOnClickListener {
                userInteractionNavigation = true
                finish()
            }
            fab.setOnClickListener {
                userInteractionNavigation = true
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(CATEGORY_OPENABLE)
                    putExtra(EXTRA_ALLOW_MULTIPLE, false)
                    type = "*/*"
                }
                startActivityForResult(
                    intent, REQUEST_CODE
                )
            }
        }

        mainViewModel.getServer()?.observe(this,
            Observer { response ->
                response?.let { server ->
                    binding.apply {
                        tvEmpty.visibility = View.GONE
                        streamingContent.visibility = View.VISIBLE
                    }
                    val uris: ArrayList<UriInterpretation> =
                        getFileUri(Uri.parse(server.data))
                    val uriPath = populateUriPath(uris)

                    httpServer = HttpStreamServer(server.port!!, uris, this)
                    preferredServerUrl = httpServer!!.listOfIpAddresses()[0].toString()

                    val stream = HttpServers(
                        id = server.port!!,
                        name = uriPath,
                        item = uris,
                        serverUrl = preferredServerUrl,
                        httpStreamServer = httpServer,
                        socketChannel = httpServer!!.getSocket()
                    )
                    setUpView(stream)
                } ?: run {
                    binding.apply {
                        tvEmpty.visibility = View.VISIBLE
                        streamingContent.visibility = View.GONE
                    }
                }
            })
    }

    fun sendConnectionStartMessage(ipAddress: String) {
        handler.handleMessage(
            handler.obtainMessage(
                HANDLER_CONNECTION_START,
                ipAddress
            )
        )
    }

    fun sendConnectionEndMessage(ipAddress: String) {
        handler.handleMessage(
            handler.obtainMessage(
                HANDLER_CONNECTION_END,
                ipAddress
            )
        )
    }

    private fun setUpView(httpServers: HttpServers) {
        val status = httpServers.socketChannel?.isClosed

        binding.apply {
            mediaTitle.text = httpServers.name
            httpUrl.paintFlags = httpUrl.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            httpUrl.text = httpServers.serverUrl
            httpServerStatus.text = status?.let { closed ->
                if (closed) getString(R.string.server_closed) else getString(R.string.server_open)
            } ?: getString(R.string.server_open)

            serverAction.setOnClickListener {
                val server = httpServer
                httpServer = null

                server?.let {
                    server.stopServer()
                }
                mainViewModel.delete(httpServers.id)
            }
            shareAction.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, httpServers.serverUrl)
                }
                startActivity(
                    Intent.createChooser(
                        shareIntent,
                        getString(R.string.share_url)
                    )
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            data?.let {
                val uris: ArrayList<UriInterpretation> = getFileUris(data)
                val port = 9999

                // Stop server before starting new one if exist
                httpServer?.stopServer()
                mainViewModel.delete(port)

                // Initialize server
                httpServer = HttpStreamServer(port, uris, this)
                preferredServerUrl = httpServer!!.listOfIpAddresses()[0].toString()
                saveServerUrlToClipboard()

                mainViewModel.insertServer(
                    port = port,
                    data = data.data.toString(),
                    url = preferredServerUrl
                )
                // Start share activity
                val shareIntent = Intent(this, StreamShareActivity::class.java)
                    .setAction(ACTION_SEND_SINGLE)
                    .putExtra(Intent.EXTRA_STREAM, preferredServerUrl)
                startActivityForResult(
                    shareIntent,
                    SHARE_REQUEST
                )
            }
        }
    }

    // Future update to allow multiple streams simultaneously
    private fun nextFreePort(): Int {
        var port: Int = Random(System.currentTimeMillis()).nextInt(49152, 65535)
        while (true) {
            port = if (isLocalPortFree(port)) {
                return port
            } else {
                ThreadLocalRandom.current().nextInt(49152, 65535)
            }
        }
    }

    private fun isLocalPortFree(port: Int): Boolean {
        return try {
            ServerSocket(port).close()
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun saveServerUrlToClipboard() {
        val clipboard =
            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(preferredServerUrl, preferredServerUrl))
        Snackbar.make(
            findViewById(android.R.id.content),
            getString(R.string.url_clipboard),
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun getFileUris(data: Intent): ArrayList<UriInterpretation> {
        val theUris =
            ArrayList<UriInterpretation>()
        val dataUri = data.data
        if (dataUri != null) {
            theUris.add(UriInterpretation(dataUri, contentResolver))
        } else {
            getFileUrisFromClipboard(data, theUris)
        }
        return theUris
    }

    private fun getFileUri(data: Uri?): ArrayList<UriInterpretation> {
        val theUris =
            ArrayList<UriInterpretation>()
        if (data != null) {
            theUris.add(UriInterpretation(data, contentResolver))
        }
        return theUris
    }

    private fun populateUriPath(uriList: ArrayList<UriInterpretation>): String {
        val output = StringBuilder()
        val sep = "\n"
        var first = true
        for (thisUriInterpretation in uriList) {
            if (first) {
                first = false
            } else {
                output.append(sep)
            }
            output.append(thisUriInterpretation.path)
        }
        return output.toString()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        userInteractionNavigation = true
    }

    override fun onPause() {
        super.onPause()
        if (userInteractionNavigation)
            return else userInteractionNavigation = false
    }

    private fun getFileUrisFromClipboard(
        data: Intent,
        theUris: ArrayList<UriInterpretation>
    ) {
        val clipData = data.clipData
        for (i in 0 until clipData!!.itemCount) {
            val item = clipData.getItemAt(i)
            val uri = item.uri
            theUris.add(UriInterpretation(uri, contentResolver))
        }
    }

    companion object {
        const val REQUEST_CODE = 1024
        private const val SHARE_REQUEST = 1001
        const val HANDLER_CONNECTION_START = 42
        const val HANDLER_CONNECTION_END = 4242
    }
}