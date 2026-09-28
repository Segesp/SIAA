package com.siaa.core.audio

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/** No silent fallback to network synthesis for a phone used offline on a bus. */
internal object OfflineTtsVoices {
    fun choose(tts: TextToSpeech, tags: List<String>): Voice? {
        val voices = tts.voices.orEmpty().filter {
            !it.isNetworkConnectionRequired &&
                !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }
        for (tag in tags.distinct()) {
            val locale = Locale.forLanguageTag(tag)
            voices.filter { it.locale.toLanguageTag().equals(locale.toLanguageTag(), true) }
                .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.name })
                .firstOrNull()?.let { return it }
        }
        val language = Locale.forLanguageTag(tags.first()).language
        return voices.filter { it.locale.language == language }
            .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.name }).firstOrNull()
    }
}
