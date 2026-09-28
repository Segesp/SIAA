package com.siaa.app.data

import android.content.Context
import android.content.SharedPreferences
import com.siaa.core.model.DeviceProfile
import com.siaa.core.model.SessionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("siaa_preferences", Context.MODE_PRIVATE)

    private val _deviceProfile = MutableStateFlow(loadDeviceProfile())
    val deviceProfile: StateFlow<DeviceProfile> = _deviceProfile.asStateFlow()

    private val _highContrast = MutableStateFlow(prefs.getBoolean("pref_high_contrast", false))
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()

    private val _muteIfTalkBack = MutableStateFlow(prefs.getBoolean("pref_mute_if_talkback", true))
    val muteIfTalkBack: StateFlow<Boolean> = _muteIfTalkBack.asStateFlow()

    private val _sessionHistory = MutableStateFlow(loadSessionHistory())
    val sessionHistory: StateFlow<List<SessionResult>> = _sessionHistory.asStateFlow()

    fun updateDeviceProfile(profile: DeviceProfile) {
        _deviceProfile.value = profile
        prefs.edit()
            .putLong("profile_id", profile.id)
            .putString("profile_name", profile.name)
            .putInt("profile_primary_key", profile.primaryKeyCode)
            .putInt("profile_secondary_key", profile.secondaryKeyCode)
            .putInt("profile_back_key", profile.backKeyCode)
            .putInt("profile_stop_key", profile.stopKeyCode)
            .putBoolean("profile_play_pause_avail", profile.playPauseAvailable)
            .putBoolean("profile_next_avail", profile.nextAvailable)
            .putBoolean("profile_prev_avail", profile.previousAvailable)
            .putBoolean("profile_single_button", profile.singleButtonMode)
            .putInt("profile_silence_timeout", profile.silenceTimeoutSeconds)
            .putBoolean("profile_auto_repeat", profile.autoRepeatOptions)
            .putFloat("profile_voice_speed", profile.voiceSpeed)
            .apply()
    }

    fun setHighContrast(enabled: Boolean) {
        _highContrast.value = enabled
        prefs.edit().putBoolean("pref_high_contrast", enabled).apply()
    }

    fun setMuteIfTalkBack(enabled: Boolean) {
        _muteIfTalkBack.value = enabled
        prefs.edit().putBoolean("pref_mute_if_talkback", enabled).apply()
    }

    fun saveSessionResult(result: SessionResult) {
        val current = _sessionHistory.value.toMutableList()
        current.add(0, result)
        _sessionHistory.value = current

        val jsonArray = JSONArray()
        for (item in current.take(50)) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("totalQuestions", item.totalQuestions)
                put("correctAnswers", item.correctAnswers)
                put("durationMinutes", item.durationMinutes)
                put("level", item.level)
                put("modality", item.modality)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("pref_session_history", jsonArray.toString()).apply()
    }

    private fun loadDeviceProfile(): DeviceProfile {
        return DeviceProfile(
            id = prefs.getLong("profile_id", 1L),
            name = prefs.getString("profile_name", "Audífonos Predeterminados") ?: "Audífonos Predeterminados",
            primaryKeyCode = prefs.getInt("profile_primary_key", 85),
            secondaryKeyCode = prefs.getInt("profile_secondary_key", 87),
            backKeyCode = prefs.getInt("profile_back_key", 88),
            stopKeyCode = prefs.getInt("profile_stop_key", 86),
            playPauseAvailable = prefs.getBoolean("profile_play_pause_avail", true),
            nextAvailable = prefs.getBoolean("profile_next_avail", true),
            previousAvailable = prefs.getBoolean("profile_prev_avail", true),
            singleButtonMode = prefs.getBoolean("profile_single_button", false),
            silenceTimeoutSeconds = prefs.getInt("profile_silence_timeout", 12),
            autoRepeatOptions = prefs.getBoolean("profile_auto_repeat", true),
            voiceSpeed = prefs.getFloat("profile_voice_speed", 1.0f)
        )
    }

    private fun loadSessionHistory(): List<SessionResult> {
        val raw = prefs.getString("pref_session_history", null) ?: return emptyList()
        val list = mutableListOf<SessionResult>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SessionResult(
                        id = obj.getString("id"),
                        totalQuestions = obj.getInt("totalQuestions"),
                        correctAnswers = obj.getInt("correctAnswers"),
                        durationMinutes = obj.getInt("durationMinutes"),
                        level = obj.getString("level"),
                        modality = obj.getString("modality"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
