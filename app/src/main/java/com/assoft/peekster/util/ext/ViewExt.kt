package com.assoft.peekster.util.ext

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.assoft.peekster.R
import com.assoft.peekster.data.mediasource.Audio
import com.assoft.peekster.data.mediasource.TvShow
import com.assoft.peekster.data.mediasource.Video
import com.assoft.peekster.nav.BottomChildDrawerFragment
import com.assoft.peekster.activity.MediaViewModel
import com.assoft.peekster.database.entities.Category
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BaseTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("previousFullscreen", "layoutFullscreen")
fun View.bindLayoutFullscreen(previousFullscreen: Boolean, fullscreen: Boolean) {
    if (previousFullscreen != fullscreen && fullscreen) {
        systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}

const val ANIMATION_FAST_MILLIS = 1000L
fun FloatingActionButton.simulateClick(delay: Long = ANIMATION_FAST_MILLIS) {
    performClick()
    isPressed = true
    invalidate()
    postDelayed({
        invalidate()
        isPressed = false
    }, delay)
}

fun Toolbar.setupToolbarMenuItem(
    bottomChildDrawerFragment: BottomChildDrawerFragment,
    lifecycleOwner: LifecycleOwner,
    viewModel: MediaViewModel
) {
    inflateMenu(R.menu.toolbar_menu)
    val lockItem = menu.findItem(R.id.action_parental_control) ?: return
    lockItem.setOnMenuItemClickListener {
        if (viewModel.isChileModeEnabled()) viewModel.setParentalControl() else
            bottomChildDrawerFragment.toggle()
        true
    }

    val iconSize = resources.getDimensionPixelSize(R.dimen.grid_4)
    val target = lockItem.asGlideTarget(iconSize)
    viewModel.parentControlData.observe(lifecycleOwner, Observer {
        setChildLockItem(context, target, it)
    })
}

fun setChildLockItem(
    context: Context,
    target: Target<Drawable>,
    boolean: Boolean,
    placeholder: Int = R.drawable.child_icon
) {
    // Inflate the drawable for proper tinting
    val placeholderDrawable = AppCompatResources.getDrawable(context, placeholder)
    when (boolean) {
        false -> {
            Glide.with(context)
                .load(placeholderDrawable)
                .apply(RequestOptions.circleCropTransform())
                .into(target)
        }
        true -> {
            Glide.with(context)
                .load(R.drawable.child_icon_yellow)
                .apply(RequestOptions.placeholderOf(placeholderDrawable).circleCrop())
                .into(target)
        }
    }
}

fun MenuItem.asGlideTarget(size: Int): Target<Drawable> = object : BaseTarget<Drawable>() {
    override fun getSize(cb: SizeReadyCallback) {
        cb.onSizeReady(size, size)
    }

    override fun removeCallback(cb: SizeReadyCallback) {}
    override fun onLoadStarted(placeholder: Drawable?) {
        icon = placeholder
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        icon = errorDrawable
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        icon = placeholder
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        icon = resource
    }
}

/**
 * Retrieve a color from the current [android.content.res.Resources.Theme].
 */
@ColorInt
@SuppressLint("Recycle")
fun Context.themeColor(
    @AttrRes themeAttrId: Int
): Int {
    return obtainStyledAttributes(
        intArrayOf(themeAttrId)
    ).use {
        it.getColor(0, Color.RED)
    }
}

/**
 * Executes the given [block] function on this TypedArray and then recycles it.
 *
 * @see kotlin.io.use
 */
inline fun <R> TypedArray.use(block: (TypedArray) -> R): R {
    return block(this).also {
        recycle()
    }
}

/**
 * Coerce the receiving Float between inputMin and inputMax and linearly interpolate to the
 * outputMin to outputMax scale. This function is able to handle ranges which span negative and
 * positive numbers.
 *
 * This differs from [lerp] as the input values are not required to be between 0 and 1.
 */
fun Float.normalize(
    inputMin: Float,
    inputMax: Float,
    outputMin: Float,
    outputMax: Float
): Float {
    if (this < inputMin) {
        return outputMin
    } else if (this > inputMax) {
        return outputMax
    }

    return outputMin * (1 - (this - inputMin) / (inputMax - inputMin)) +
            outputMax * ((this - inputMin) / (inputMax - inputMin))
}

/** Extension function to show toast for Context. */
fun Context?.toast(text: CharSequence, duration: Int = Toast.LENGTH_LONG) =
    this?.let { Toast.makeText(it, text, duration).show() }

@BindingAdapter("goneUnless")
fun goneUnless(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

/** Extension function that starts activity with intent receiver, flag to close calling activity
 * and extras option to pass to new activity */
fun Activity.startActivity(
    intent: Intent, finish: Boolean = false,
    extras: Map<String, Any?>? = null
) {
    extras?.forEach { intent.addExtra(it.key, it.value) }
    startActivity(intent)
    if (finish) finish()
}

/** Extension function to add extra key/value data to [Intent] */
internal fun Intent.addExtra(key: String, value: Any?) {
    when (value) {
        is Long -> putExtra(key, value)
        is String -> putExtra(key, value)
        is Boolean -> putExtra(key, value)
        is Float -> putExtra(key, value)
        is Double -> putExtra(key, value)
        is Int -> putExtra(key, value)
        is Parcelable -> putExtra(key, value)
        //Add other types when needed
    }
}

@BindingAdapter("duration")
fun videoDuration(view: TextView, video: Video) {
    val time: Long = video.duration.toLong()
    val minutes = (time / 1000) / 60

    val duration = view.resources.getString(
        R.string.video_duration,
        minutes
    )
    view.text = duration
}

@BindingAdapter("tvDuration")
fun tvShowDuration(view: TextView, tvShow: TvShow) {
    val time: Long = tvShow.duration.toLong()
    val minutes = (time / 1000) / 60

    val duration = view.resources.getString(
        R.string.video_duration,
        minutes
    )
    view.text = duration
}

@BindingAdapter("duration")
fun audioDuration(view: TextView, audio: Audio) {
    val time: Long = audio.duration.toLong()
    val minutes = (time / 1000) / 60
    val seconds = (time / 1000) % 60

    val duration = view.resources.getString(
        R.string.audio_duration,
        minutes, seconds
    )
    view.text = duration
}

@BindingAdapter("format")
fun videoFormat(view: TextView, resolution: String) {
    val format = when {
        resolution.contains("x144") -> "144p"
        resolution.contains("x240") -> "240p"
        resolution.contains("×360") -> "360p"
        resolution.contains("x480") -> "480p"
        resolution.contains("×720") -> "720p (HD)"
        resolution.contains("×1080") -> "1080p (HD)"
        resolution.contains("×1440") -> "1440p (HD)"
        resolution.contains("×2160") -> "2160p (4K)"
        resolution.contains("×4320") -> "4320p (8K)"
        else -> "Unknown"
    }
    view.text = format
}

@BindingAdapter("language")
fun videoLanguage(view: TextView, language: String?) {
    view.text = language?.let {
        language
    } ?: "Unknown"
}

@BindingAdapter("toolbarTitle")
fun toolBarTitle(toolbar: Toolbar, category: Category) {
    val title = toolbar.resources.getString(
        R.string.toolbar_title,
        category.name, category.type
    )
    toolbar.title = title
}

/** Extension method to add text changed listener for EditText. */
inline fun EditText.afterTextChanged(crossinline afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            afterTextChanged.invoke(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

/** Extension method to validate a entry to return a error, else return a message. */
inline fun TextInputLayout.validate(crossinline validator: (String) -> Boolean, message: String) {
    this.editText?.afterTextChanged {
        this.error = if (validator(it)) {
            isErrorEnabled = false
            ""
        } else message
    }
    this.error = if (validator(this.editText?.text.toString())) {
        isErrorEnabled = false
        ""
    } else message
}

/** Extension function to add [TextWatcher] to the EditText */
inline fun EditText.onTextChanged(crossinline listener: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            listener(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

@BindingAdapter(
    "marginLeftSystemWindowInsets",
    "marginTopSystemWindowInsets",
    "marginRightSystemWindowInsets",
    "marginBottomSystemWindowInsets",
    requireAll = false
)
fun View.applySystemWindowInsetsMargin(
    previousApplyLeft: Boolean,
    previousApplyTop: Boolean,
    previousApplyRight: Boolean,
    previousApplyBottom: Boolean,
    applyLeft: Boolean,
    applyTop: Boolean,
    applyRight: Boolean,
    applyBottom: Boolean
) {
    if (previousApplyLeft == applyLeft &&
        previousApplyTop == applyTop &&
        previousApplyRight == applyRight &&
        previousApplyBottom == applyBottom
    ) {
        return
    }

    doOnApplyWindowInsets { view, insets, _, margin, _ ->
        val left = if (applyLeft) insets.systemWindowInsetLeft else 0
        val top = if (applyTop) insets.systemWindowInsetTop else 0
        val right = if (applyRight) insets.systemWindowInsetRight else 0
        val bottom = if (applyBottom) insets.systemWindowInsetBottom else 0

        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = margin.left + left
            topMargin = margin.top + top
            rightMargin = margin.right + right
            bottomMargin = margin.bottom + bottom
        }
    }
}

@BindingAdapter(
    "paddingLeftSystemWindowInsets",
    "paddingTopSystemWindowInsets",
    "paddingRightSystemWindowInsets",
    "paddingBottomSystemWindowInsets",
    requireAll = false
)
fun View.applySystemWindowInsetsPadding(
    previousApplyLeft: Boolean,
    previousApplyTop: Boolean,
    previousApplyRight: Boolean,
    previousApplyBottom: Boolean,
    applyLeft: Boolean,
    applyTop: Boolean,
    applyRight: Boolean,
    applyBottom: Boolean
) {
    if (previousApplyLeft == applyLeft &&
        previousApplyTop == applyTop &&
        previousApplyRight == applyRight &&
        previousApplyBottom == applyBottom
    ) {
        return
    }

    doOnApplyWindowInsets { view, insets, padding, _, _ ->
        val left = if (applyLeft) insets.systemWindowInsetLeft else 0
        val top = if (applyTop) insets.systemWindowInsetTop else 0
        val right = if (applyRight) insets.systemWindowInsetRight else 0
        val bottom = if (applyBottom) insets.systemWindowInsetBottom else 0

        view.setPadding(
            padding.left + left,
            padding.top + top,
            padding.right + right,
            padding.bottom + bottom
        )
    }
}

fun View.doOnApplyWindowInsets(
    block: (View, WindowInsets, InitialPadding, InitialMargin, Int) -> Unit
) {
    // Create a snapshot of the view's padding & margin states
    val initialPadding = recordInitialPaddingForView(this)
    val initialMargin = recordInitialMarginForView(this)
    val initialHeight = recordInitialHeightForView(this)
    // Set an actual OnApplyWindowInsetsListener which proxies to the given
    // lambda, also passing in the original padding & margin states
    setOnApplyWindowInsetsListener { v, insets ->
        block(v, insets, initialPadding, initialMargin, initialHeight)
        // Always return the insets, so that children can also use them
        insets
    }
    // request some insets
    requestApplyInsetsWhenAttached()
}

class InitialPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

class InitialMargin(val left: Int, val top: Int, val right: Int, val bottom: Int)

private fun recordInitialPaddingForView(view: View) = InitialPadding(
    view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom
)

private fun recordInitialMarginForView(view: View): InitialMargin {
    val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
        ?: throw IllegalArgumentException("Invalid view layout params")
    return InitialMargin(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
}

private fun recordInitialHeightForView(view: View): Int {
    return view.layoutParams.height
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        // We're already attached, just request as normal
        requestApplyInsets()
    } else {
        // We're not attached to the hierarchy, add a listener to
        // request when we are
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}
