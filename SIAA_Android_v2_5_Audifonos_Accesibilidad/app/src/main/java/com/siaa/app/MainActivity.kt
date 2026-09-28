package com.siaa.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.siaa.app.media.SiaaPlaybackService
import com.siaa.app.ui.SiaaApp
import com.siaa.core.model.SessionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private val graph get() = (application as SiaaApplication).graph
    private var pendingSpeech = false

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val audioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingSpeech) { pendingSpeech=false; beginSpeechRecognition() }
        else if (!granted) { pendingSpeech=false; viewModel.setSystemMessage("Sin permiso de micrófono no se compara la voz. Puedes seguir usando las sesiones auditivas.") }
    }
    private val exportBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@registerForActivityResult
        if (!canChangeData()) return@registerForActivityResult
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                val root=JSONObject(graph.backupManager.exportJson()); root.put("userPreferences",graph.preferenceStore.exportJsonObject())
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(root.toString(2)) } ?: error("No se pudo abrir destino")
            } }
                .onSuccess { viewModel.setSystemMessage("Backup exportado correctamente.") }
                .onFailure { viewModel.setSystemMessage("Error al exportar: ${it.message}") }
        }
    }
    private val importBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        if (!canChangeData()) return@registerForActivityResult
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                val text=contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("No se pudo leer backup")
                val root=JSONObject(text); graph.backupManager.importJson(root.toString()); root.optJSONObject("userPreferences")?.let(graph.preferenceStore::importJsonObject)
                graph.preferenceStore.update { it.copy(headphones=it.headphones.copy(setupCompleted=false,confirmedRoute="")) }
            } }
                .onSuccess { viewModel.setSystemMessage("Backup restaurado."); viewModel.refresh() }
                .onFailure { viewModel.setSystemMessage("Backup rechazado: ${it.message}") }
        }
    }
    private val importContentPack = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        if (!canChangeData()) return@registerForActivityResult
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { contentResolver.openInputStream(uri)?.use { graph.contentPackManager.install(it) } ?: error("No se pudo leer pack") } }
                .onSuccess { r ->
                    withContext(Dispatchers.IO) { graph.seeder.seedIfNeeded(force = true) }
                    viewModel.setSystemMessage("Content pack ${r.version} instalado y verificado."); viewModel.refresh()
                }.onFailure { viewModel.setSystemMessage("Content pack rechazado: ${it.message}") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SiaaApp(
                viewModel = viewModel,
                onStartMode = ::startMode,
                onStop = ::stopSession,
                onStartCalibration = ::startCalibration,
                onStopCalibration = ::stopCalibration,
                onStartSpeaking = ::requestSpeaking,
                onExportBackup = { exportBackup.launch("siaa-learning-backup.json") },
                onImportBackup = { importBackup.launch(arrayOf("application/json","text/plain")) },
                onImportContentPack = { importContentPack.launch(arrayOf("application/zip","application/octet-stream")) },
                onRollbackContent = ::rollbackContent,
                onRuntimeAction = ::sendRuntimeAction,
                onSpeakGuide = { sendServiceAction(SiaaPlaybackService.ACTION_SPEAK_GUIDE) },
                onOpenAudioSettings = { openSystemSettings(android.provider.Settings.ACTION_SOUND_SETTINGS) },
                onOpenVoiceSettings = { openSystemSettings(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA) }
            )
        }
    }

    override fun onResume() { super.onResume(); viewModel.refresh() }

    private fun startMode(mode: SessionMode) {
        requestNotificationPermissionIfNeeded()
        viewModel.setSystemMessage("")
        val p = graph.preferenceStore.current()
        val intent = Intent(this, SiaaPlaybackService::class.java).apply {
            action = SiaaPlaybackService.ACTION_START_SESSION
            putExtra(SiaaPlaybackService.EXTRA_MODE, mode.name)
            putExtra(SiaaPlaybackService.EXTRA_MAX_ITEMS, p.maxItems)
            putExtra(SiaaPlaybackService.EXTRA_TARGET_DURATION_MINUTES, p.targetDurationMinutes)
            putExtra(SiaaPlaybackService.EXTRA_LEARNING_GOAL, p.learningGoal.name)
            putExtra(SiaaPlaybackService.EXTRA_ENGLISH_VARIETY, p.englishVariety.name)
            putExtra(SiaaPlaybackService.EXTRA_INTENSITY, p.intensity.name)
            putExtra(SiaaPlaybackService.EXTRA_ANNOUNCE_CONTROLS, p.announceControls)
            putExtra(SiaaPlaybackService.EXTRA_FEEDBACK_EXPLANATIONS, p.feedbackExplanations)
            putExtra(SiaaPlaybackService.EXTRA_SPEECH_RATE, p.speechRate)
            putExtra(SiaaPlaybackService.EXTRA_CONTROL_TOKEN, graph.controlToken)
        }
        runCatching { ContextCompat.startForegroundService(this, intent) }
            .onFailure { viewModel.setSystemMessage("Android no permitió iniciar el audio. Mantén la app abierta y vuelve a intentarlo.") }
    }

    private fun requestSpeaking() {
        if (graph.runtime.snapshot.value.state !in setOf(com.siaa.core.runtime.LessonState.IDLE,com.siaa.core.runtime.LessonState.SESSION_END,com.siaa.core.runtime.LessonState.ERROR)) {
            viewModel.setSystemMessage("Finaliza la sesión auditiva antes de usar el micrófono."); return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingSpeech=true; audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else beginSpeechRecognition()
    }
    private fun beginSpeechRecognition() {
        val item=viewModel.uiState.value.practice.speaking ?: return
        viewModel.setSystemMessage("Escuchando: ${item.targetEn}")
        graph.productionSpeechRecognizer.start { result -> runOnUiThread {
            result.onSuccess(viewModel::completeSpeaking).onFailure { viewModel.setSystemMessage("No se pudo evaluar speaking: ${it.message}") }
        } }
    }

    private fun canChangeData(): Boolean {
        val idle = graph.runtime.snapshot.value.state in setOf(com.siaa.core.runtime.LessonState.IDLE,com.siaa.core.runtime.LessonState.SESSION_END,com.siaa.core.runtime.LessonState.ERROR) && !graph.mediaDiagnostics.setup.value.active
        if (!idle) viewModel.setSystemMessage("Finaliza la sesión o comprobación antes de cambiar datos.")
        return idle
    }
    private fun rollbackContent() {
        if (!canChangeData()) return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { graph.contentPackManager.rollbackToBundled(); graph.seeder.seedIfNeeded(force=true) }
            viewModel.setSystemMessage("Se restauró el contenido incluido en la app."); viewModel.refresh()
        }
    }

    private fun stopSession() { startService(Intent(this, SiaaPlaybackService::class.java).apply { action=SiaaPlaybackService.ACTION_STOP_SESSION; putExtra(SiaaPlaybackService.EXTRA_CONTROL_TOKEN,graph.controlToken) }) }
    private fun startCalibration() { requestNotificationPermissionIfNeeded(); viewModel.setSystemMessage(""); sendServiceAction(SiaaPlaybackService.ACTION_START_CALIBRATION) }
    private fun stopCalibration() { startService(Intent(this,SiaaPlaybackService::class.java).apply { action=SiaaPlaybackService.ACTION_STOP_CALIBRATION; putExtra(SiaaPlaybackService.EXTRA_CONTROL_TOKEN,graph.controlToken) }) }
    private fun sendServiceAction(actionName: String) {
        runCatching { ContextCompat.startForegroundService(this,Intent(this,SiaaPlaybackService::class.java).apply {
            action=actionName; putExtra(SiaaPlaybackService.EXTRA_CONTROL_TOKEN,graph.controlToken)
        }) }.onFailure { viewModel.setSystemMessage("No se pudo iniciar el audio. Mantén SIAA abierto e inténtalo otra vez.") }
    }
    private fun sendRuntimeAction(actionName:String,turn:Long) {
        runCatching { startService(Intent(this,SiaaPlaybackService::class.java).apply {
            action=SiaaPlaybackService.ACTION_RUNTIME
            putExtra(SiaaPlaybackService.EXTRA_RUNTIME_ACTION,actionName)
            putExtra(SiaaPlaybackService.EXTRA_TURN,turn)
            putExtra(SiaaPlaybackService.EXTRA_SESSION,graph.runtime.snapshot.value.sessionId ?: -1L)
            putExtra(SiaaPlaybackService.EXTRA_CONTROL_TOKEN,graph.controlToken)
        }) }.onFailure { viewModel.setSystemMessage("Abre de nuevo la sesión desde Inicio.") }
    }
    private fun openSystemSettings(actionName:String) {
        runCatching { startActivity(Intent(actionName)) }.onFailure {
            runCatching { startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                .onFailure { viewModel.setSystemMessage("Abre Ajustes del teléfono y busca ‘Texto a voz’ o ‘Sonido’.") }
        }
    }
    private fun requestNotificationPermissionIfNeeded() { if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
}
