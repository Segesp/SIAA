package com.siaa.core.model

import android.view.KeyEvent

data class DeviceProfile(
    val id: Long = 1L,
    val name: String = "Audífonos Predeterminados",
    val primaryKeyCode: Int = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
    val secondaryKeyCode: Int = KeyEvent.KEYCODE_MEDIA_NEXT,
    val backKeyCode: Int = KeyEvent.KEYCODE_MEDIA_PREVIOUS,
    val stopKeyCode: Int = KeyEvent.KEYCODE_MEDIA_STOP,
    val playPauseAvailable: Boolean = true,
    val nextAvailable: Boolean = true,
    val previousAvailable: Boolean = true,
    val singleButtonMode: Boolean = false,
    val silenceTimeoutSeconds: Int = 12,
    val autoRepeatOptions: Boolean = true,
    val voiceSpeed: Float = 1.0f
)
