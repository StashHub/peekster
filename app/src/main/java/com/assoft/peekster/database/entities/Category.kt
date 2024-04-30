package com.assoft.peekster.database.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

/**
 * This class represents [Category] data.
 *
 * The [ColumnInfo] name is explicitly declared to allow flexibility for renaming the data class
 * properties without requiring changing the column name.
 */
@Entity(tableName = "categories")
@Parcelize
data class Category(

    @ColumnInfo(name = "name")
    @SerializedName("name")
    var name: String,

    @ColumnInfo(name = "type")
    @SerializedName("type")
    var type: String,

    @ColumnInfo(name = "path")
    @SerializedName("path")
    var path: String,

    @ColumnInfo(name = "folder")
    @SerializedName("folder")
    var folder: String,

    @ColumnInfo(name = "default")
    @SerializedName("default")
    var fixed: Boolean

) : Parcelable {
    @IgnoredOnParcel
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L
}