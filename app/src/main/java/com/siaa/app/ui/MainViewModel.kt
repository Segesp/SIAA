package com.siaa.app.ui

import android.app.Application
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siaa.app.data.ExerciseRepository
import com.siaa.app.data.UserPreferencesRepository
import com.siaa.app.media.AudioPlayerManager
import com.siaa.core.model.AudioExercise
import com.siaa.core.model.AudioRouteType
import com.siaa.core.model.DeviceProfile
import com.siaa.core.model.SessionResult
import com.siaa.core.runtime.MediaControlEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppTab {
    HOME,
    HEADPHONES,
    MORE,
    ACTIVE_SESSION
}

enum class SessionState {
    IDLE,
    PLAYING_PROMPT,
    ANNOUNCING_OPTIONS,
    WAITING_ANSWER,
    SHOWING_FEEDBACK,
    PAUSED,
    FINISHED
}

enum class CalibrationStep {
    STEP_SOUND_CHECK,
    STEP_BUTTON_MAPPING,
    STEP_TRIAL,
    COMPLETED
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepo = UserPreferencesRepository(application)
    private val exerciseRepo = ExerciseRepository(application)

    val audioPlayer = AudioPlayerManager(
        context = application,
        onBecomingNoisy = {
            if (_sessionState.value != SessionState.IDLE && _sessionState.value != SessionState.FINISHED) {
                pauseSession()
            }
        }
    )

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    val deviceProfile: StateFlow<DeviceProfile> = preferencesRepo.deviceProfile
    val highContrast: StateFlow<Boolean> = preferencesRepo.highContrast
    val sessionHistory: StateFlow<List<SessionResult>> = preferencesRepo.sessionHistory

    // Session Launcher Configuration
    private val _selectedLevel = MutableStateFlow("TODOS")
    val selectedLevel: StateFlow<String> = _selectedLevel.asStateFlow()

    private val _selectedModality = MutableStateFlow("Mixto")
    val selectedModality: StateFlow<String> = _selectedModality.asStateFlow()

    private val _selectedDuration = MutableStateFlow(10)
    val selectedDuration: StateFlow<Int> = _selectedDuration.asStateFlow()

    // Active Session State
    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _exercises = MutableStateFlow<List<AudioExercise>>(emptyList())
    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()

    private val _currentExercise = MutableStateFlow<AudioExercise?>(null)
    val currentExercise: StateFlow<AudioExercise?> = _currentExercise.asStateFlow()

    private val _activeSpokenOptionIndex = MutableStateFlow(0)
    val activeSpokenOptionIndex: StateFlow<Int> = _activeSpokenOptionIndex.asStateFlow()

    private val _lastAnswerCorrect = MutableStateFlow<Boolean?>(null)
    val lastAnswerCorrect: StateFlow<Boolean?> = _lastAnswerCorrect.asStateFlow()

    private val _sessionScore = MutableStateFlow(0)
    val sessionScore: StateFlow<Int> = _sessionScore.asStateFlow()

    private val _isTrialMode = MutableStateFlow(false)
    val isTrialMode: StateFlow<Boolean> = _isTrialMode.asStateFlow()

    // Headphone Calibration Wizard State
    private val _calibrationStep = MutableStateFlow(CalibrationStep.STEP_SOUND_CHECK)
    val calibrationStep: StateFlow<CalibrationStep> = _calibrationStep.asStateFlow()

    private val _lastDetectedKeyCode = MutableStateFlow<Int?>(null)
    val lastDetectedKeyCode: StateFlow<Int?> = _lastDetectedKeyCode.asStateFlow()

    private val _lastDetectedKeyName = MutableStateFlow<String?>(null)
    val lastDetectedKeyName: StateFlow<String?> = _lastDetectedKeyName.asStateFlow()

    private var sessionJob: Job? = null
    private var silenceTimeoutJob: Job? = null

    init {
        viewModelScope.launch {
            exerciseRepo.loadExercisesIfNeeded()
        }
    }

    fun selectTab(tab: AppTab) {
        if (_sessionState.value != SessionState.IDLE && _sessionState.value != SessionState.FINISHED && tab != AppTab.ACTIVE_SESSION) {
            pauseSession()
        }
        _currentTab.value = tab
    }

    fun setLevel(level: String) { _selectedLevel.value = level }
    fun setModality(modality: String) { _selectedModality.value = modality }
    fun setDuration(minutes: Int) { _selectedDuration.value = minutes }

    fun toggleHighContrast(enabled: Boolean) {
        preferencesRepo.setHighContrast(enabled)
    }

    fun updateProfile(profile: DeviceProfile) {
        preferencesRepo.updateDeviceProfile(profile)
    }

    // --- Interactive Session Lifecycle ---

    fun startSession(isTrial: Boolean = false) {
        _isTrialMode.value = isTrial
        _sessionScore.value = 0
        _currentExerciseIndex.value = 0
        _lastAnswerCorrect.value = null
        _currentTab.value = AppTab.ACTIVE_SESSION

        viewModelScope.launch {
            val list = if (isTrial) {
                listOf(exerciseRepo.getTrialExercise())
            } else {
                val totalExercises = (_selectedDuration.value * 2).coerceIn(6, 40)
                exerciseRepo.getExercises(
                    level = _selectedLevel.value,
                    modality = _selectedModality.value,
                    count = totalExercises
                )
            }
            _exercises.value = list
            if (list.isNotEmpty()) {
                playExercise(list[0])
            }
        }
    }

    private fun playExercise(exercise: AudioExercise) {
        _currentExercise.value = exercise
        _lastAnswerCorrect.value = null
        _sessionState.value = SessionState.PLAYING_PROMPT
        silenceTimeoutJob?.cancel()

        // 1. Play target audio clip
        audioPlayer.playAssetAudio(exercise.audioRelPath) {
            // Target audio finished -> now announce options
            if (_sessionState.value == SessionState.PLAYING_PROMPT) {
                announceOptions(exercise)
            }
        }
    }

    private fun announceOptions(exercise: AudioExercise) {
        _sessionState.value = SessionState.ANNOUNCING_OPTIONS

        val profile = deviceProfile.value
        // If TalkBack touch exploration is enabled, skip auto speech announcements to avoid speech collisions
        if (audioPlayer.isTouchExplorationActive()) {
            _sessionState.value = SessionState.WAITING_ANSWER
            startSilenceTimer(exercise)
            return
        }

        if (profile.singleButtonMode) {
            // Cycle through options one by one
            cycleSingleButtonOptions(exercise, 0)
        } else {
            // Speak all options
            val promptText = "Opciones: " + exercise.options.joinToString(". ") {
                "Opción ${it.index + 1}: ${it.text}"
            }
            audioPlayer.speakText(promptText, isEnglish = false, speechRate = profile.voiceSpeed) {
                if (_sessionState.value == SessionState.ANNOUNCING_OPTIONS) {
                    _sessionState.value = SessionState.WAITING_ANSWER
                    startSilenceTimer(exercise)
                }
            }
        }
    }

    private fun cycleSingleButtonOptions(exercise: AudioExercise, optionIndex: Int) {
        if (_sessionState.value != SessionState.ANNOUNCING_OPTIONS && _sessionState.value != SessionState.WAITING_ANSWER) return

        val opt = exercise.options[optionIndex]
        _activeSpokenOptionIndex.value = optionIndex
        _sessionState.value = SessionState.ANNOUNCING_OPTIONS

        val textToSpeak = "Opción ${opt.index + 1}: ${opt.text}"
        audioPlayer.speakText(textToSpeak, isEnglish = false, speechRate = deviceProfile.value.voiceSpeed) {
            _sessionState.value = SessionState.WAITING_ANSWER
            // Wait for user click, or after 3.5s proceed to next option
            silenceTimeoutJob?.cancel()
            silenceTimeoutJob = viewModelScope.launch {
                delay(3500)
                if (_sessionState.value == SessionState.WAITING_ANSWER) {
                    val nextOpt = (optionIndex + 1) % exercise.options.size
                    cycleSingleButtonOptions(exercise, nextOpt)
                }
            }
        }
    }

    private fun startSilenceTimer(exercise: AudioExercise) {
        silenceTimeoutJob?.cancel()
        val timeoutSecs = deviceProfile.value.silenceTimeoutSeconds
        silenceTimeoutJob = viewModelScope.launch {
            delay(timeoutSecs * 1000L)
            if (_sessionState.value == SessionState.WAITING_ANSWER) {
                if (deviceProfile.value.autoRepeatOptions) {
                    // Repeat prompt and options gently without failing
                    audioPlayer.speakText("Repitiendo audio.", isEnglish = false) {
                        playExercise(exercise)
                    }
                } else {
                    pauseSession()
                }
            }
        }
    }

    fun submitAnswer(optionIndex: Int) {
        silenceTimeoutJob?.cancel()
        val exercise = _currentExercise.value ?: return
        val isCorrect = (optionIndex == exercise.correctIndex)
        _lastAnswerCorrect.value = isCorrect
        _sessionState.value = SessionState.SHOWING_FEEDBACK

        if (isCorrect) {
            _sessionScore.value += 1
        }

        val feedbackSpeech = if (isCorrect) {
            "¡Correcto! " + exercise.explanation
        } else {
            "No del todo. " + exercise.explanation
        }

        audioPlayer.speakText(feedbackSpeech, isEnglish = false) {
            viewModelScope.launch {
                delay(1200)
                advanceToNextExercise()
            }
        }
    }

    private fun advanceToNextExercise() {
        val nextIdx = _currentExerciseIndex.value + 1
        val list = _exercises.value
        if (nextIdx < list.size) {
            _currentExerciseIndex.value = nextIdx
            playExercise(list[nextIdx])
        } else {
            finishSession()
        }
    }

    fun repeatCurrentAudio() {
        _currentExercise.value?.let { playExercise(it) }
    }

    fun pauseSession() {
        audioPlayer.pause()
        silenceTimeoutJob?.cancel()
        _sessionState.value = SessionState.PAUSED
    }

    fun resumeSession() {
        _currentExercise.value?.let {
            playExercise(it)
        }
    }

    fun finishSession() {
        audioPlayer.stop()
        silenceTimeoutJob?.cancel()
        _sessionState.value = SessionState.FINISHED

        if (!_isTrialMode.value && _exercises.value.isNotEmpty()) {
            val result = SessionResult(
                totalQuestions = _exercises.value.size,
                correctAnswers = _sessionScore.value,
                durationMinutes = _selectedDuration.value,
                level = _selectedLevel.value,
                modality = _selectedModality.value
            )
            preferencesRepo.saveSessionResult(result)
        }
    }

    fun exitSessionToHome() {
        finishSession()
        _sessionState.value = SessionState.IDLE
        _currentTab.value = AppTab.HOME
    }

    // --- Media Key & Earbud Input Handling ---

    fun handleMediaControlEvent(event: MediaControlEvent) {
        when (_currentTab.value) {
            AppTab.ACTIVE_SESSION -> {
                when (event) {
                    MediaControlEvent.PLAY_PAUSE -> {
                        if (_sessionState.value == SessionState.PAUSED) {
                            resumeSession()
                        } else if (_sessionState.value == SessionState.WAITING_ANSWER || _sessionState.value == SessionState.ANNOUNCING_OPTIONS) {
                            if (deviceProfile.value.singleButtonMode) {
                                submitAnswer(_activeSpokenOptionIndex.value)
                            } else {
                                submitAnswer(0) // Option 1
                            }
                        }
                    }
                    MediaControlEvent.NEXT -> {
                        if (_sessionState.value == SessionState.WAITING_ANSWER || _sessionState.value == SessionState.ANNOUNCING_OPTIONS) {
                            submitAnswer(1) // Option 2
                        }
                    }
                    MediaControlEvent.PREVIOUS -> {
                        if (_sessionState.value == SessionState.WAITING_ANSWER || _sessionState.value == SessionState.ANNOUNCING_OPTIONS) {
                            submitAnswer(2) // Option 3
                        }
                    }
                    MediaControlEvent.STOP, MediaControlEvent.PAUSE -> {
                        pauseSession()
                    }
                    else -> {}
                }
            }
            AppTab.HEADPHONES -> {
                // When in calibration step
                if (_calibrationStep.value == CalibrationStep.STEP_BUTTON_MAPPING) {
                    val code = when (event) {
                        MediaControlEvent.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                        MediaControlEvent.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
                        MediaControlEvent.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                        MediaControlEvent.STOP -> KeyEvent.KEYCODE_MEDIA_STOP
                        MediaControlEvent.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
                        else -> 85
                    }
                    recordDetectedKey(code)
                }
            }
            AppTab.HOME -> {
                if (event == MediaControlEvent.PLAY_PAUSE) {
                    startSession(isTrial = false)
                }
            }
            else -> {}
        }
    }

    fun recordDetectedKey(keyCode: Int) {
        val name = when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK, 85 -> "Botón Central / Play-Pause"
            KeyEvent.KEYCODE_MEDIA_NEXT, 87 -> "Siguiente Pista / Doble Toque"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, 88 -> "Pista Anterior / Triple Toque"
            KeyEvent.KEYCODE_MEDIA_STOP, 86 -> "Botón Stop"
            else -> "Tecla de Audífono ($keyCode)"
        }
        _lastDetectedKeyCode.value = keyCode
        _lastDetectedKeyName.value = name

        // Update profile with detected button
        val current = deviceProfile.value
        val updated = current.copy(primaryKeyCode = keyCode)
        updateProfile(updated)
    }

    // --- Headphone Calibration Wizard ---

    fun setCalibrationStep(step: CalibrationStep) {
        _calibrationStep.value = step
        if (step == CalibrationStep.STEP_TRIAL) {
            startSession(isTrial = true)
        }
    }

    fun playSoundCheck() {
        audioPlayer.speakText(
            "Prueba de sonido de SIAA. Si escuchas esto claramente en tus audífonos, tu salida privada está lista para tu viaje.",
            isEnglish = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
