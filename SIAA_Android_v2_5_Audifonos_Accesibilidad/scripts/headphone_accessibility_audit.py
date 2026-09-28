#!/usr/bin/env python3
"""Source-wiring gate. Does not claim Android execution, WCAG compliance or physical headset compatibility."""
from pathlib import Path
import json
import sys
ROOT=Path(__file__).resolve().parents[1]
checks=[]
def contains(label,path,*tokens):
    p=ROOT/path
    text=p.read_text(encoding='utf-8') if p.exists() else ''
    checks.append((label,all(token in text for token in tokens)))
contains('guided ungraded setup','core/runtime/src/main/kotlin/com/siaa/core/runtime/HeadphoneSetupFlow.kt','CONFIRM_PRIMARY','REHEARSAL','successfulChecks','fun skip')
contains('capabilities and clamps','core/model/src/main/kotlin/com/siaa/core/model/HeadphoneInteraction.kt','SINGLE_SWITCH','coerceIn(12, 90)','coerceIn(2, 10)','codes.distinct()')
contains('shared spoken protocol','core/runtime/src/main/kotlin/com/siaa/core/runtime/HeadphoneProtocol.kt','REPEAT','SLOWER','PAUSE','@Synchronized','repeatCount != 0')
contains('runtime scanning and explicit screen answers','core/runtime/src/main/kotlin/com/siaa/core/runtime/LessonRuntime.kt','fun startScanning','fun onScreenAnswer','PauseReason.INACTIVITY','supportedSpeechRate','scanChoice','answerText','excludedPauseMs')
contains('persistent preferences','app/src/main/java/com/siaa/app/UserPreferences.kt','headphones_v25','headphoneJson','readHeadphones')
contains('setup tied to current output','app/src/main/java/com/siaa/app/MainViewModel.kt','preflight.routeSignature != route','setupCompleted = true','HeadphoneCompatibility.validate')
contains('headset intent de-duplication','app/src/main/java/com/siaa/app/media/EarbudCommandRouter.kt','event.repeatCount != 0','return null // Do not invent','KEYCODE_MEDIA_STOP')
contains('safe transport and calibrated raw events','app/src/main/java/com/siaa/app/media/SiaaPlaybackService.kt','ForwardingPlayer','inputGate.accept','confirmedOutput()','return true // Never fall through','onUpdateNotification','controller.isTrusted','startInForegroundRequired')
contains('service cleanup and manual-only resume','app/src/main/java/com/siaa/app/media/SiaaPlaybackService.kt','armSetupExpiry','stopCoach','generation == coachGeneration','pauseForRouteChange','audioFocus.abandon','EXTRA_TURN','EXTRA_SESSION')
contains('output connection observer','core/audio/src/main/java/com/siaa/core/audio/AudioOutputGuard.kt','AudioDeviceCallback','onAudioDevicesRemoved','previousSignature != now','ACTION_AUDIO_BECOMING_NOISY')
contains('preflight timeout and cleanup','core/audio/src/main/java/com/siaa/core/audio/AudioPreflight.kt','withTimeoutOrNull(6_000L)','finally { tts.shutdown() }','getStreamVolume','routeBefore == routeAfter')
contains('offline voices selected explicitly','core/audio/src/main/java/com/siaa/core/audio/OfflineTtsVoices.kt','!it.isNetworkConnectionRequired','KEY_FEATURE_NOT_INSTALLED')
contains('tts uses selected voice and bounded playback','core/audio/src/main/java/com/siaa/core/audio/AndroidTtsSpeechPort.kt','withTimeout','tts.setVoice(voice)','CONTENT_TYPE_SPEECH')
contains('first-run route and readable navigation','app/src/main/java/com/siaa/app/ui/SiaaApp.kt','Tu primera sesión','AppTab.HEADPHONES','Terminar bienvenida','BackHandler')
contains('accessible semantic components','app/src/main/java/com/siaa/app/ui/SiaaApp.kt','heightIn(min=56.dp)','heading()','role=Role.Switch','stateDescription','liveRegion=LiveRegionMode.Polite','LazyColumn')
contains('UI controls and fallback transcript','app/src/main/java/com/siaa/app/ui/SiaaApp.kt','ANSWER_A','ANSWER_B','r.captions','Contraste alto','Confirm','firstLabel','imePadding()')
contains('data restoration invalidates output trust','app/src/main/java/com/siaa/app/MainActivity.kt','setupCompleted=false,confirmedRoute=""','canChangeData','EXTRA_TURN','requestNotificationPermissionIfNeeded')
contains('instrumentation authored','app/src/androidTest/java/com/siaa/app/ui/AccessibilityComponentsInstrumentedTest.kt','fontScale=2f','SemanticsProperties.Heading','assertHeightIsAtLeast(56.dp)')
contains('JVM behavior regression authored','tools/headphones_accessibility_smoke.kt','TWO_SCAN_ROUNDS_UNGRADED_PAUSE','SCREEN_ANSWER_NOT_CURRENT_SCAN_CHOICE','DIRECT_SILENCE_PAUSES_WITHOUT_GRADING')
contains('first-time user guide shipped','docs/GUIA_USUARIO_V25.md','Comprobar conexión y voces','un solo control','No es un APK')
contains('hardware acceptance matrix shipped','docs/QA_AUDIFONOS_ACCESIBILIDAD_V25.md','PENDIENTE','TalkBack','pantalla bloqueada')
for label,ok in checks: print(('PASS' if ok else 'FAIL'),label)
print(f'Source wiring: {sum(ok for _,ok in checks)}/{len(checks)}; NOT device/accessibility certification.')
sys.exit(0 if all(ok for _,ok in checks) else 1)
