package com.siaa.core.model

data class AudioExercise(
    val id: String,
    val text: String,
    val kind: String,
    val level: String,
    val audioRelPath: String,
    val questionPrompt: String,
    val options: List<ExerciseOption>,
    val correctIndex: Int,
    val explanation: String
)

data class ExerciseOption(
    val index: Int,
    val keyBadge: String,
    val text: String,
    val earbudHint: String
)

data class SessionResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val totalQuestions: Int,
    val correctAnswers: Int,
    val durationMinutes: Int,
    val level: String,
    val modality: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AudioRouteType {
    HEADPHONES_BLUETOOTH,
    HEADPHONES_WIRED,
    DEVICE_SPEAKER,
    UNKNOWN
}
