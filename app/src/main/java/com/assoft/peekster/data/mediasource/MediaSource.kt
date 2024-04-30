package com.assoft.peekster.data.mediasource

import android.content.Context

object MediaSource {
    /**
     * Returns a static instance of [GetVideoContent]
     */
    fun withVideoContext(context: Context): GetVideoContent? {
        return GetVideoContent.getInstance(context)
    }

    /**
     * Returns a static instance of [GetTvShowContent]
     */
    fun withTvContext(context: Context): GetTvShowContent? {
        return GetTvShowContent.getInstance(context)
    }
    /**
     * Returns a static instance of [GetAudioContent]
     */
    fun withAudioContext(context: Context): GetAudioContent? {
        return GetAudioContent.getInstance(context)
    }
}