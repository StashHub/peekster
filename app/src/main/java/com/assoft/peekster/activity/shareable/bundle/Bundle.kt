package com.assoft.peekster.activity.shareable.bundle

import java.io.IOException
import kotlin.collections.ArrayList

/**
 * List of items to be transferred
 */
class Bundle : ArrayList<Item>() {
    /**
     * Retrieve the total size of the bundle content
     * @return total size in bytes
     */
    var totalSize: Long = 0
        private set

    /**
     * Add the specified item to the bundle for transfer
     */
    @Throws(IOException::class)
    fun addItem(item: Item) {
        add(item)
        totalSize += item.getLongProperty(Item.SIZE, true)!!
    }
}
