package com.assoft.peekster.activity.shareable.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.assoft.peekster.R
import com.assoft.peekster.activity.shareable.transfer.TransferManager
import com.assoft.peekster.activity.shareable.transfer.TransferService
import  com.assoft.peekster.activity.shareable.transfer.TransferStatus
import com.google.android.material.floatingactionbutton.FloatingActionButton
import timber.log.Timber

class TransferFragment : Fragment() {

    private lateinit var broadcastReceiver: BroadcastReceiver

    private lateinit var recyclerView: RecyclerView

    private lateinit var emptyTextView: TextView

    /**
     * The system calls this when it's time for the fragment
     * to draw its user interface for the first time.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Create layout parameters for full expansion
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Create a container
        val parentView: ViewGroup = LinearLayout(context)
        parentView.layoutParams = layoutParams

        // Setup the adapter and recycler view
        val adapter = TransferAdapter(parentView.context)
        recyclerView = RecyclerView(parentView.context)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.layoutParams = layoutParams
        recyclerView.visibility = View.GONE
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        parentView.addView(recyclerView)

        // Setup the empty view
        emptyTextView = TextView(context)
        emptyTextView.gravity = Gravity.CENTER
        emptyTextView.layoutParams = layoutParams
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            emptyTextView.setTextAppearance(R.style.TextAppearance_Peekster_Headline6)
        } else {
            emptyTextView.setTextAppearance(
                parentView.context,
                R.style.TextAppearance_Peekster_Headline6
            )
        }
        emptyTextView.setText(R.string.activity_transfer_empty_text)
        emptyTextView.setTextColor(Color.parseColor("#FFFFFF"))
        parentView.addView(emptyTextView)

        // Hide the FAB when the user scrolls
        val fab: FloatingActionButton? = activity?.findViewById(R.id.fab)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    fab?.hide()
                } else {
                    fab?.show()
                }
            }
        })

        // Enable swipe-to-dismiss
        ItemTouchHelper(
            object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(
                    viewHolder: RecyclerView.ViewHolder,
                    direction: Int
                ) {
                    // Calculate the position of the item and retrieve its status
                    val position = viewHolder.adapterPosition
                    val transferStatus: TransferStatus = adapter.getStatus(position)

                    // Remove the item from the adapter
                    adapter.remove(position)

                    // If none remain, reshow the empty text
                    if (adapter.itemCount == 0) {
                        recyclerView.visibility = View.GONE
                        emptyTextView.visibility = View.VISIBLE
                    }

                    // Remove the item from the servicels

                    val removeIntent: Intent = Intent(context, TransferService::class.java)
                        .setAction(TransferService.ACTION_REMOVE_TRANSFER)
                        .putExtra(TransferService.EXTRA_TRANSFER, transferStatus.id)
                    context?.startService(removeIntent)
                }
            }
        ).attachToRecyclerView(recyclerView)

        // Disable change animations (because they are really, really ugly)
        (recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                val transferStatus: TransferStatus =
                    intent.getParcelableExtra(TransferManager.EXTRA_STATUS) as TransferStatus
                adapter.update(transferStatus)
                if (adapter.itemCount == 1) {
                    recyclerView.visibility = View.VISIBLE
                    emptyTextView.visibility = View.GONE
                }
            }
        }
        return parentView
    }

    override fun onStart() {
        super.onStart()
        Timber.i("Registering broadcast receiver")

        // Start listening for broadcasts
        context?.registerReceiver(
            broadcastReceiver,
            IntentFilter(TransferManager.TRANSFER_UPDATED)
        )

        // Get fresh data from the service
        val broadcastIntent = Intent(context, TransferService::class.java)
            .setAction(TransferService.ACTION_BROADCAST)
        context?.startService(broadcastIntent)
    }

    override fun onStop() {
        super.onStop()
        Timber.i("Unregistering broadcast receiver")

        // Stop listening for broadcasts
        context?.unregisterReceiver(broadcastReceiver)
    }
}