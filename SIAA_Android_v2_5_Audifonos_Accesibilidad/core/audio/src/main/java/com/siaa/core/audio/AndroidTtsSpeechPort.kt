package com.siaa.core.audio

import android.content.Context
import android.os.Bundle
import android.media.AudioAttributes
import kotlinx.coroutines.withTimeout
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.siaa.core.runtime.SpeechPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TtsLanguageUnavailableException(languageTag: String) : IllegalStateException("TTS language not available: $languageTag")
class TtsPlaybackException(message: String) : RuntimeException(message)

class AndroidTtsSpeechPort(context: Context) : SpeechPort, TextToSpeech.OnInitListener {
    private val ready = CompletableDeferred<Unit>()
    private val pending = ConcurrentHashMap<String, CancellableContinuation<Unit>>()
    private val tts = TextToSpeech(context.applicationContext, this)

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                utteranceId ?: return
                val cont = pending.remove(utteranceId)
                if (cont != null && cont.isActive) cont.resume(Unit)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                utteranceId ?: return
                val cont = pending.remove(utteranceId)
                if (cont != null && cont.isActive) {
                    cont.resumeWithException(TtsPlaybackException("TTS playback error for utterance $utteranceId"))
                }
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                utteranceId ?: return
                val cont = pending.remove(utteranceId)
                if (cont != null && cont.isActive) {
                    cont.resumeWithException(TtsPlaybackException("TTS playback error ($errorCode) for utterance $utteranceId"))
                }
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            tts.setSpeechRate(0.94f)
            if (!ready.isCompleted) ready.complete(Unit)
        } else if (!ready.isCompleted) {
            ready.completeExceptionally(IllegalStateException("TextToSpeech init failed: $status"))
        }
    }

    override suspend fun speak(text: String, languageTag: String, rate: Float) {
        if (text.isBlank()) return
        withTimeout(8_000L) { ready.await() }
        tts.setSpeechRate((0.94f * rate).coerceIn(0.55f, 1.25f))
        val voice = resolveVoice(languageTag) ?: throw TtsLanguageUnavailableException(languageTag)
        val langResult = tts.setVoice(voice)
        if (langResult == TextToSpeech.ERROR) {
            throw TtsLanguageUnavailableException(languageTag)
        }
        val id = UUID.randomUUID().toString()
        withTimeout((text.length * 160L + 15_000L).coerceIn(20_000L, 180_000L)) {
        suspendCancellableCoroutine<Unit> { cont ->
            pending[id] = cont
            cont.invokeOnCancellation {
                pending.remove(id)
                tts.stop()
            }
            val result = tts.speak(text, TextToSpeech.QUEUE_ADD, Bundle(), id)
            if (result == TextToSpeech.ERROR) {
                pending.remove(id)
                if (cont.isActive) {
                    cont.resumeWithException(TtsPlaybackException("TTS speak returned ERROR for utterance $id"))
                }
            }
        }
    }

    }

    private fun resolveVoice(languageTag: String): android.speech.tts.Voice? {
        val requested = Locale.forLanguageTag(languageTag)
        val fallbacks = when (requested.language.lowercase(Locale.ROOT)) {
            "es" -> listOf(languageTag, "es-PE", "es-US", "es-MX", "es-ES", "es")
            "en" -> listOf(languageTag, "en-US", "en-GB", "en-AU", "en")
            else -> listOf(languageTag)
        }.distinct()
        return OfflineTtsVoices.choose(tts, fallbacks)
    }

    override fun stop() {
        tts.stop()
        val toCancel = pending.values.toList()
        pending.clear()
        toCancel.forEach { cont ->
            if (cont.isActive) {
                cont.cancel(CancellationException("TTS stopped"))
            }
        }
    }

    override fun shutdown() {
        stop()
        tts.shutdown()
    }
}

