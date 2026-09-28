package com.siaa.app.data

import android.content.Context
import com.siaa.core.model.AudioExercise
import com.siaa.core.model.ExerciseOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader

class ExerciseRepository(private val context: Context) {

    private val allExercises = mutableListOf<AudioExercise>()
    private var isLoaded = false

    suspend fun loadExercisesIfNeeded() = withContext(Dispatchers.IO) {
        if (isLoaded && allExercises.isNotEmpty()) return@withContext

        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("audio/audio_index.json")
            val jsonText = InputStreamReader(inputStream).use { it.readText() }
            val root = JSONObject(jsonText)
            val entries = root.getJSONObject("entries")

            val keys = entries.keys()
            val rawEntries = mutableListOf<RawEntry>()

            while (keys.hasNext()) {
                val key = keys.next()
                val obj = entries.getJSONObject(key)
                val text = obj.optString("text", key)
                val kind = obj.optString("kind", "listening")
                val level = obj.optString("level", "A1").uppercase()
                val rel = obj.optString("rel", "")
                if (rel.isNotEmpty()) {
                    rawEntries.add(RawEntry(key, text, kind, level, rel))
                }
            }

            // Group entries to generate contextual options
            val groupedByLevel = rawEntries.groupBy { it.level }

            val generated = rawEntries.mapIndexed { index, entry ->
                val candidates = (groupedByLevel[entry.level] ?: rawEntries)
                    .filter { it.text != entry.text }
                    .shuffled()
                    .take(2)

                val optionTexts = mutableListOf<String>()
                optionTexts.add(entry.text)
                if (candidates.size >= 2) {
                    optionTexts.add(candidates[0].text)
                    optionTexts.add(candidates[1].text)
                } else {
                    optionTexts.add("No se especificó en el audio")
                    optionTexts.add("Información alternativa no mencionada")
                }
                optionTexts.shuffle()

                val correctIdx = optionTexts.indexOf(entry.text)

                val prompt = when (entry.kind) {
                    "phonology" -> "¿Qué frase o sonido se pronunció exactamente?"
                    "phrase" -> "¿Qué expresión o fórmula comunicativa se escuchó?"
                    else -> "¿Qué enunciado o idea clave se escuchó en el audio?"
                }

                val options = optionTexts.mapIndexed { optIdx, txt ->
                    val badge = when (optIdx) {
                        0 -> "1"
                        1 -> "2"
                        else -> "3"
                    }
                    val hint = when (optIdx) {
                        0 -> "1 toque / Centro"
                        1 -> "2 toques / Siguiente"
                        else -> "3 toques / Anterior"
                    }
                    ExerciseOption(
                        index = optIdx,
                        keyBadge = badge,
                        text = txt,
                        earbudHint = hint
                    )
                }

                AudioExercise(
                    id = "ex_$index",
                    text = entry.text,
                    kind = entry.kind,
                    level = entry.level,
                    audioRelPath = entry.rel,
                    questionPrompt = prompt,
                    options = options,
                    correctIndex = correctIdx,
                    explanation = "La respuesta correcta es: \"${entry.text}\""
                )
            }

            allExercises.clear()
            allExercises.addAll(generated)
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            // Provide fallback exercises if asset reading fails
            allExercises.addAll(getFallbackExercises())
            isLoaded = true
        }
    }

    suspend fun getExercises(
        level: String,
        modality: String,
        count: Int
    ): List<AudioExercise> = withContext(Dispatchers.IO) {
        loadExercisesIfNeeded()

        var filtered = allExercises.asSequence()

        if (level != "TODOS") {
            filtered = filtered.filter { it.level.equals(level, ignoreCase = true) }
        }

        when (modality.lowercase()) {
            "escucha", "listening" -> filtered = filtered.filter { it.kind == "listening" }
            "fonología", "phonology" -> filtered = filtered.filter { it.kind == "phonology" }
            "frases", "phrase" -> filtered = filtered.filter { it.kind == "phrase" }
        }

        val list = filtered.toList().shuffled()
        if (list.isNotEmpty()) {
            list.take(count)
        } else {
            allExercises.shuffled().take(count)
        }
    }

    fun getTrialExercise(): AudioExercise {
        return AudioExercise(
            id = "trial_1",
            text = "Good morning, how can I help you today?",
            kind = "phrase",
            level = "A1",
            audioRelPath = "audio/phrase/lx22_a.ogg",
            questionPrompt = "Ensayo sin nota: ¿Qué saludo se pronunció?",
            options = listOf(
                ExerciseOption(0, "1", "Good morning, how can I help you today?", "1 toque"),
                ExerciseOption(1, "2", "Good evening, nice to meet you.", "2 toques"),
                ExerciseOption(2, "3", "See you tomorrow morning.", "3 toques")
            ),
            correctIndex = 0,
            explanation = "¡Excelente! Has respondido usando los controles de tus audífonos."
        )
    }

    private fun getFallbackExercises(): List<AudioExercise> {
        return listOf(
            AudioExercise(
                id = "fb_1",
                text = "The dentist moved my appointment from Tuesday morning to Wednesday afternoon.",
                kind = "listening",
                level = "A2",
                audioRelPath = "audio/listening/l12_appointment.ogg",
                questionPrompt = "¿Qué cambio de cita se realizó?",
                options = listOf(
                    ExerciseOption(0, "1", "De martes en la mañana a miércoles en la tarde", "1 toque"),
                    ExerciseOption(1, "2", "De lunes en la tarde a jueves en la mañana", "2 toques"),
                    ExerciseOption(2, "3", "Se canceló definitivamente la cita", "3 toques")
                ),
                correctIndex = 0,
                explanation = "Tuesday morning to Wednesday afternoon."
            )
        )
    }

    private data class RawEntry(
        val key: String,
        val text: String,
        val kind: String,
        val level: String,
        val rel: String
    )
}
