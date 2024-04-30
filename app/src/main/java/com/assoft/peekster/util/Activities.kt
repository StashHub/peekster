package com.assoft.peekster.util

import android.content.Intent

/**
 * An [android.app.Activity] that can be addressed by an intent.
 */
interface AddressableActivity<T> {
    val dynamicStart: T?
}

private const val PACKAGE_NAME = "com.assoft.peekster"

/**
 * All addressable activities.
 *
 * Can contain intent extra names or functions associated with the activity creation.
 */
object Activities {

    /** MovieDetailActivity */
    object MovieDetail : AddressableActivity<Intent> {
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.MovieDetailActivity".loadIntentOrNull()
    }

    /** TvShowDetailActivity */
    object TvShowDetail : AddressableActivity<Intent>{
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.TvShowDetailActivity".loadIntentOrNull()
    }

    /** MediaPlayerActivity */
    object MediaPlayer : AddressableActivity<Intent> {
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.vlc.MediaPlayerActivity".loadIntentOrNull()
    }

    object StreamMediaPlayer : AddressableActivity<Intent> {
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.vlc.StreamMediaPlayerActivity".loadIntentOrNull()
    }

    object TvPlayer : AddressableActivity<Intent> {
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.vlc.TvShowPlayerActivity".loadIntentOrNull()
    }

    /** AudioPlayerActivity */
    object AudioPlayer : AddressableActivity<Intent> {
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.vlc.AudioPlayerActivity".loadIntentOrNull()
    }

    /** CategoryActivity */
    object CategoryActivity : AddressableActivity<Intent> {
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.CategoryActivity".loadIntentOrNull()
    }

    /** AddCategoryActivity */
    object AddCategoryActivity : AddressableActivity<Intent> {
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.AddNewCategoryActivity".loadIntentOrNull()
    }

    object WelcomeActivity : AddressableActivity<Intent> {
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.WelcomeActivity".loadIntentOrNull()
    }

    object MainActivity : AddressableActivity<Intent> {
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.MainActivity".loadIntentOrNull()
    }

    object TransferActivity : AddressableActivity<Intent> {
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.shareable.ui.TransferActivity".loadIntentOrNull()
    }

    object StreamActivity : AddressableActivity<Intent> {
        override val dynamicStart: Intent?
            get() = "$PACKAGE_NAME.activity.shareable.stream.StreamActivity".loadIntentOrNull()
    }
}

private fun intentTo(className: String): Intent =
    Intent(Intent.ACTION_VIEW).setClassName(PACKAGE_NAME, className)

internal fun String.loadIntentOrNull(): Intent? =
    try {
        Class.forName(this).run { intentTo(this@loadIntentOrNull) }
    } catch (e: ClassNotFoundException) {
        null
    }