package com.assoft.peekster.util.ext

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.ListView
import androidx.annotation.Dimension
import androidx.annotation.Nullable
import java.util.*
import kotlin.math.roundToInt


fun String.replaced(): String {
    return this.replace(".", " ").replace("-", " ").replace("_", " ")
        .replace("\\s+".toRegex(), " ")
}

fun String.removeExtension(): String {
    return if (this.indexOf(".") > 0) {
        this.substring(0, this.lastIndexOf("."))
    } else {
        this
    }
}

fun String.language(): String {
    return if (this == "en") {
        "English"
    } else {
        "Unknown"
    }
}

fun Int.duration(): Long {
    val time: Long = this.toLong()
    return (time / 1000) / 60
}

fun String.format(): String {
    val regex = Regex("[×x]")
    val match = regex.find(this)
    match?.let {
        return when (this.substringAfter(it.value).toInt()) {
            in 0..144 -> "144p"
            in 145..240 -> "240p"
            in 241..360 -> "360p"
            in 361..480 -> "480p"
            in 481..720 -> "720p (HD)"
            in 721..1080 -> "1080p (HD)"
            in 1081..1440 -> "1440p (HD)"
            in 1441..2160 -> "2160p (4K)"
            in 2161..4320 -> "4320p (8K)"
            else -> "Unknown"
        }
    }
    return "Unknown"
}

fun String.removeYear(): String {
    val regex = Regex("20\\d{2}") // Years from 2000-2099
    val match = regex.find(this)

    match?.let {
        return match.range.first.let { this.substring(0, it) }
    }

    return this.replace(regex, "")
}

fun String.extractEpisodeName(): String {
    val regex = Regex("[sS]\\d{2}[eE]\\d{2}")
    val match = regex.find(this)
    match?.let {
        return this.substring(0, this.indexOf(it.value) + it.value.length)
    }
    return this
}

fun String.isTvShow(): String? {
    var name = this
    var season = false
    var episode = false
    var s = ""
    var e = ""
    name = name.replaced()
    name = name.toLowerCase(Locale.getDefault())
    for (i in 0..53) {
        val number: String = if (i < 10) {
            "0$i"
        } else {
            "" + i
        }
        if (name.contains("s$number")) {
            s = "s$number"
            season = true
        }
        if (name.contains("e$number")) {
            e = "e$number"
            episode = true
        }
        if (season && episode) {
            val index = name.indexOf(s + e)
            return name.substring(0, index).trim { it <= ' ' }
        }
    }
    return null
}

/** Utils **/
fun Context.dpToPx(dp: Int): Float {
    val displayMetrics = this.resources.displayMetrics
    return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt().toFloat()
}

@JvmOverloads
@Dimension(unit = Dimension.PX)
fun Number.dpToPx(
    metrics: DisplayMetrics = Resources.getSystem().displayMetrics
): Float {
    return toFloat() * metrics.density
}

/** Extension function to convert pixels to density independent */
@JvmOverloads
@Dimension(unit = Dimension.DP)
fun Number.pxToDp(
    metrics: DisplayMetrics = Resources.getSystem().displayMetrics
): Float {
    return toFloat() / metrics.density
}

fun resolveFileTypeIcon(
    ctx: Context,
    fileUri: Uri
): Drawable? {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(fileUri, getMimeType(ctx, fileUri))
    val pm = ctx.packageManager
    val matches = pm.queryIntentActivities(intent, 0)
    for (match in matches) {
        //final CharSequence label = match.loadLabel(pm);
        return match.loadIcon(pm)
    }
    return null //ContextCompat.getDrawable(ctx, R.drawable.ic_file);
}

fun getMimeType(ctx: Context, uri: Uri): String? {
    val mimeType: String?
    mimeType = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        val cr = ctx.applicationContext.contentResolver
        cr.getType(uri)
    } else {
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            fileExtension.toLowerCase(Locale.getDefault())
        )
    }
    return mimeType
}

fun View.hideKeyboard(context: Context?) {
    val inputMethodManager =
        context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
}

fun ensureVisible(@Nullable listView: ListView?, pos: Int) {
    if (listView?.adapter == null) {
        return
    }
    if (pos < 0 || pos >= listView.adapter.count) {
        return
    }
    val first: Int = listView.firstVisiblePosition
    val last: Int = listView.lastVisiblePosition
    if (pos < first) {
        listView.setSelection(pos)
        return
    }
    if (pos >= last) {
        listView.setSelection(1 + pos - (last - first))
    }
}