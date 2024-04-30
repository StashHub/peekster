package com.assoft.peekster.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.assoft.peekster.domain.TvShow
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.IgnoredOnParcel

/**
 * This class represents [DatabaseTvShow] data.
 *
 * The [ColumnInfo] name is explicitly declared to allow flexibility for renaming the data class
 * properties without requiring changing the column name.
 */
@Entity(tableName = "tv")
data class DatabaseTvShow(

    @ColumnInfo(name = "title")
    @SerializedName("title")
    val title: String?,

    @ColumnInfo(name = "popularity")
    @SerializedName("popularity")
    val popularity: String?,

    @ColumnInfo(name = "vote_count")
    @SerializedName("vote_count")
    val vote_count: String?,

    @ColumnInfo(name = "poster_path")
    @SerializedName("poster_path")
    val poster_path: String?,

    @ColumnInfo(name = "backdrop_path")
    @SerializedName("backdrop_path")
    val backdrop_path: String?,

    @ColumnInfo(name = "original_language")
    @SerializedName("original_language")
    val original_language: String?,

    @ColumnInfo(name = "genres")
    @SerializedName("genres")
    val genres: String?,

    @ColumnInfo(name = "vote_average")
    @SerializedName("vote_average")
    val vote_average: String?,

    @ColumnInfo(name = "overview")
    @SerializedName("overview")
    val overview: String?,

    @ColumnInfo(name = "release_date")
    @SerializedName("release_date")
    val release_date: String?,

    @ColumnInfo(name = "country")
    @SerializedName("country")
    val country: String?,

    @ColumnInfo(name = "file_path")
    @SerializedName("file_path")
    val file_path: String?,

    @ColumnInfo(name = "file_name")
    @SerializedName("file_name")
    val file_name: String?
){
    @IgnoredOnParcel
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L
}

fun DatabaseTvShow.asDomainModel(): TvShow? {
    return TvShow(
        country = country,
        title = title,
        popularity = popularity,
        vote_count = vote_count,
        poster_path = poster_path,
        backdrop_path = backdrop_path,
        original_language = original_language,
        genres = genres,
        vote_average = vote_average,
        overview = overview,
        release_date = release_date,
        filepath = file_path,
        file_name = file_name
    )
}

fun TvShow.asDatabaseModel() : DatabaseTvShow{
    return DatabaseTvShow(
        country = country,
        title = title,
        popularity = popularity,
        vote_count = vote_count,
        poster_path = poster_path,
        backdrop_path = backdrop_path,
        original_language = original_language,
        genres = genres,
        vote_average = vote_average,
        overview = overview,
        release_date = release_date,
        file_path = filepath,
        file_name = file_name
    )
}