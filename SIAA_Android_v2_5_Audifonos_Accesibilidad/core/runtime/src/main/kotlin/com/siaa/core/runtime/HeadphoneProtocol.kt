package com.siaa.core.runtime

import com.siaa.core.model.*

data class SpokenChoice(val id: String, val label: String)

/** One protocol supplies both spoken help and visible instructions. */
object HeadphoneProtocol {
    fun choices(selfAssessment: Boolean): List<SpokenChoice> =
        (if (selfAssessment) listOf(
            SpokenChoice("YES", "Sí, lo resolví"), SpokenChoice("UNSURE", "Tuve dudas"),
            SpokenChoice("NO", "No lo resolví")
        ) else listOf(SpokenChoice("A", "Opción A"), SpokenChoice("B", "Opción B"))) + listOf(
            SpokenChoice("REPEAT", "Repetir la pregunta"), SpokenChoice("SLOWER", "Escuchar más despacio"),
            SpokenChoice("PAUSE", "Pausar la sesión")
        )

    fun hint(selfAssessment: Boolean, capabilities: SessionCapabilities, prefs: HeadphonePreferences): String {
        val p = prefs.normalized()
        if (capabilities.singleSwitchScanning) return "Leeré las opciones por turnos. Después de cada opción, usa ${p.primaryGesture} durante el silencio para elegirla. Si no haces nada, escucharás la siguiente."
        return if (selfAssessment) "${p.primaryGesture} para sí. ${p.secondaryGesture} para tuve dudas. ${p.backGesture} para no."
        else buildString {
            append("${p.primaryGesture} para A. ${p.secondaryGesture} para B.")
            if (capabilities.hasBack) append(" ${p.backGesture} para repetir.")
        }
    }
}

/** Filters key repeats and duplicate delivery; never guesses single/double/triple taps. */
class MediaInputGate(private val debounceMs: Long = 280L) {
    private var lastCode: Int? = null
    private var lastAt: Long = Long.MIN_VALUE
    @Synchronized fun accept(code: Int, down: Boolean, repeatCount: Int, elapsedMs: Long): Boolean {
        if (!down || repeatCount != 0) return false
        if (lastCode == code && lastAt != Long.MIN_VALUE && elapsedMs - lastAt in 0 until debounceMs) return false
        lastCode = code; lastAt = elapsedMs
        return true
    }
    @Synchronized fun reset() { lastCode = null; lastAt = Long.MIN_VALUE }
}
