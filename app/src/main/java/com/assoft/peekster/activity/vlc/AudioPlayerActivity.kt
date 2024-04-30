package com.assoft.peekster.activity.vlc

import android.app.AlertDialog
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.os.Handler
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.assoft.peekster.R
import com.assoft.peekster.activity.Activity
import com.assoft.peekster.data.mediasource.Audio
import com.assoft.peekster.data.mediasource.GetAudioContent
import com.assoft.peekster.data.mediasource.MediaSource
import com.assoft.peekster.databinding.ActivityAudioPlayerBinding
import com.assoft.peekster.util.contentView
import com.assoft.peekster.util.ext.toast
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import timber.log.Timber
import java.util.*

class AudioPlayerActivity : Activity(), OnSeekBarChangeListener {

    /** Internal variable for obtaining the [ActivityAudioPlayerBinding] binding. */
    private val binding: ActivityAudioPlayerBinding by contentView(R.layout.activity_audio_player)

    /**
     * Internal reference to the [LibVLC] instance
     */
    private lateinit var libvlc: LibVLC

    /**
     * Internal reference to the [MediaPlayer] instance
     */
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var media: Media

    private lateinit var allAudio: List<Audio>

    private var selectedAudioId = 0

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
                Timber.i("Media player ended")
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.getParcelableExtra<Audio>("data")
        selectedAudioId = data?.id?.toInt()!!

        allAudio = MediaSource.withAudioContext(this)
            ?.getAllAudioContent(GetAudioContent.externalContentUri) as List<Audio>

        val playDrawable =
            getDrawable(R.drawable.avd_pause_play2) as AnimatedVectorDrawable?
        val pauseDrawable =
            getDrawable(R.drawable.avd_play_pause2) as AnimatedVectorDrawable?

        binding.apply {
            this.audio = data
            lifecycleOwner = this@AudioPlayerActivity

            seekBar.setOnSeekBarChangeListener(this@AudioPlayerActivity)

            playBtn.apply {
                setOnClickListener {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                        this.setImageDrawable(playDrawable)
                    } else {
                        mediaPlayer.play()
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
                previousSong()
            }

            next.setOnClickListener {
                nextSong()
            }
        }
        createAudioPlayer(data)
    }

    private fun nextSong() {
        if (selectedAudioId < allAudio.size - 1) {
            if (mediaPlayer.isPlaying) {
                releasePlayer()
                handler.removeCallbacks(progressHandler)
            }
            val nextAudio = allAudio[++selectedAudioId]
            binding.audio = nextAudio
            binding.executePendingBindings()
            createAudioPlayer(nextAudio)
        }
    }

    private fun previousSong() {
        if (selectedAudioId > 0) {
            if (mediaPlayer.isPlaying) {
                releasePlayer()
                handler.removeCallbacks(progressHandler)
            }
            val prevAudio = allAudio[--selectedAudioId]
            binding.audio = prevAudio
            binding.executePendingBindings()
            createAudioPlayer(prevAudio)
        }
    }

    override fun onPause() {
        super.onPause()
        if (userInteractionNavigation)
            return else userInteractionNavigation = false
        releasePlayer()
    }

    override fun onBackPressed() {
        releasePlayer()
        super.onBackPressed()
        userInteractionNavigation = true
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun createAudioPlayer(audio: Audio?) {
        try {
            // Create LibVLC
            val options = ArrayList<String>()
            options.add("--aout=opensles")
            options.add("--audio-time-stretch") // time stretching
            options.add("-vvv") // verbosity
            options.add("--http-reconnect")
            options.add("--network-caching=" + 6 * 1000)
            libvlc = LibVLC(this, options)

            // Keep screen on
            binding.root.keepScreenOn = true

            // Create media player
            mediaPlayer = MediaPlayer(libvlc)

            // Set event listener
            mediaPlayer.setEventListener(listener)

            media = Media(libvlc, audio?.path)
            mediaPlayer.media = media

            binding.seekBar.max = audio?.duration!! / 1000
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
            toast("Error creating Media Player")
            Timber.e(exception)
        }
    }

    private fun releasePlayer() {
        // Stop the media player
        mediaPlayer.stop()
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

    private fun endReached() {
        if (selectedAudioId < allAudio.size - 1) {
            nextSong()
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
}