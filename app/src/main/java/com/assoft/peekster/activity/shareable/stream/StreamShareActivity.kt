package com.assoft.peekster.activity.shareable.stream

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.database.DataSetObserver
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import com.assoft.peekster.R
import com.assoft.peekster.activity.Activity
import com.assoft.peekster.activity.shareable.util.Settings
import com.assoft.peekster.domain.NetworkDevice
import timber.log.Timber
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket

class StreamShareActivity : Activity() {

    /**
     * Adapter that discovers other devices on the network
     */
    private inner class DeviceAdapter(context: Context) : ArrayAdapter<String?>(
        context,
        R.layout.view_simple_list_item,
        R.id.text1
    ) {

        /**
         * Maintain a mapping of device IDs to discovered devices
         */
        private val devices: MutableMap<String, NetworkDevice> = mutableMapOf()

        /**
         * Maintain a queue of devices to resolve
         */
        private val queue: MutableList<NsdServiceInfo> = mutableListOf()

        private lateinit var nsdManager: NsdManager
        private lateinit var thisDeviceName: String

        /**
         * Prepare to resolve the next service
         *
         * For some inexplicable reason, Android chokes miserably when
         * resolving more than one service at a time. The queue performs each
         * resolution sequentially.
         */
        private fun prepareNextService() {
            synchronized(queue) {
                queue.removeAt(0)
                if (queue.size == 0) {
                    return
                }
            }
            resolveNextService()
        }

        /**
         * Resolve the next service in the queue
         */
        private fun resolveNextService() {
            var serviceInfo: NsdServiceInfo
            synchronized(queue) { serviceInfo = queue[0] }
            Timber.d(
                String.format("resolving \"%s\"", serviceInfo.serviceName)
            )
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    Timber.d(
                        String.format("resolved \"%s\"", serviceInfo.serviceName)
                    )
                    val device = NetworkDevice(
                        nick_name = serviceInfo.serviceName,
                        device_id = "",
                        host = serviceInfo.host,
                        port = serviceInfo.port
                    )
                    runOnUiThread {
                        devices[serviceInfo.serviceName] = device
                        add(serviceInfo.serviceName)
                    }
                    prepareNextService()
                }

                override fun onResolveFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int
                ) {
                    Timber.e(
                        String.format(
                            "Unable to resolve \"%s\": %d",
                            serviceInfo.serviceName, errorCode
                        )
                    )
                    prepareNextService()
                }
            })
        }

        /**
         * Listener for discovery events
         */
        private val discoveryListener: DiscoveryListener = object : DiscoveryListener {
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceName == thisDeviceName) {
                    return
                }
                Timber.i(
                    String.format(
                        "found \"%s\"; queued for resolving",
                        serviceInfo.serviceName
                    )
                )
                var resolve: Boolean
                synchronized(queue) {
                    resolve = queue.size == 0
                    queue.add(serviceInfo)
                }
                if (resolve) {
                    resolveNextService()
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.i(
                    String.format("lost \"%s\"", serviceInfo.serviceName)
                )
                runOnUiThread {
                    remove(serviceInfo.serviceName)
                    devices.remove(serviceInfo.serviceName)
                }
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Timber.d("Service discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.d("Service discovery stopped")
            }

            override fun onStartDiscoveryFailed(
                serviceType: String,
                errorCode: Int
            ) {
                Timber.e("Unable to start service discovery")
            }

            override fun onStopDiscoveryFailed(
                serviceType: String,
                errorCode: Int
            ) {
                Timber.e("Unable to stop service discovery")
            }
        }

        fun start() {
            nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            nsdManager.discoverServices(
                NetworkDevice.SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD, discoveryListener
            )
            thisDeviceName = Settings(context).deviceName.toString()
        }

        fun stop() {
            nsdManager.stopServiceDiscovery(discoveryListener)
        }

        /**
         * Retrieve the specified device
         * @param position device index
         * @return device at the specified position
         */
        fun getDevice(position: Int): NetworkDevice? {
            return devices[getItem(position)]
        }

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            var view = convertView
            if (convertView == null) {
                val layoutInflater = LayoutInflater.from(context)
                view = layoutInflater.inflate(R.layout.view_simple_list_item, parent, false)
            }
            val device: NetworkDevice? = devices[getItem(position)]
            (view?.findViewById<View>(R.id.text1) as TextView).text = device?.nick_name
            (view.findViewById<View>(R.id.text2) as TextView).text = device?.host?.hostAddress
            (view.findViewById<View>(R.id.icon) as ImageView).setImageResource(
                R.drawable.ic_devices_white_24dp
            )
            return view
        }
    }

    private lateinit var deviceAdapter: DeviceAdapter

    /**
     * Ensure that the intent passed to the activity is valid
     * @return true if the intent is valid
     */
    private fun isValidIntent(): Boolean {
        return (intent.action == ACTION_SEND_MULTIPLE) || intent.action == ACTION_SEND_SINGLE ||
                intent.action == Intent.ACTION_SEND &&
                intent.hasExtra(Intent.EXTRA_STREAM)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream_share)

        // Ensure valid data is present in the intent
        if (isValidIntent()) {
            deviceAdapter = DeviceAdapter(context = this)
            deviceAdapter.registerDataSetObserver(object : DataSetObserver() {
                override fun onChanged() {
                    findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                }
            })
            deviceAdapter.start()

            val serverUrl = intent.getStringExtra(Intent.EXTRA_STREAM) as String
            val listView: ListView = findViewById(R.id.selectList)
            listView.adapter = deviceAdapter
            listView.onItemClickListener =
                OnItemClickListener { _, _, position, _ ->
                    val device: NetworkDevice = deviceAdapter.getDevice(position) as NetworkDevice
                    val data = device.nick_name + ";" + serverUrl
                    Thread(Runnable {
                        try {
                            synchronized(this) {
                                val socketChannel = Socket(device.host, 40820)
                                val printWriter = PrintWriter(socketChannel.getOutputStream())
                                printWriter.apply {
                                    write(data)
                                    flush()
                                    close()
                                }.also {
                                    socketChannel.close()
                                }
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }).start()
                    // Close the activity
                    this@StreamShareActivity.setResult(RESULT_OK)
                    finish()
                }
        }
    }

    override fun onDestroy() {
        deviceAdapter.stop()
        userInteractionNavigation = true
        super.onDestroy()
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

    companion object {
        const val ACTION_SEND_SINGLE = "com.assoft.peekster.SEND_SINGLE"
    }
}