package com.assoft.peekster.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.assoft.peekster.R
import com.assoft.peekster.data.mediasource.TvShow
import com.assoft.peekster.databinding.TvShowsItemBinding
import com.assoft.peekster.util.ext.extractEpisodeName
import com.assoft.peekster.util.ext.removeYear
import com.assoft.peekster.util.ext.replaced
import com.assoft.peekster.activity.MainEventListener

class EpisodesAdapter(
    private val listener: MainEventListener
) : RecyclerView.Adapter<EpisodesViewHolder>() {
    var tvShow = emptyList<TvShow>()

    override fun getItemCount() = tvShow.size

    override fun getItemId(position: Int): Long {
        return tvShow[position].id
    }

    override fun getItemViewType(position: Int) = R.layout.tv_shows_item

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodesViewHolder {
        return EpisodesViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                viewType,
                parent,
                false
            ),
            listener
        )
    }

    override fun onBindViewHolder(holder: EpisodesViewHolder, position: Int) {
        holder.bind(tvShow[position])
        holder.binding.title.text = tvShow[position].name.extractEpisodeName()
        holder.binding.roundCornerFrameLayout.setOnClickListener {
            val fullName = tvShow[position].path?.substringAfterLast("/")
            val filName = fullName?.substringBeforeLast(".")
            if (filName != null) {
                tvShow[position].name = filName.replaced().removeYear().extractEpisodeName()
            }
            listener.playTvShow(tvShow = tvShow[position], position = position)
        }
    }
}

class EpisodesViewHolder(
    val binding: TvShowsItemBinding,
    private val listener: MainEventListener
) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(tvShow: TvShow) {
        binding.tv = tvShow
        binding.listener = listener
        binding.executePendingBindings()
    }
}