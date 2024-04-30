package com.assoft.peekster.activity.vlc

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.assoft.peekster.R
import com.assoft.peekster.activity.Activity
import com.assoft.peekster.data.mediasource.GetTvShowContent
import com.assoft.peekster.data.mediasource.MediaSource
import com.assoft.peekster.data.mediasource.TvShow
import com.assoft.peekster.databinding.ActivityTvShowPlayerBinding
import com.assoft.peekster.util.contentView
import com.assoft.peekster.util.ext.*
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout
import timber.log.Timber
import java.util.*

private const val AUTO_HIDE_DELAY = 3_000L

class TvShowPlayerActivity : Activity(), IVLCVout.Callback,
    SeekBar.OnSeekBarChangeListener {

    /** Internal variable for obtaining the [ActivityTvShowPlayerBinding] binding. */
    private val binding: ActivityTvShowPlayerBinding by contentView(R.layout.activity_tv_show_player)

    /**
     * Internal reference to the [LibVLC] instance
     */
    private lateinit var libvlc: LibVLC

    /**
     * Internal reference to the [MediaPlayer] instance
     */
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var media: Media

    private lateinit var allShows: List<TvShow>

    private var selectedTvShowId = 0

    private var seekForwardTime = 30000

    private var rewindTime = 10000

    private var isLocked = false

    private var shortAnimationDuration: Int = 0

    private var tvData: TvShow? = null

    /** Handler for the current thread used to update seek bar progress. */
    private val handler = Handler()

    private val progressHandler: Runnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 0)
        }
    }

    private val listener = MediaPlayer.EventListener { event ->
        when (event.type) {
            MediaPlayer.Event.EndReached -> {
                Timber.i("Media play ended")
                endReached()
            }
            MediaPlayer.Event.EncounteredError -> {
                Timber.i("Media player ended")
                encounteredError()
            }
        }
    }

    private fun updateProgress() {
        if (mediaPlayer.hasMedia() && mediaPlayer.isPlaying) {
            val currentDuration: Int = mediaPlayer.time.toInt()
            binding.seekBar.progress = currentDuration / 1000
            binding.timeCurrent.text = convertIntoTime(currentDuration)

            val totalDuration = resources.getString(
                R.string.total_duration,
                convertIntoTime((mediaPlayer.length - currentDuration).toInt())
            )
            binding.timeTotal.text = totalDuration
        }
    }

    /**
     * Called when the activity is first created. This is where
     * you should do all of your normal static set up: create
     * views, bind data to lists, etc.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Retrieve and cache the system's default "short" animation time.
        shortAnimationDuration = resources.getInteger(R.integer.motion_default_large)

        tvData = intent.getParcelableExtra("data")
        selectedTvShowId = intent.getIntExtra("position", 0)

        allShows = MediaSource.withTvContext(this)
            ?.getAllTvShowEpisodes(
                GetTvShowContent.externalContentUri,
                tvData?.name?.isTvShow()?.replaced()?.removeExtension().toString()
            ) as List<TvShow>

        val playDrawable =
            getDrawable(R.drawable.avd_pause_play2) as AnimatedVectorDrawable?
        val pauseDrawable =
            getDrawable(R.drawable.avd_play_pause2) as AnimatedVectorDrawable?

        binding.apply {
            this.tv = tvData
            lifecycleOwner = this@TvShowPlayerActivity

            seekBar.setOnSeekBarChangeListener(this@TvShowPlayerActivity)

            playBtn.apply {
                setOnClickListener {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                        keepScreenOn = false
                        this.setImageDrawable(playDrawable)
                    } else {
                        mediaPlayer.play()
                        keepScreenOn = true
                        this.setImageDrawable(pauseDrawable)
                    }

                    val drawable =
                        this.drawable.current

                    if (drawable is Animatable) {
                        (drawable as Animatable).start()
                    }
                }
            }

            prev.setOnClickListener {
                previousMovie()
            }

            next.setOnClickListener {
                nextMovie()
            }

            fastForward.setOnClickListener {
                val nextPosition = (mediaPlayer.time + seekForwardTime) / 1000
                val mediaLength = mediaPlayer.length / 1000
                if (nextPosition <= mediaLength) {
                    mediaPlayer.time += seekForwardTime
                }
            }

            rewind.setOnClickListener {
                val prevPosition = (mediaPlayer.time - rewindTime) / 1000
                if (prevPosition >= 0) {
                    mediaPlayer.time -= rewindTime
                }
            }

            close.setOnClickListener {
                onBackPressed()
            }

            backPressed.setOnClickListener {
                onBackPressed()
            }

            lock.setOnClickListener {
                lockEverything()
                isLocked = true
                unlock.visibility = View.VISIBLE
            }

            unlock.setOnClickListener {
                topLayerLayout.visibility = View.VISIBLE
                bottomLayerLayout.visibility = View.VISIBLE
                unlock.visibility = View.INVISIBLE
                isLocked = false
            }

            audioTrack.setOnClickListener {
                showAudioTracks()
            }

            closedCaption.setOnClickListener {
                showSubtitleTracks()
            }
        }
        autoHideController()
        hideSystemUI()
    }

    private fun nextMovie() {
        if (selectedTvShowId < allShows.size - 1) {
            if (mediaPlayer.isPlaying) {
                releasePlayer()
                handler.removeCallbacks(progressHandler)
            }
            releasePlayer()
            val nextShow = allShows[++selectedTvShowId]
            binding.tv = nextShow
            binding.executePendingBindings()
            createMediaPlayer(nextShow)
        }
    }

    private fun previousMovie() {
        if (selectedTvShowId > 0) {
            if (mediaPlayer.isPlaying) {
                releasePlayer()
                handler.removeCallbacks(progressHandler)
            }
            releasePlayer()
            val prevShow = allShows[--selectedTvShowId]
            binding.tv = prevShow
            binding.executePendingBindings()
            createMediaPlayer(prevShow)
        }
    }

    private fun lockEverything() {
        binding.topLayerLayout.visibility = View.INVISIBLE
        binding.bottomLayerLayout.visibility = View.INVISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun createMediaPlayer(tv: TvShow?) {
        try {
            // Create LibVLC
            val options = ArrayList<String>()
            options.add("--aout=opensles")
            options.add("--audio-time-stretch") // time stretching
            options.add("-vvv") // verbosity
            options.add("--http-reconnect")
            options.add("--network-caching=" + 6 * 1000)
            libvlc = LibVLC(this, options)

            // Create media player
            mediaPlayer = MediaPlayer(libvlc)

            // Set event listener
            mediaPlayer.setEventListener(listener)
            mediaPlayer.attachViews(binding.vlcLayout, null, false, false)
            mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_FIT_SCREEN

            media = Media(libvlc, tv?.path)
            mediaPlayer.media = media

            binding.seekBar.max = tv?.duration!! / 1000
            binding.timeTotal.text = convertIntoTime(mediaPlayer.length.toInt())

            // Play the media file
            mediaPlayer.play().also {
                val animatedVectorDrawable2 =
                    getDrawable(R.drawable.avd_play_pause2) as AnimatedVectorDrawable?
                binding.playBtn.setImageDrawable(animatedVectorDrawable2)

                val drawable =
                    binding.playBtn.drawable.current
                if (drawable is Animatable) {
                    (drawable as Animatable).start()
                }
                handler.postDelayed(progressHandler, 0)
            }
        } catch (exception: Exception) {
            toast("Error creating Media Player ${exception.message}")
            Timber.e(exception)
        }
    }

    private fun releasePlayer() {
        // Stop the media player
        mediaPlayer.stop()

        val vOut = mediaPlayer.vlcVout
        vOut.removeCallback(this)
        vOut.detachViews()
        libvlc.release()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            mediaPlayer.time = progress.toLong() * 1000
            val currentDuration: Int = mediaPlayer.time.toInt()
            binding.timeCurrent.text = convertIntoTime(currentDuration)
            val totalDuration = resources.getString(
                R.string.total_duration,
                convertIntoTime((mediaPlayer.length - currentDuration).toInt())
            )
            binding.timeTotal.text = totalDuration
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        handler.removeCallbacks(progressHandler)
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        handler.postDelayed(progressHandler, 0)
    }

    private fun autoHideController() {
        binding.apply {
            if (topLayerLayout.visibility == View.VISIBLE && bottomLayerLayout.visibility == View.VISIBLE) {
                handler.postDelayed({
                    topLayerLayout.animate().alpha(0.0f)
                        .setDuration(shortAnimationDuration.toLong())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                topLayerLayout.visibility = View.INVISIBLE
                            }
                        })
                    bottomLayerLayout.animate().alpha(0.0f)
                        .setDuration(shortAnimationDuration.toLong())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                bottomLayerLayout.visibility = View.INVISIBLE
                            }
                        })
                }, AUTO_HIDE_DELAY)
            }
        }
    }

    private fun endReached() {
        if (selectedTvShowId < allShows.size - 1) {
            nextMovie()
        } else {
            /* Exit player when reaching the end */
            finish()
        }
    }

    private fun encounteredError() {
        /* Encountered Error, exit player with a message */
        val dialog: AlertDialog = AlertDialog.Builder(this)
            .setPositiveButton(R.string.ok) { _, _ -> finish() }
            .setTitle(R.string.encountered_error_title)
            .setMessage(R.string.encountered_error_message)
            .create()
        dialog.show()
    }

    private fun convertIntoTime(ms: Int): String? {
        val seconds: Int
        val minutes: Int
        val hours: Int
        var x: Int = ms / 1000
        seconds = x % 60
        x /= 60
        minutes = x % 60
        x /= 60
        hours = x % 24
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onSurfacesCreated(vlcVout: IVLCVout?) {
    }

    override fun onSurfacesDestroyed(vlcVout: IVLCVout?) {
        releasePlayer()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLocked) {
            hideSystemUI()
            binding.apply {
                topLayerLayout.visibility = View.INVISIBLE
                bottomLayerLayout.visibility = View.INVISIBLE
                if (unlock.visibility == View.VISIBLE) {
                    handler.postDelayed({
                        unlock.animate().alpha(0.0f)
                            .setDuration(500L)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    unlock.visibility = View.INVISIBLE
                                }
                            })
                    }, 200L)
                } else {
                    handler.postDelayed({
                        unlock.animate().alpha(1.0f)
                            .setDuration(500L)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    unlock.visibility = View.VISIBLE
                                }
                            })
                    }, 200L)
                }
            }
        } else if (event.action == MotionEvent.ACTION_DOWN) {
            binding.apply {
                if (topLayerLayout.visibility == View.VISIBLE && bottomLayerLayout.visibility == View.VISIBLE) {
                    hideSystemUI()
                    handler.postDelayed({
                        topLayerLayout.animate().alpha(0.0f)
                            .setDuration(500L)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    topLayerLayout.visibility = View.INVISIBLE
                                }
                            })
                        bottomLayerLayout.animate().alpha(0.0f)
                            .setDuration(500L)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    bottomLayerLayout.visibility = View.INVISIBLE
                                }
                            })
                    }, 0)
                } else {
                    showSystemUI()
                    handler.postDelayed({
                        topLayerLayout.animate().alpha(1.0f)
                            .setDuration(0L)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    topLayerLayout.visibility = View.VISIBLE
                                }
                            })
                        bottomLayerLayout.animate().alpha(1.0f)
                            .setDuration(0L)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    bottomLayerLayout.visibility = View.VISIBLE
                                }
                            })
                    }, 0)
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun showSystemUI() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val decorView = window.decorView
        decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun showAudioTracks() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle("Audio Tracks")

        val layout = layoutInflater.inflate(R.layout.audio_tracks_layout, null)
        val radioGroup = layout.findViewById<RadioGroup>(R.id.radioGroup)

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(layout).create()
        val ad = builder.show()

        val trackInfo = mediaPlayer.audioTracks
        for ((index, value) in trackInfo.withIndex()) {
            if (value.name != "Disable") {
                val radioButton = RadioButton(this)
                radioButton.id = index
                // Set bottom padding to RadioButton
                radioButton.setPadding(16.dpToPx().toInt())
                radioButton.text = if (index == 0) "None" else value.name
                radioButton.setTextColor(ContextCompat.getColor(this, R.color.white))
                radioButton.isChecked = index == mediaPlayer.audioTrack

                // Add RadioButton to RadioGroup
                radioGroup.addView(radioButton)

                radioButton.setOnClickListener { v ->
                    (v.parent as RadioGroup).check(v.id)
                    mediaPlayer.audioTrack = v.id
                    ad.dismiss()
                }
            }
        }
    }

    private fun showSubtitleTracks() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        builder.setTitle("Subtitle Tracks")

        val layout = layoutInflater.inflate(R.layout.audio_tracks_layout, null)
        val radioGroup = layout.findViewById<RadioGroup>(R.id.radioGroup)

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(layout).create()
        val ad = builder.show()

        val trackInfo = mediaPlayer.spuTracks
        trackInfo?.let {
            for ((index, value) in trackInfo.withIndex()) {
                if (value.name != "Disable") {
                    val radioButton = RadioButton(this)
                    radioButton.id = index
                    // Set bottom padding to RadioButton
                    radioButton.setPadding(16.dpToPx().toInt())
                    radioButton.text = if (index == 0) "None" else value.name
                    radioButton.setTextColor(ContextCompat.getColor(this, R.color.white))
                    radioButton.isChecked = index == mediaPlayer.spuTrack

                    // Add RadioButton to RadioGroup
                    radioGroup.addView(radioButton)

                    radioButton.setOnClickListener { v ->
                        (v.parent as RadioGroup).check(v.id)
                        mediaPlayer.spuTrack = v.id
                        ad.dismiss()
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        releasePlayer()
        userInteractionNavigation = true
    }

    override fun onPause() {
        super.onPause()
        if (userInteractionNavigation)
            return else userInteractionNavigation = false

        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        tvData?.let {
            createMediaPlayer(it)
        }
        autoHideController()
        hideSystemUI()
    }
}