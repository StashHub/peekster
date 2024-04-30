package com.assoft.peekster.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.assoft.peekster.domain.HttpServer
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.IgnoredOnParcel

@Entity(tableName = "server")
data class DatabaseHttpServer(

    @ColumnInfo(name = "port")
    @SerializedName("port")
    val port: Int?,

    @ColumnInfo(name = "data")
    @SerializedName("data")
    val data: String?,

    @ColumnInfo(name = "url")
    @SerializedName("url")
    val url: String?
) {
    @IgnoredOnParcel
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L
}

fun DatabaseHttpServer.asDomainModel(): HttpServer? {
    return HttpServer(
        port = port,
        data = data,
        url = url
    )
}

fun HttpServer.asDatabaseModel(): DatabaseHttpServer {
    return DatabaseHttpServer(
        port = port,
        data = data,
        url = url
    )
}