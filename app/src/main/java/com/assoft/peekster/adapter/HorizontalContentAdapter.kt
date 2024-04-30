package com.assoft.peekster.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.assoft.peekster.R
import com.assoft.peekster.data.Data
import com.assoft.peekster.data.Data.gridLayout
import com.assoft.peekster.data.mediasource.Audio
import com.assoft.peekster.data.mediasource.TvShow
import com.assoft.peekster.data.mediasource.Video
import com.assoft.peekster.domain.Movie
import com.assoft.peekster.activity.MainEventListener
import com.assoft.peekster.activity.MediaViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.json.JSONException
import org.json.JSONObject

class HorizontalMovieAdapter(
    private val context: Context,
    private val listener: MainEventListener
) : RecyclerView.Adapter<MovieViewHolder>() {

    private var genreIds: ArrayList<Int> = ArrayList()
    private var genreNames: ArrayList<String> = ArrayList()

    private lateinit var recyclerView: RecyclerView

    init {
        for (index in 0..17) {
            genreIds.add(Data.genreIds[index])
            genreNames.add(Data.genre[index])
        }
    }

    var movies = mutableListOf<Video>()

    override fun getItemCount() = movies.size

    override fun getItemId(position: Int): Long {
        return movies[position].hashCode().toLong()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun getItemViewType(position: Int): Int {
        return if (gridLayout) {
            R.layout.item_movie_vertical_item
        } else {
            R.layout.item_movie_horizontal_item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        return if (gridLayout) {
            MovieViewHolder(
                LayoutInflater.from(context)
                    .inflate(viewType, parent, false)
            )
        } else {
            MovieViewHolder(
                LayoutInflater.from(context)
                    .inflate(viewType, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val kidModeEnabled = (listener as MediaViewModel).isChileModeEnabled()
        val movie = listener.findMovieByName(movies[position].name)
        val excludeMovie = movie?.genres?.let {
            it.contains("Action") || it.contains("Horror") || movie.adult.equals("true")
        } ?: run {
            movie?.adult.equals("true")
        }

        val param = holder.itemView.layoutParams
        if (kidModeEnabled && excludeMovie) {
            param.width = 0
            param.height = 0
        } else {
            val scale = context.resources.displayMetrics.density
            val dpWidthInPx = (138 * scale).toInt()
            val dpHeightInPx = if (gridLayout) (212 * scale).toInt() else (204 * scale).toInt()

            param.width = dpWidthInPx
            param.height = dpHeightInPx

            holder.poster.setImageResource(R.drawable.ic_default_img)
            holder.title.text = ""

            holder.constraintLayout.setOnClickListener {
                listener.showMovieDetail(movies[position])
            }

            movie?.let {
                holder.title.text = movie.title
                Glide.with(context)
                    .load(movie.poster_path)
                    .into(holder.poster)
            } ?: run {
                fetchMovieFromTMdb(holder, position)
            }
        }
        holder.itemView.layoutParams = param
    }

    /** Fetch movie data from TMDB */
    private fun fetchMovieFromTMdb(viewHolder: MovieViewHolder, position: Int) {
        val url: String = Data.posterUrlMovie + movies[position].name

        val option = RequestOptions()
            .skipMemoryCache(false)
            .placeholder(R.drawable.ic_default_img)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .priority(Priority.HIGH)
            .dontAnimate()
        val postRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    viewHolder.title.text = movies[position].name
                    val jsonObjectResponse = JSONObject(response)
                    if (jsonObjectResponse.getInt("total_results") > 0) {
                        val jsonArray = jsonObjectResponse.getJSONArray("results")
                        val jsonObject = jsonArray.getJSONObject(0)
                        val movie = Movie()
                        movie.title = jsonObject.getString("title")
                        movie.popularity = jsonObject.getString("popularity")
                        movie.vote_count = jsonObject.getString("vote_count")
                        movie.poster_path = jsonObject.getString("poster_path")
                        movie.adult = jsonObject.getString("adult")
                        movie.backdrop_path = jsonObject.getString("backdrop_path")
                        movie.original_language = jsonObject.getString("original_language")

                        var generS = ""
                        try {
                            val genres = jsonObject.getJSONArray("genre_ids")
                            for (i in 0 until genres.length()) {
                                val id = genres.getInt(i)
                                generS = if (generS.isEmpty()) {
                                    genreNames[genreIds.indexOf(id)]
                                } else {
                                    generS + ", " + genreNames[genreIds.indexOf(id)]
                                }
                            }
                            movie.genres = generS
                        } catch (e: java.lang.Exception) {
                            movie.genres = generS
                        }
                        movie.vote_average = jsonObject.getString("vote_average")
                        movie.overview = jsonObject.getString("overview")
                        movie.release_date = jsonObject.getString("release_date")

                        Glide.with(context)
                            .setDefaultRequestOptions(option)
                            .asBitmap()
                            .load(Data.imageUrl + movie.poster_path)
                            .into(object : CustomTarget<Bitmap?>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap?>?
                                ) {
                                    Glide.with(context)
                                        .load(resource)
                                        .into(viewHolder.poster)
                                    val poster: String =
                                        Data.getAbsoluteDir(context, "")
                                            .toString() + "/Peekster/.Movies/Posters/" + movie.title + ".jpg"
                                    val cover: String = Data.getAbsoluteDir(context, "")
                                        .toString() + "/Peekster/.Movies/Covers/" + movie.title + ".jpg"
                                    val check: Long? = (listener as MediaViewModel).insert(
                                        movies[position].name,
                                        movie.title!!,
                                        movie.popularity!!,
                                        movie.vote_count!!,
                                        poster,
                                        movie.adult!!,
                                        cover,
                                        movie.original_language!!,
                                        movie.genres,
                                        movie.vote_average!!,
                                        movie.overview!!,
                                        movie.release_date!!,
                                        movie.file_path
                                    )

                                    movie.title?.let {
                                        Data.saveImageTo(
                                            context,
                                            it, "Movies/Posters", resource
                                        )
                                    }
                                    if (check == -1L) Toast.makeText(
                                        context,
                                        "Movie Not Added",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {}
                            })

                        Glide.with(context)
                            .setDefaultRequestOptions(option)
                            .asBitmap()
                            .load(Data.imageUrl + movie.backdrop_path)
                            .into(object : CustomTarget<Bitmap?>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap?>?
                                ) {
                                    movie.title?.let {
                                        Data.saveImageTo(
                                            context,
                                            it, "Movies/Covers", resource
                                        )
                                    }
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {}
                            })
                        viewHolder.title.text = movie.title
                    }
                } catch (e: JSONException) {
                    viewHolder.title.text = movies[position].name
                    Glide.with(context)
                        .setDefaultRequestOptions(option)
                        .load(movies[position].path)
                        .into(viewHolder.poster)
                }
            },
            Response.ErrorListener {
                viewHolder.title.text = movies[position].name
                Glide.with(context)
                    .setDefaultRequestOptions(option)
                    .load(movies[position].path)
                    .into(viewHolder.poster)
            }
        )
        Volley.newRequestQueue(context).add(postRequest)
    }
}

class MovieViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    var title: TextView = itemView.findViewById(R.id.title)
    var poster: ImageView = itemView.findViewById(R.id.iv_poster)
    var constraintLayout: ConstraintLayout = itemView.findViewById(R.id.constraintLayout)
}

class HorizontalMusicAdapter(
    private val context: Context,
    private val listener: MainEventListener
) : RecyclerView.Adapter<MusicViewHolder>() {

    var music = emptyList<Audio>()

    override fun getItemCount() = music.size

    override fun getItemId(position: Int): Long {
        return music[position].id
    }

    override fun getItemViewType(position: Int): Int {
        return if (gridLayout) {
            R.layout.item_music_vertical_item
        } else {
            R.layout.item_music_horizontal_item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        return if (gridLayout) {
            MusicViewHolder(
                LayoutInflater.from(context)
                    .inflate(viewType, parent, false)
            )
        } else {
            MusicViewHolder(
                LayoutInflater.from(context)
                    .inflate(viewType, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.poster.setImageResource(R.drawable.ic_default_img)
        holder.title.text = music[position].title
        holder.artist.text = music[position].artist

        val option = RequestOptions()
            .skipMemoryCache(false)
            .placeholder(R.drawable.ic_default_img)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .priority(Priority.HIGH)
            .dontAnimate()

        Glide.with(context)
            .setDefaultRequestOptions(option)
            .load(Uri.parse(music[position].thumbnail))
            .into(holder.poster)

        holder.constraintLayout.setOnClickListener {
            listener.playAudio(music[position])
        }
    }
}

class MusicViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    var title: TextView = itemView.findViewById(R.id.title)
    var poster: ImageView = itemView.findViewById(R.id.iv_poster)
    var artist: TextView = itemView.findViewById(R.id.artist)
    var constraintLayout: ConstraintLayout = itemView.findViewById(R.id.constraintLayout)
}

class HorizontalTvShowAdapter(
    private val context: Context,
    private val listener: MainEventListener
) : RecyclerView.Adapter<TvShowViewHolder>() {

    private var genreIds: ArrayList<Int> = ArrayList()
    private var genreNames: ArrayList<String> = ArrayList()

    init {
        for (index in 0..17) {
            genreIds.add(Data.genreIds[index])
            genreNames.add(Data.genre[index])
        }
    }

    var tvShow = emptyList<TvShow>()

    override fun getItemCount() = tvShow.size

    override fun getItemId(position: Int): Long {
        return tvShow[position].id
    }

    override fun getItemViewType(position: Int): Int {
        return if (gridLayout) {
            R.layout.item_tv_vertical_item
        } else {
            R.layout.item_tv_horizontal_item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TvShowViewHolder {
        return if (gridLayout) {
            TvShowViewHolder(
                LayoutInflater.from(context)
                    .inflate(viewType, parent, false)
            )
        } else {
            TvShowViewHolder(
                LayoutInflater.from(context)
                    .inflate(viewType, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: TvShowViewHolder, position: Int) {
        val kidModeEnabled = (listener as MediaViewModel).isChileModeEnabled()
        val tv = listener.findTvShowByName(tvShow[position].name)
        val excludeTvShow = tv?.genres?.let {
            it.contains("Action") || it.contains("Horror")
        } ?: run {
            false
        }

        if (kidModeEnabled && excludeTvShow) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        } else {
            val scale = context.resources.displayMetrics.density
            val dpWidthInPx = (138 * scale).toInt()
            val dpHeightInPx = (204 * scale).toInt()

            holder.itemView.visibility = View.VISIBLE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(
                dpWidthInPx,
                dpHeightInPx
            )

            holder.poster.setImageResource(R.drawable.ic_default_img)
            holder.title.text = ""

            holder.constraintLayout.setOnClickListener {
                listener.showTvShowDetail(tvShow[position])
            }

            val tvShow = listener.findTvShowByName(tvShow[position].name)
            tvShow?.let {
                holder.title.text = tvShow.title
                Glide.with(context)
                    .load(tvShow.poster_path)
                    .into(holder.poster)
            } ?: run {
                fetchTvShowFromTMdb(holder, position)
            }
        }
    }

    /** Fetch tv show data from TMDB */
    private fun fetchTvShowFromTMdb(viewHolder: TvShowViewHolder, position: Int) {
        val url: String = Data.posterUrlTv + tvShow[position].name

        val option = RequestOptions()
            .skipMemoryCache(false)
            .placeholder(R.drawable.ic_default_img)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .priority(Priority.HIGH)
            .dontAnimate()
        val postRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    viewHolder.title.text = tvShow[position].name
                    val jsonObjectResponse = JSONObject(response)
                    if (jsonObjectResponse.getInt("total_results") > 0) {
                        val jsonArray = jsonObjectResponse.getJSONArray("results")
                        val jsonObject = jsonArray.getJSONObject(0)
                        val tv = com.assoft.peekster.domain.TvShow()
                        tv.title = jsonObject.getString("name")
                        tv.popularity = jsonObject.getString("popularity")
                        tv.vote_count = jsonObject.getString("vote_count")
                        tv.poster_path = jsonObject.getString("poster_path")
                        tv.backdrop_path = jsonObject.getString("backdrop_path")
                        tv.original_language = jsonObject.getString("original_language")

                        var generS = ""
                        try {
                            val genres = jsonObject.getJSONArray("genre_ids")
                            for (i in 0 until genres.length()) {
                                val id = genres.getInt(i)
                                generS = if (generS.isEmpty()) {
                                    genreNames[genreIds.indexOf(id)]
                                } else {
                                    generS + ", " + genreNames[genreIds.indexOf(id)]
                                }
                            }
                            tv.genres = generS
                        } catch (e: java.lang.Exception) {
                            tv.genres = generS
                        }
                        tv.vote_average = jsonObject.getString("vote_average")
                        tv.overview = jsonObject.getString("overview")
                        tv.release_date = jsonObject.getString("first_air_date")
                        tv.country = jsonObject.getString("origin_country")

                        Glide.with(context)
                            .setDefaultRequestOptions(option)
                            .asBitmap()
                            .load(Data.imageUrl + tv.poster_path)
                            .into(object : CustomTarget<Bitmap?>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap?>?
                                ) {
                                    Glide.with(context)
                                        .load(resource)
                                        .into(viewHolder.poster)
                                    val poster: String =
                                        Data.getAbsoluteDir(context, "")
                                            .toString() + "/PeekSter/.TvShows/Posters/" + tv.title + ".jpg"
                                    val cover: String = Data.getAbsoluteDir(context, "")
                                        .toString() + "/PeekSter/.TvShows/Covers/" + tv.title + ".jpg"
                                    val check: Long? =
                                        (listener as MediaViewModel).insertTvShow(
                                            tvShow[position].name,
                                            tv.title!!,
                                            tv.popularity!!,
                                            tv.vote_count!!,
                                            poster,
                                            tv.country!!,
                                            cover,
                                            tv.original_language!!,
                                            tv.genres,
                                            tv.vote_average!!,
                                            tv.overview!!,
                                            tv.release_date!!,
                                            tv.filepath
                                        )

                                    tv.title?.let {
                                        Data.saveImageTo(
                                            context,
                                            it, "TvShows/Posters", resource
                                        )
                                    }
                                    if (check == -1L) Toast.makeText(
                                        context,
                                        "Tv Show Not Added",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {}
                            })

                        Glide.with(context)
                            .setDefaultRequestOptions(option)
                            .asBitmap()
                            .load(Data.imageUrl + tv.backdrop_path)
                            .into(object : CustomTarget<Bitmap?>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap?>?
                                ) {
                                    tv.title?.let {
                                        Data.saveImageTo(
                                            context,
                                            it, "TvShows/Covers", resource
                                        )
                                    }
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {}
                            })
                        viewHolder.title.text = tv.title
                    }
                } catch (e: JSONException) {
                    viewHolder.title.text = tvShow[position].name
                    Glide.with(context)
                        .setDefaultRequestOptions(option)
                        .load(tvShow[position].path)
                        .into(viewHolder.poster)
                }
            },
            Response.ErrorListener {
                viewHolder.title.text = tvShow[position].name
                Glide.with(context)
                    .setDefaultRequestOptions(option)
                    .load(tvShow[position].path)
                    .into(viewHolder.poster)
            }
        )
        Volley.newRequestQueue(context).add(postRequest)
    }
}

class TvShowViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    var title: TextView = itemView.findViewById(R.id.title)
    var poster: ImageView = itemView.findViewById(R.id.iv_poster)
    var constraintLayout: ConstraintLayout = itemView.findViewById(R.id.constraintLayout)
//    var menu: ImageView = itemView.findViewById(R.id.menu)
//    var option: ConstraintLayout = itemView.findViewById(R.id.itemOptions)
}

