package com.assoft.peekster.activity.shareable.transfer

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class TransferStatus(
    var id: Int = 0,
    var direction: Direction = Direction.Receive,
    var remoteDeviceName: String? = null,
    var state: State = State.Transferring,
    var progress: Int = 0,
    var bytesTransferred: Long = 0L,
    var bytesTotal: Long = 0L,
    var error: String? = null
) : Parcelable{
    /**
     * State of the transfer
     */
    enum class State {
        Connecting, Transferring, Failed, Succeeded
    }

    /**
     * Direction of transfer relative to the current device
     */
    enum class Direction {
        Receive, Send
    }

    val isFinished: Boolean
        get() = state == State.Succeeded || state == State.Failed
}