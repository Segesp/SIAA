package com.siaa.app.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.accessibility.AccessibilityManager
import com.siaa.core.model.AudioRouteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AudioPlayerManager(
    private val context: Context,
    private val onPlaybackStateChanged: (Boolean) -> Unit = {},
    private val onBecomingNoisy: () -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _currentRoute = MutableStateFlow(detectAudioRoute())
    val currentRoute: StateFlow<AudioRouteType> = _currentRoute.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var focusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
                onBecomingNoisy()
                refreshAudioRoute()
            }
        }
    }

    init {
        tts = TextToSpeech(context.applicationContext, this)
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        context.registerReceiver(noisyReceiver, filter)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("es", "ES")
            isTtsReady = true
        }
    }

    fun isTouchExplorationActive(): Boolean {
        return accessibilityManager.isTouchExplorationEnabled
    }

    fun detectAudioRoute(): AudioRouteType {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET -> return AudioRouteType.HEADPHONES_BLUETOOTH

                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> return AudioRouteType.HEADPHONES_WIRED
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (audioManager.isBluetoothA2dpOn) return AudioRouteType.HEADPHONES_BLUETOOTH
            @Suppress("DEPRECATION")
            if (audioManager.isWiredHeadsetOn) return AudioRouteType.HEADPHONES_WIRED
        }
        return AudioRouteType.DEVICE_SPEAKER
    }

    fun refreshAudioRoute() {
        _currentRoute.value = detectAudioRoute()
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            focusRequest = req
            val res = audioManager.requestAudioFocus(req)
            hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            hasAudioFocus
        } else {
            @Suppress("DEPRECATION")
            val res = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            hasAudioFocus
        }
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
        hasAudioFocus = false
    }

    fun playAssetAudio(
        relPath: String,
        onCompletion: () -> Unit = {}
    ) {
        if (!requestAudioFocus()) return

        stopTts()
        mediaPlayer?.release()

        try {
            val afd = context.assets.openFd(relPath)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnCompletionListener {
                    _isPlaying.value = false
                    onPlaybackStateChanged(false)
                    onCompletion()
                }
                prepare()
                start()
                _isPlaying.value = true
                onPlaybackStateChanged(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
            onPlaybackStateChanged(false)
            onCompletion()
        }
    }

    fun speakText(
        text: String,
        isEnglish: Boolean = false,
        speechRate: Float = 1.0f,
        onCompletion: () -> Unit = {}
    ) {
        if (!requestAudioFocus()) return
        if (!isTtsReady || tts == null) {
            onCompletion()
            return
        }

        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
        }

        val utteranceId = "siaa_tts_${System.currentTimeMillis()}"
        tts?.language = if (isEnglish) Locale.US else Locale("es", "ES")
        tts?.setSpeechRate(speechRate)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                _isPlaying.value = true
                onPlaybackStateChanged(true)
            }

            override fun onDone(id: String?) {
                _isPlaying.value = false
                onPlaybackStateChanged(false)
                onCompletion()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                _isPlaying.value = false
                onPlaybackStateChanged(false)
                onCompletion()
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
        stopTts()
        _isPlaying.value = false
        onPlaybackStateChanged(false)
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopTts()
        abandonAudioFocus()
        _isPlaying.value = false
        onPlaybackStateChanged(false)
    }

    private fun stopTts() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
    }

    fun release() {
        try {
            context.unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.shutdown()
        tts = null
        abandonAudioFocus()
    }
}
