package com.siaa.app.media

import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import com.siaa.core.model.DeviceProfile
import com.siaa.core.runtime.MediaControlEvent

class EarbudCommandRouter {

    @Suppress("DEPRECATION")
    fun extractKeyEvent(intent: Intent): KeyEvent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        }
    }

    fun rawKeyCode(intent: Intent): Int? {
        val event = extractKeyEvent(intent) ?: return null
        if (event.action != KeyEvent.ACTION_DOWN) return null
        if (event.repeatCount > 0) return null
        return event.keyCode
    }

    fun fromMediaButtonIntent(
        intent: Intent,
        profile: DeviceProfile? = null
    ): Pair<MediaControlEvent, KeyEvent>? {
        val event = extractKeyEvent(intent) ?: return null
        if (event.action != KeyEvent.ACTION_DOWN) return null
        if (event.repeatCount > 0) return null

        val keyCode = event.keyCode

        // Security check: Stop and Pause must never be converted into answer inputs
        if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            return Pair(MediaControlEvent.STOP, event)
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            return Pair(MediaControlEvent.PAUSE, event)
        }

        if (profile != null) {
            if (keyCode == profile.stopKeyCode) {
                return Pair(MediaControlEvent.STOP, event)
            }
            if (profile.playPauseAvailable && keyCode == profile.primaryKeyCode) {
                return Pair(MediaControlEvent.PLAY_PAUSE, event)
            }
            if (profile.nextAvailable && keyCode == profile.secondaryKeyCode) {
                return Pair(MediaControlEvent.NEXT, event)
            }
            if (profile.previousAvailable && keyCode == profile.backKeyCode) {
                return Pair(MediaControlEvent.PREVIOUS, event)
            }
            return null
        }

        // Default firmware key mapping
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY ->
                Pair(MediaControlEvent.PLAY_PAUSE, event)
            KeyEvent.KEYCODE_MEDIA_NEXT ->
                Pair(MediaControlEvent.NEXT, event)
            KeyEvent.KEYCODE_MEDIA_PREVIOUS ->
                Pair(MediaControlEvent.PREVIOUS, event)
            KeyEvent.KEYCODE_MEDIA_STOP ->
                Pair(MediaControlEvent.STOP, event)
            KeyEvent.KEYCODE_MEDIA_PAUSE ->
                Pair(MediaControlEvent.PAUSE, event)
            else -> null
        }
    }
}
