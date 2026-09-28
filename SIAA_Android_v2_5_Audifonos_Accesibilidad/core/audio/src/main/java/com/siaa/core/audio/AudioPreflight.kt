package com.siaa.core.audio

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/** Detects installed offline voices and candidate outputs; the user must confirm actual routing by listening. */
data class AudioPreflightReport(
    val privateOutput: Boolean,
    val spanishTts: Boolean,
    val englishTts: Boolean,
    val spanishLocale: String?,
    val englishLocale: String?,
    val outputTypes: List<Int>,
    val ready: Boolean,
    val message: String,
    val routeSignature: String = "",
    val audibleVolume: Boolean = false
)

class AudioPreflight(private val context: Context) {
    suspend fun run(requirePrivateOutput: Boolean = true): AudioPreflightReport {
        val manager = context.getSystemService(AudioManager::class.java)
        val outputs = manager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.map { it.type }.orEmpty()
        val routeBefore = AudioOutputGuard.routeSignature(context)
        val privateOutput = routeBefore.isNotBlank()
        val audible = (manager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) > 0
        val init = CompletableDeferred<Int>()
        val tts = TextToSpeech(context.applicationContext) { status -> if (!init.isCompleted) init.complete(status) }
        var es: Locale? = null
        var en: Locale? = null
        try {
            val status = withTimeoutOrNull(6_000L) { init.await() } ?: TextToSpeech.ERROR
            if (status == TextToSpeech.SUCCESS) {
                es = OfflineTtsVoices.choose(tts, listOf("es-PE","es-US","es-MX","es-ES","es"))?.locale
                en = OfflineTtsVoices.choose(tts, listOf("en-US","en-GB","en-AU","en"))?.locale
            }
        } finally { tts.shutdown() }
        val routeAfter = AudioOutputGuard.routeSignature(context)
        val stable = routeBefore == routeAfter
        val ready = (!requirePrivateOutput || privateOutput) && es != null && en != null && stable && audible
        val message = buildString {
            if (!privateOutput && requirePrivateOutput) append("Conecta audífonos. ")
            if (!audible) append("El volumen multimedia está en silencio. Ajústalo a un nivel cómodo. ")
            if (es == null) append("Descarga una voz sin conexión en español en los ajustes de texto a voz. ")
            if (en == null) append("Descarga una voz sin conexión en inglés. ")
            if (!stable) append("La salida de sonido cambió. Repite la comprobación. ")
            if (ready) append("Voces sin conexión detectadas. Escucha la prueba y confirma que sale por tus audífonos.")
        }.trim()
        return AudioPreflightReport(privateOutput,es!=null,en!=null,es?.toLanguageTag(),en?.toLanguageTag(),outputs,ready,message,routeAfter,audible)
    }
}
