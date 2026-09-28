package com.siaa.app.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityManager
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.siaa.app.ContentInitState
import com.siaa.app.MainActivity
import com.siaa.app.R
import com.siaa.app.SiaaApplication
import com.siaa.core.audio.AudioFocusController
import com.siaa.core.audio.AudioOutputGuard
import com.siaa.core.model.*
import com.siaa.core.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

/** All playback/UI/notification entry points share the same safety and answer gates. */
@OptIn(UnstableApi::class)
class SiaaPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private lateinit var output: AudioOutputGuard
    private lateinit var audioFocus: AudioFocusController
    private var activeDeviceProfile: DeviceProfile? = null
    private var calibrationActive = false
    private var ownsSession = false
    private var sawActiveSession = false
    private var startJob: Job? = null
    private var coachJob: Job? = null
    private var setupExpiryJob: Job? = null
    private var finishing = false
    private var coachGeneration = 0L
    private val router = EarbudCommandRouter()
    private val inputGate = MediaInputGate()
    private val interpreter = MediaCommandInterpreter()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appGraph get() = (application as SiaaApplication).graph
    private val runtime get() = appGraph.runtime
    private fun running() = runtime.snapshot.value.state !in TERMINAL

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("Preparando audio…"))
        audioFocus = AudioFocusController(this, onTransientLoss = {
            if (calibrationActive) abortSetup("Otra aplicación necesita el audio. Vuelve a comprobar tus controles.")
            else if (running()) { runtime.pauseForFocusLoss(); if (::player.isInitialized) player.pause() }
            else { stopCoach(); finishService() }
        })
        output = AudioOutputGuard(this, ::routeChanged)
        output.register()
        player = ExoPlayer.Builder(this).build()
        // System transport calls act on the tutor, not just the silent Media3 anchor.
        // A lock-screen Play/Pause is never interpreted as answer A.
        val tutorPlayer = object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands = super.getAvailableCommands().buildUpon()
                .addAll(Player.COMMAND_PLAY_PAUSE, Player.COMMAND_STOP,
                    Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM).build()
            override fun isCommandAvailable(command: Int): Boolean = availableCommands.contains(command)
            override fun play() { transport(KeyEvent.KEYCODE_MEDIA_PLAY) }
            override fun pause() { transport(KeyEvent.KEYCODE_MEDIA_PAUSE) }
            override fun setPlayWhenReady(playWhenReady: Boolean) { transport(if (playWhenReady) KeyEvent.KEYCODE_MEDIA_PLAY else KeyEvent.KEYCODE_MEDIA_PAUSE) }
            override fun stop() { transport(KeyEvent.KEYCODE_MEDIA_STOP) }
            override fun seekToNext() { transport(KeyEvent.KEYCODE_MEDIA_NEXT) }
            override fun seekToNextMediaItem() { transport(KeyEvent.KEYCODE_MEDIA_NEXT) }
            override fun seekToPrevious() { transport(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
            override fun seekToPreviousMediaItem() { transport(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
        }
        session = MediaSession.Builder(this, tutorPlayer)
            .setSessionActivity(openAppIntent())
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    if (controller.packageName != packageName && !controller.isTrusted)
                        return MediaSession.ConnectionResult.reject()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailablePlayerCommands(tutorPlayer.availableCommands)
                        .build()
                }
                override fun onMediaButtonEvent(session: MediaSession, controllerInfo: MediaSession.ControllerInfo, intent: Intent): Boolean {
                    if (intent.action != Intent.ACTION_MEDIA_BUTTON) return false
                    val key = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return true
                    if (!inputGate.accept(key.keyCode, key.action == KeyEvent.ACTION_DOWN, key.repeatCount, SystemClock.elapsedRealtime())) return true
                    appGraph.mediaDiagnostics.recordRawKeyCode(key.keyCode)
                    if (calibrationActive) {
                        if (key.keyCode == KeyEvent.KEYCODE_MEDIA_STOP) { abortSetup("Comprobación detenida."); return true }
                        appGraph.mediaDiagnostics.receiveSetup(key.keyCode)
                        return true // Never fall through to Media3's default player handling.
                    }
                    val mapped = router.fromMediaButtonIntent(intent, activeDeviceProfile) ?: return true
                    appGraph.mediaDiagnostics.record(mapped.first, mapped.second)
                    interpreter.interpret(mapped.first, runtime.snapshot.value.state)?.let(::runtimeCommand)
                    return true // Includes known commands intentionally ignored by the turn state.
                }
            }).build()

        serviceScope.launch {
            runtime.snapshot.collectLatest { snap ->
                if (!ownsSession) return@collectLatest
                if (snap.state !in TERMINAL) sawActiveSession = true
                if (snap.state == LessonState.PAUSED) {
                    player.pause()
                    audioFocus.abandon()
                }
                updateMetadata(snap.message)
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(snap.message))
                if (sawActiveSession && snap.state in TERMINAL) finishService()
            }
        }
        serviceScope.launch {
            appGraph.mediaDiagnostics.setup.collectLatest { setup ->
                if (!calibrationActive) return@collectLatest
                armSetupExpiry()
                appGraph.mediaDiagnostics.recordLabel(setup.message)
                // Do not compete automatically with TalkBack. A manual 'Escuchar instrucciones' remains available.
                val talkBack = getSystemService(AccessibilityManager::class.java)?.isTouchExplorationEnabled == true
                if (!talkBack && setup.step != SetupStep.IDLE) speakCoach(setup.message, keepService = true)
            }
        }
    }

    private fun transport(code: Int) {
        if (!inputGate.accept(code, true, 0, SystemClock.elapsedRealtime())) return
        if (calibrationActive) {
            // A transport PLAY/PAUSE cannot safely serve as an answer button.
            // Raw PLAY_PAUSE media keys are detected in onMediaButtonEvent instead.
            if (code == KeyEvent.KEYCODE_MEDIA_STOP) abortSetup("Comprobación detenida.")
            else appGraph.mediaDiagnostics.recordLabel("Android envió una orden de reproducción, no un botón identificable. Prueba otro gesto de los audífonos. Los controles de la notificación no sirven para calibrar respuestas.")
            return
        }
        when (code) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> runtimeCommand(RuntimeCommand.PLAY)
            KeyEvent.KEYCODE_MEDIA_PAUSE -> runtimeCommand(RuntimeCommand.PAUSE)
            KeyEvent.KEYCODE_MEDIA_STOP -> { runtime.stop(); finishService() }
            // Transport Next/Previous from the lock screen repeat; they never fabricate a graded answer.
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> runtimeCommand(RuntimeCommand.REPEAT)
        }
    }

    private fun runtimeCommand(command: RuntimeCommand) {
        if (command == RuntimeCommand.PLAY) {
            if (runtime.snapshot.value.state != LessonState.PAUSED) return
            if (!confirmedOutput()) { status("Vuelve a conectar los audífonos configurados antes de reanudar."); return }
            if (!audioFocus.request()) { status("Otra aplicación está usando el audio. Intenta reanudar después."); return }
            activateMediaAnchor()
        }
        runtime.onCommand(command)
        if (command == RuntimeCommand.STOP) finishService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action in INTERNAL_ACTIONS && intent?.getStringExtra(EXTRA_CONTROL_TOKEN) != appGraph.controlToken) {
            status("Control interno rechazado: token inválido")
            if (!running() && !calibrationActive) finishService()
            return START_NOT_STICKY
        }
        val inheritedResult = super.onStartCommand(intent, flags, startId)
        when (action) {
            ACTION_START_SESSION -> startLesson(intent!!)
            ACTION_STOP_SESSION -> { runtime.stop(); finishService() }
            ACTION_START_CALIBRATION -> startSetup()
            ACTION_STOP_CALIBRATION -> { if (!running()) { appGraph.mediaDiagnostics.cancelSetup(); calibrationActive=false; stopCoach(); finishService() } }
            ACTION_SPEAK_GUIDE -> {
                if (running()) return START_NOT_STICKY
                if (!output.hasSafePrivateOutput() || !audioFocus.request()) {
                    status("Conecta tus audífonos y comprueba el volumen multimedia.")
                    if (!calibrationActive) finishService()
                } else {
                    activateMediaAnchor()
                    val text = if (calibrationActive) appGraph.mediaDiagnostics.setup.value.message
                        else "Hola. Esta es la prueba de SIAA. Confirma que me escuchas por tus audífonos, no por el altavoz del teléfono."
                    speakCoach(text, keepService=calibrationActive, bilingual=!calibrationActive)
                }
            }
            ACTION_RUNTIME -> {
                if (!running()) { finishService(); return START_NOT_STICKY }
                val actionName=intent?.getStringExtra(EXTRA_RUNTIME_ACTION).orEmpty()
                if (actionName.startsWith("ANSWER_")) {
                    if (intent?.getLongExtra(EXTRA_TURN,-1L) == runtime.snapshot.value.turnId &&
                        intent?.getLongExtra(EXTRA_SESSION,-1L) == runtime.snapshot.value.sessionId)
                        runtime.onScreenAnswer(actionName.removePrefix("ANSWER_"))
                } else runCatching { RuntimeCommand.valueOf(actionName) }.getOrNull()?.let(::runtimeCommand)
            }
            else -> {
                if (!running() && !calibrationActive && startJob?.isActive != true) finishService()
                return inheritedResult
            }
        }
        return START_NOT_STICKY
    }

    private fun startLesson(intent: Intent) {
        if (running() || startJob?.isActive == true) { status("Ya tienes una sesión abierta. Pausa, reanuda o finaliza esa sesión."); return }
        val p=appGraph.preferenceStore.current()
        if (!p.headphones.setupCompleted || !confirmedOutput()) {
            status("Abre Audífonos: comprueba los controles y confirma dónde escuchas antes de iniciar.")
            finishService(); return
        }
        val requested=intent.getStringExtra(EXTRA_MODE)
        val mode=runCatching { SessionMode.valueOf(requested ?: p.defaultMode.name) }.getOrDefault(SessionMode.ADAPTIVE)
        if (mode in setOf(SessionMode.SPEAKING,SessionMode.READING,SessionMode.WRITING)) {
            status("Esta práctica se abre desde Más, con el teléfono en la mano."); finishService(); return
        }
        calibrationActive=false; appGraph.mediaDiagnostics.cancelSetup(); setupExpiryJob?.cancel(); stopCoach()
        startJob=serviceScope.launch {
            try {
                val init=appGraph.contentState.first { it !is ContentInitState.Loading }
                if (init is ContentInitState.Error) { status("No se pudo cargar el contenido. Reabre la aplicación."); finishService(); return@launch }
                val profile=appGraph.repository.latestDeviceProfile()
                val invalid=HeadphoneCompatibility.validate(profile)
                if (invalid != null || profile == null) { status(invalid ?: "Configura tus controles."); finishService(); return@launch }
                val capabilities=HeadphoneCompatibility.capabilities(p.headphones.mode,profile)
                if (!capabilities.supportsBinary) { status("Este perfil necesita el modo de un solo control. Actívalo en Audífonos."); finishService(); return@launch }
                val preflight=appGraph.audioPreflight.run(requirePrivateOutput=true)
                if (!preflight.ready || !confirmedOutput()) { status(preflight.message.ifBlank { "La salida cambió. Comprueba tus audífonos." }); finishService(); return@launch }
                if (!audioFocus.request()) { status("Otra aplicación está usando el audio. Intenta iniciar después."); finishService(); return@launch }
                activeDeviceProfile=profile; inputGate.reset(); ownsSession=true; sawActiveSession=false
                activateMediaAnchor()
                runtime.start(SessionConfig(mode=mode, maxItems=p.maxItems, targetDurationMinutes=p.targetDurationMinutes,
                    learningGoal=p.learningGoal, englishVariety=p.englishVariety, intensity=p.intensity,
                    announceControls=p.announceControls, feedbackExplanations=p.feedbackExplanations,
                    speechRate=p.speechRate, capabilities=capabilities, deviceProfileId=profile.id,
                    headphonePreferences=p.headphones,
                    policy=SessionPolicy(binaryResponseTimeoutMs=p.headphones.responseSeconds*1000L,
                        selfAssessmentTimeoutMs=p.headphones.responseSeconds*1000L,maxTimeoutRetries=1)))
            } catch (ce: CancellationException) { throw ce }
            catch (t: Throwable) { status("No se pudo iniciar el audio: ${t.message}"); finishService() }
        }
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (finishing) return
        val text = if(calibrationActive) "Comprobación de audífonos, sin nota" else runtime.snapshot.value.message
        val n = notification(text)
        if (startInForegroundRequired) startForeground(NOTIFICATION_ID,n)
        else getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID,n)
    }

    private fun startSetup() {
        if (running() || startJob?.isActive == true) { status("Finaliza tu sesión antes de configurar otros controles."); return }
        if (!output.hasSafePrivateOutput() || !audioFocus.request()) {
            status("Conecta audífonos antes de comprobar tus controles."); finishService(); return
        }
        calibrationActive=true; activeDeviceProfile=null; inputGate.reset()
        appGraph.mediaDiagnostics.beginSetup()
        activateMediaAnchor()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification("Comprobación de audífonos, sin nota"))
    }

    private fun armSetupExpiry() {
        setupExpiryJob?.cancel()
        setupExpiryJob=serviceScope.launch {
            delay(180_000L)
            if(calibrationActive) abortSetup("La comprobación se cerró por inactividad. Puedes iniciarla de nuevo sin perder tu progreso.")
        }
    }
    private fun stopCoach() { coachGeneration++; coachJob?.cancel(); appGraph.speech.stop() }
    private fun speakCoach(text: String, keepService: Boolean, bilingual: Boolean = false) {
        stopCoach()
        val generation = coachGeneration
        coachJob=serviceScope.launch {
            try {
                if (output.hasSafePrivateOutput()) {
                    appGraph.speech.speak(text,"es-PE",appGraph.preferenceStore.current().speechRate)
                    if (bilingual && output.hasSafePrivateOutput()) appGraph.speech.speak("Hello. Ready to learn.","en-US",appGraph.preferenceStore.current().speechRate)
                }
            } catch (ce: CancellationException) { throw ce }
            catch (t: Throwable) { status("No se pudo reproducir la guía. Comprueba las voces en Ajustes.") }
            finally { if (generation == coachGeneration && !keepService && !calibrationActive && !running() && startJob?.isActive != true) finishService() }
        }
    }
    private fun confirmedOutput(): Boolean = output.hasSafePrivateOutput() &&
        AudioOutputGuard.routeSignature(this) == appGraph.preferenceStore.current().headphones.confirmedRoute
    private fun routeChanged() {
        stopCoach()
        if (calibrationActive) { abortSetup("La conexión cambió. Vuelve a iniciar la comprobación de audífonos."); return }
        startJob?.cancel()
        if (running()) { runtime.pauseForRouteChange(); player.pause(); audioFocus.abandon() }
        else finishService()
    }
    private fun abortSetup(message: String) {
        calibrationActive=false; appGraph.mediaDiagnostics.cancelSetup()
        stopCoach(); status(message); finishService()
    }
    private fun status(text: String) {
        appGraph.mediaDiagnostics.recordLabel(text)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID,notification(text))
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (ownsSession && running() && player.isPlaying) return // Closing the UI is not stopping an audio lesson.
        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() {
        stopCoach(); startJob?.cancel(); setupExpiryJob?.cancel()
        if (ownsSession && running()) runtime.stop()
        if (calibrationActive) appGraph.mediaDiagnostics.cancelSetup()
        calibrationActive=false
        if (::output.isInitialized) output.unregister()
        if (::audioFocus.isInitialized) audioFocus.abandon()
        if (!running()) appGraph.speech.stop()
        session?.release(); session=null
        if (::player.isInitialized) player.release()
        serviceScope.cancel(); super.onDestroy()
    }
    private fun activateMediaAnchor() {
        if (player.mediaItemCount == 0) {
            val uri=Uri.parse("android.resource://$packageName/${R.raw.siaa_silence}")
            player.setMediaItem(MediaItem.Builder().setUri(uri).setMediaMetadata(
                MediaMetadata.Builder().setTitle("SIAA").setArtist("Aprende inglés con audífonos").build()).build())
            player.repeatMode=Player.REPEAT_MODE_ONE; player.volume=0f; player.prepare()
        }
        player.play()
    }
    private fun updateMetadata(message: String) {
        val item=player.currentMediaItem ?: return
        if (item.mediaMetadata.artist?.toString() == message) return
        player.replaceMediaItem(0,item.buildUpon().setMediaMetadata(MediaMetadata.Builder().setTitle("SIAA").setArtist(message).build()).build())
    }
    private fun finishService() {
        finishing=true
        setupExpiryJob?.cancel()
        if (calibrationActive) appGraph.mediaDiagnostics.cancelSetup()
        calibrationActive=false; ownsSession=false
        if (::player.isInitialized) { player.pause(); player.clearMediaItems() }
        if (::audioFocus.isInitialized) audioFocus.abandon()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID,"Sesiones de inglés",NotificationManager.IMPORTANCE_LOW))
    }
    private fun openAppIntent() = PendingIntent.getActivity(this,10,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun commandIntent(command: String, requestCode: Int): PendingIntent = PendingIntent.getService(this,requestCode,
        Intent(this,SiaaPlaybackService::class.java).apply {
            action=ACTION_RUNTIME; putExtra(EXTRA_RUNTIME_ACTION,command); putExtra(EXTRA_CONTROL_TOKEN,appGraph.controlToken)
        },PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun notification(text: String): Notification {
        val paused=runtime.snapshot.value.state == LessonState.PAUSED
        return NotificationCompat.Builder(this,CHANNEL_ID).setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("SIAA · inglés con audífonos").setContentText(text).setContentIntent(openAppIntent())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .addAction(android.R.drawable.ic_media_pause,if(paused) "Reanudar" else "Pausar",commandIntent(if(paused) "PLAY" else "PAUSE",12))
            .addAction(android.R.drawable.ic_media_previous,"Repetir",commandIntent("REPEAT",13))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,"Finalizar",commandIntent("STOP",14))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setOnlyAlertOnce(true).setOngoing(!paused).build()
    }
    companion object {
        const val ACTION_START_SESSION="com.siaa.app.action.START_SESSION"
        const val ACTION_STOP_SESSION="com.siaa.app.action.STOP_SESSION"
        const val ACTION_START_CALIBRATION="com.siaa.app.action.START_CALIBRATION"
        const val ACTION_STOP_CALIBRATION="com.siaa.app.action.STOP_CALIBRATION"
        const val ACTION_SPEAK_GUIDE="com.siaa.app.action.SPEAK_GUIDE"
        const val ACTION_RUNTIME="com.siaa.app.action.RUNTIME"
        const val EXTRA_RUNTIME_ACTION="runtime_action"
        const val EXTRA_TURN="turn_id"
        const val EXTRA_SESSION="session_id"
        const val EXTRA_MODE="mode"
        const val EXTRA_MAX_ITEMS="max_items"
        const val EXTRA_TARGET_DURATION_MINUTES="target_duration_minutes"
        const val EXTRA_LEARNING_GOAL="learning_goal"
        const val EXTRA_ENGLISH_VARIETY="english_variety"
        const val EXTRA_INTENSITY="intensity"
        const val EXTRA_ANNOUNCE_CONTROLS="announce_controls"
        const val EXTRA_FEEDBACK_EXPLANATIONS="feedback_explanations"
        const val EXTRA_SPEECH_RATE="speech_rate"
        const val EXTRA_CONTROL_TOKEN="control_token"
        private const val CHANNEL_ID="siaa_session"
        private const val NOTIFICATION_ID=1001
        private val TERMINAL=setOf(LessonState.IDLE,LessonState.SESSION_END,LessonState.ERROR)
        private val INTERNAL_ACTIONS=setOf(ACTION_START_SESSION,ACTION_STOP_SESSION,ACTION_START_CALIBRATION,ACTION_STOP_CALIBRATION,ACTION_RUNTIME,ACTION_SPEAK_GUIDE)
    }
}
