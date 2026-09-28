package com.siaa.app

import android.content.Context
import com.siaa.core.model.HeadphonePreferences
import com.siaa.core.model.HeadphoneMode
import com.siaa.core.model.EnglishVariety
import com.siaa.core.model.LearningGoal
import com.siaa.core.model.SessionIntensity
import com.siaa.core.model.SessionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class UserPreferences(
    val headphones: HeadphonePreferences = HeadphonePreferences(),
    val maxItems: Int = 60,
    val targetDurationMinutes: Int = 25,
    val dailyGoalMinutes: Int = 30,
    val announceControls: Boolean = true,
    val feedbackExplanations: Boolean = true,
    val defaultMode: SessionMode = SessionMode.ADAPTIVE,
    val speechRate: Float = 1.0f,
    val learningGoal: LearningGoal = LearningGoal.GENERAL,
    val targetCefr: String = "B2",
    val englishVariety: EnglishVariety = EnglishVariety.MIXED,
    val intensity: SessionIntensity = SessionIntensity.BALANCED,
    val onboardingCompleted: Boolean = false,
    val placementCompleted: Boolean = false,
    val extendedSkillsEnabled: Boolean = true
)

class PreferenceStore(context: Context) {
    private val prefs = context.getSharedPreferences("siaa_preferences", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<UserPreferences> = _state.asStateFlow()

    fun current(): UserPreferences = _state.value

    fun exportJsonObject(): JSONObject = JSONObject().apply {
        val p=current(); put("headphones", headphoneJson(p.headphones)); put("maxItems",p.maxItems); put("targetDurationMinutes",p.targetDurationMinutes); put("dailyGoalMinutes",p.dailyGoalMinutes);
        put("announceControls",p.announceControls); put("feedbackExplanations",p.feedbackExplanations); put("defaultMode",p.defaultMode.name); put("speechRate",p.speechRate.toDouble());
        put("learningGoal",p.learningGoal.name); put("targetCefr",p.targetCefr); put("englishVariety",p.englishVariety.name); put("intensity",p.intensity.name);
        put("onboardingCompleted",p.onboardingCompleted); put("placementCompleted",p.placementCompleted); put("extendedSkillsEnabled",p.extendedSkillsEnabled)
    }

    fun importJsonObject(o: JSONObject) = update { old -> old.copy(
        headphones=o.optJSONObject("headphones")?.let(::readHeadphones) ?: old.headphones,
        maxItems=o.optInt("maxItems",old.maxItems), targetDurationMinutes=o.optInt("targetDurationMinutes",old.targetDurationMinutes), dailyGoalMinutes=o.optInt("dailyGoalMinutes",old.dailyGoalMinutes),
        announceControls=o.optBoolean("announceControls",old.announceControls), feedbackExplanations=o.optBoolean("feedbackExplanations",old.feedbackExplanations),
        defaultMode=runCatching { SessionMode.valueOf(o.optString("defaultMode",old.defaultMode.name)) }.getOrDefault(old.defaultMode), speechRate=o.optDouble("speechRate",old.speechRate.toDouble()).toFloat(),
        learningGoal=runCatching { LearningGoal.valueOf(o.optString("learningGoal",old.learningGoal.name)) }.getOrDefault(old.learningGoal), targetCefr=o.optString("targetCefr",old.targetCefr),
        englishVariety=runCatching { EnglishVariety.valueOf(o.optString("englishVariety",old.englishVariety.name)) }.getOrDefault(old.englishVariety), intensity=runCatching { SessionIntensity.valueOf(o.optString("intensity",old.intensity.name)) }.getOrDefault(old.intensity),
        onboardingCompleted=o.optBoolean("onboardingCompleted",old.onboardingCompleted), placementCompleted=o.optBoolean("placementCompleted",old.placementCompleted), extendedSkillsEnabled=o.optBoolean("extendedSkillsEnabled",old.extendedSkillsEnabled)
    ) }

    fun update(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(_state.value).normalized()
        prefs.edit()
            .putString("headphones_v25", headphoneJson(next.headphones).toString())
            .putInt(KEY_MAX_ITEMS, next.maxItems)
            .putInt(KEY_DURATION, next.targetDurationMinutes)
            .putInt(KEY_DAILY_GOAL, next.dailyGoalMinutes)
            .putBoolean(KEY_ANNOUNCE, next.announceControls)
            .putBoolean(KEY_EXPLANATIONS, next.feedbackExplanations)
            .putString(KEY_DEFAULT_MODE, next.defaultMode.name)
            .putFloat(KEY_SPEECH_RATE, next.speechRate)
            .putString(KEY_GOAL, next.learningGoal.name)
            .putString(KEY_TARGET_CEFR, next.targetCefr)
            .putString(KEY_VARIETY, next.englishVariety.name)
            .putString(KEY_INTENSITY, next.intensity.name)
            .putBoolean(KEY_ONBOARDING, next.onboardingCompleted)
            .putBoolean(KEY_PLACEMENT, next.placementCompleted)
            .putBoolean(KEY_EXTENDED, next.extendedSkillsEnabled)
            .apply()
        _state.value = next
    }

    private fun load(): UserPreferences = UserPreferences(
        headphones = runCatching { readHeadphones(JSONObject(prefs.getString("headphones_v25", "{}") ?: "{}")) }.getOrDefault(HeadphonePreferences()),
        maxItems = prefs.getInt(KEY_MAX_ITEMS, 60),
        targetDurationMinutes = prefs.getInt(KEY_DURATION, 25),
        dailyGoalMinutes = prefs.getInt(KEY_DAILY_GOAL, 30),
        announceControls = prefs.getBoolean(KEY_ANNOUNCE, true),
        feedbackExplanations = prefs.getBoolean(KEY_EXPLANATIONS, true),
        defaultMode = enumValue(KEY_DEFAULT_MODE, SessionMode.ADAPTIVE),
        speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f),
        learningGoal = enumValue(KEY_GOAL, LearningGoal.GENERAL),
        targetCefr = prefs.getString(KEY_TARGET_CEFR, "B2") ?: "B2",
        englishVariety = enumValue(KEY_VARIETY, EnglishVariety.MIXED),
        intensity = enumValue(KEY_INTENSITY, SessionIntensity.BALANCED),
        onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING, false),
        placementCompleted = prefs.getBoolean(KEY_PLACEMENT, false),
        extendedSkillsEnabled = prefs.getBoolean(KEY_EXTENDED, true)
    ).normalized()

    private inline fun <reified T : Enum<T>> enumValue(key: String, fallback: T): T =
        prefs.getString(key, fallback.name)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private fun UserPreferences.normalized() = copy(
        headphones = headphones.normalized(),
        maxItems = maxItems.coerceIn(5, 300),
        targetDurationMinutes = targetDurationMinutes.coerceIn(5, 180),
        dailyGoalMinutes = dailyGoalMinutes.coerceIn(5, 240),
        speechRate = if (speechRate.isFinite()) speechRate.coerceIn(0.65f, 1.35f) else 1.0f,
        targetCefr = targetCefr.uppercase().let { if (it in VALID_CEFR) it else "B2" }
    )

    private fun headphoneJson(h: HeadphonePreferences) = JSONObject().apply {
        put("mode",h.mode.name); put("primaryGesture",h.primaryGesture); put("secondaryGesture",h.secondaryGesture); put("backGesture",h.backGesture)
        put("responseSeconds",h.responseSeconds); put("scanSeconds",h.scanSeconds); put("transitionMillis",h.transitionMillis)
        put("pauseOnSilence",h.pauseOnSilence); put("captionsEnabled",h.captionsEnabled); put("simpleHome",h.simpleHome); put("highContrast",h.highContrast)
        put("setupCompleted",h.setupCompleted); put("confirmedRoute",h.confirmedRoute)
    }
    private fun readHeadphones(o: JSONObject) = HeadphonePreferences(
        mode = runCatching { HeadphoneMode.valueOf(o.optString("mode","AUTO")) }.getOrDefault(HeadphoneMode.AUTO),
        primaryGesture=o.optString("primaryGesture","tu gesto principal"), secondaryGesture=o.optString("secondaryGesture","tu segundo gesto"), backGesture=o.optString("backGesture","tu tercer gesto"),
        responseSeconds=o.optInt("responseSeconds",20),scanSeconds=o.optInt("scanSeconds",4),transitionMillis=o.optLong("transitionMillis",700),
        pauseOnSilence=o.optBoolean("pauseOnSilence",true),captionsEnabled=o.optBoolean("captionsEnabled",true),simpleHome=o.optBoolean("simpleHome",true),highContrast=o.optBoolean("highContrast",false),
        setupCompleted=o.optBoolean("setupCompleted",false),confirmedRoute=o.optString("confirmedRoute","")
    ).normalized()

    companion object {
        private val VALID_CEFR = setOf("PRE-A1", "A1", "A2", "B1", "B2", "C1", "C2")
        private const val KEY_MAX_ITEMS = "max_items"
        private const val KEY_DURATION = "target_duration_minutes"
        private const val KEY_DAILY_GOAL = "daily_goal_minutes"
        private const val KEY_ANNOUNCE = "announce_controls"
        private const val KEY_EXPLANATIONS = "feedback_explanations"
        private const val KEY_DEFAULT_MODE = "default_mode"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_GOAL = "learning_goal"
        private const val KEY_TARGET_CEFR = "target_cefr"
        private const val KEY_VARIETY = "english_variety"
        private const val KEY_INTENSITY = "session_intensity"
        private const val KEY_ONBOARDING = "onboarding_completed"
        private const val KEY_PLACEMENT = "placement_completed"
        private const val KEY_EXTENDED = "extended_skills_enabled"
    }
}
