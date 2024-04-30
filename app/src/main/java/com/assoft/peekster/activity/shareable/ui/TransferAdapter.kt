package com.assoft.peekster.activity.shareable.ui

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.assoft.peekster.R
import com.assoft.peekster.activity.shareable.transfer.TransferService
import com.assoft.peekster.activity.shareable.transfer.TransferStatus
import com.assoft.peekster.activity.shareable.transfer.TransferStatus.State.Connecting
import com.assoft.peekster.activity.shareable.transfer.TransferStatus.State.Transferring
import com.assoft.peekster.activity.shareable.transfer.TransferStatus.State.Failed
import com.assoft.peekster.activity.shareable.transfer.TransferStatus.State.Succeeded
import com.google.android.material.button.MaterialButton

/**
 * Transfer adapter that shows transfers in progress
 */
class TransferAdapter(private val context: Context) :
    RecyclerView.Adapter<TransferAdapter.ViewHolder?>() {

    private val statuses: SparseArray<TransferStatus> = SparseArray<TransferStatus>()

    override fun getItemCount() = statuses.size()

    /**
     * Update the information for a transfer in the sparse array
     */
    fun update(transferStatus: TransferStatus) {
        val index = statuses.indexOfKey(transferStatus.id)
        if (index < 0) {
            statuses.put(transferStatus.id, transferStatus)
            notifyItemInserted(statuses.size())
        } else {
            statuses.setValueAt(index, transferStatus)
            notifyItemChanged(index)
        }
    }

    /**
     * Retrieve the status for the specified index
     */
    fun getStatus(index: Int): TransferStatus {
        return statuses.valueAt(index)
    }

    /**
     * Remove the specified transfer from the sparse array
     */
    fun remove(index: Int) {
        statuses.removeAt(index)
        notifyItemRemoved(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_item_transfers, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transferStatus: TransferStatus = statuses.valueAt(position)

        // Generate transfer byte string
        val bytesText: CharSequence
        bytesText = if (transferStatus.bytesTotal == 0L) {
            context.getString(R.string.adapter_transfer_unknown)
        } else {
            context.getString(
                R.string.adapter_transfer_bytes,
                Formatter.formatShortFileSize(
                    context,
                    transferStatus.bytesTransferred
                ),
                Formatter.formatShortFileSize(
                    context,
                    transferStatus.bytesTotal
                )
            )
        }

        // Set the attributes
        holder.icon.setImageResource(
            if (transferStatus.direction == TransferStatus.Direction.Receive) R.drawable.ic_get_app_24px
            else R.drawable.ic_publish_24px
        )
        holder.device.text = transferStatus.remoteDeviceName
        holder.progress.progress = transferStatus.progress
        holder.bytes.text = bytesText
        when (transferStatus.state) {
            Connecting, Transferring -> {
                if (transferStatus.state == Connecting) {
                    holder.state.setText(R.string.adapter_transfer_connecting)
                } else {
                    holder.state.text = context.getString(
                        R.string.adapter_transfer_transferring,
                        transferStatus.progress
                    )
                }
                holder.state.setTextColor(
                    ContextCompat.getColor(
                        context,
                        android.R.color.darker_gray
                    )
                )
                holder.buttonStop.visibility = View.VISIBLE
                holder.buttonStop.setOnClickListener {
                    val stopIntent: Intent = Intent(context, TransferService::class.java)
                        .setAction(TransferService.ACTION_STOP_TRANSFER)
                        .putExtra(TransferService.EXTRA_TRANSFER, transferStatus.id)
                    context.startService(stopIntent)
                }
            }
            Succeeded -> {
                holder.state.setText(R.string.adapter_transfer_succeeded)
                holder.state.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.colorSuccess
                    )
                )
                holder.buttonStop.visibility = View.INVISIBLE
            }
            Failed -> {
                holder.state.text = context.getString(
                    R.string.adapter_transfer_failed,
                    transferStatus.error
                )
                holder.state.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.colorError
                    )
                )
                holder.buttonStop.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * View holder for individual transfers
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.transfer_icon)
        val device: TextView = itemView.findViewById(R.id.transfer_device)
        val state: TextView = itemView.findViewById(R.id.transfer_state)
        val progress: ProgressBar = itemView.findViewById(R.id.transfer_progress)
        val bytes: TextView = itemView.findViewById(R.id.transfer_bytes)
        val buttonStop: MaterialButton = itemView.findViewById(R.id.transfer_action)
    }
}