package com.assoft.peekster.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.assoft.peekster.R
import com.assoft.peekster.databinding.ItemContentLayoutContainerBinding
import com.assoft.peekster.activity.MainEventListener
import com.assoft.peekster.activity.MediaViewModel
import com.assoft.peekster.database.entities.Category

class ContentAdapter(
    private val listener: MainEventListener
) : ListAdapter<Category, ViewHolder>(CategoryDiff) {

    private var categories = emptyList<Category>()

    override fun getItemCount() = categories.size

    override fun getItemId(position: Int): Long {
        return categories[position].id
    }

    override fun getItemViewType(position: Int) = R.layout.item_content_layout_container

    override fun submitList(list: MutableList<Category>?) {
        this.categories = list as List<Category>
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                viewType,
                parent,
                false
            ),
            listener
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }
}

class ViewHolder(
    private val binding: ItemContentLayoutContainerBinding,
    private val listener: MainEventListener
) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(category: Category) {
        binding.category = category
        binding.listener = listener
        binding.viewModel = listener as MediaViewModel
        binding.executePendingBindings()
    }
}

object CategoryDiff : DiffUtil.ItemCallback<Category>() {
    override fun areItemsTheSame(oldItem: Category, newItem: Category) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Category, newItem: Category) = oldItem == newItem
}