# Diseño de interacción y cambios v2.5

Aplicación 2.5.0-dev; contenido y banco de audio 2.4.0 preservados. Esta capa reemplaza el flujo de configuración/controles de v2.3; las guías históricas no describen necesariamente la interfaz actual.

## Contrato de controles

El firmware decide qué eventos envía. La calibración guiada observa KEY_DOWN de comandos multimedia, requiere confirmación, evita funciones duplicadas y termina con un ensayo sin nota. No deduce doble/triple toque por intervalos de tiempo. `MediaInputGate` sólo suprime repeticiones y doble entrega del mismo código durante 280 ms usando reloj monotónico.

`HeadphoneCompatibility` decide capacidades: AUTO usa scan con menos de tres controles; DIRECT mantiene los filtros de elegibilidad de ejercicios; SINGLE_SWITCH fuerza scan. `HeadphoneProtocol` comparte instrucciones habladas y visibles. Los nombres elegidos por la persona se guardan en preferencias, junto con tiempos y opciones visuales.

Las llamadas ordinarias a Player/transport desde MediaSession se separan de eventos físicos identificables: Play reanuda, Pause pausa, Stop termina; Next/Previous repiten. No se aceptan para calibrar una respuesta. Algunos equipos Bluetooth que sólo exponen transport serán incompatibles con la calibración actual; no se inventa un soporte inexistente.

## Estado y evidencia

Las respuestas sólo se aceptan con el turno abierto. En scan, primero se lee la etiqueta y alternativa, después una señal y 250 ms de separación, y finalmente se abre la ventana. Hay dos recorridos y después pausa no calificada. La elección en pantalla transporta sessionId y turnId y conserva explícitamente A/B, no reutiliza la posición del scan.

Repetir, más despacio y controles usan una rama distinta a respuestas; invalidan el trabajo de voz anterior, cancelan el timeout/scan y vuelven a presentar la pregunta. La latencia de respuestas asistidas y de scan se guarda nula para no interpretarla como automaticidad del idioma. La pausa se resta del presupuesto temporal de la sesión. El modo directo conserva la opción de timeout no calificado, pero por defecto pausa tras los reintentos.

Los enunciados hablados se reflejan en una ventana de hasta 24 intervenciones del turno. No es transcripción ASR y no registra audio del entorno. El resumen distingue la evidencia de preguntas objetivas de la autoevaluación; tampoco convierte la puntuación de ASR en una certificación fonética.

## Salida, voz y ciclo de vida

Preflight comprueba salida candidata, estabilidad de tipo/nombre entre inicio y fin, volumen no nulo y voces TTS declaradas offline/instaladas. La persona confirma la prueba real. `OfflineTtsVoices` se comparte entre preflight y reproducción; se selecciona con `setVoice`, no se cambia después mediante `setLanguage` a una voz de red. La inicialización y cada utterance tienen timeout y limpieza al cancelar.

`AudioOutputGuard` observa cambios de dispositivos y ACTION_AUDIO_BECOMING_NOISY. La sesión pausa ante cambios de ruta o pérdida de foco, y sólo reanuda por petición expresa con salida confirmada. No demuestra la ruta real de cada sonido: una salida A2DP puede ser un parlante. El identificador local hash deriva de tipo/nombre, no de MAC, y puede colisionar entre equipos con el mismo nombre. Es una barrera de coherencia, no una garantía de privacidad acústica.

`SiaaPlaybackService` coordina anchor Media3, guía, sesión, notificación y limpieza. La guía usa generaciones para impedir que la cancelación de una prueba anterior cierre una nueva. La comprobación expira tras tres minutos sin cambio de paso. No hay reproducción automática al recuperar foco o reconectar. El comportamiento final del servicio/lockscreen requiere pruebas Android.

## Accesibilidad

Inicio/Audífonos/Más como navegación base. Ajustes y funciones complejas quedan en Más. Textos sin KCs ni nombres de algoritmos en el recorrido inicial. Encabezados semánticos, interruptores como una sola acción etiquetada, estados y avisos accesibles, controles principales de al menos 56 dp, listas virtualizadas, tamaño de letra del sistema, teclado con insets y contraste alto opcional.

Con exploración táctil activa, no se ejecuta guía de configuración automática; existe botón manual. No se ha probado con TalkBack físico ni se certifica WCAG. Las confirmaciones de restauración, rollback y borrado son independientes de la acción inmediata de detener el audio.

## Fuentes técnicas consultadas

Android Developers: API defaults de accesibilidad Compose; Semantics; Background playback with MediaSessionService; Managing audio focus; AudioDeviceCallback; TextToSpeech/Voice. Consulta: 25 de septiembre de 2026.

- https://developer.android.com/develop/ui/compose/accessibility/api-defaults
- https://developer.android.com/develop/ui/compose/accessibility/semantics
- https://developer.android.com/media/media3/session/background-playback
- https://developer.android.com/media/optimize/audio-focus
- https://developer.android.com/reference/android/media/AudioDeviceCallback
- https://developer.android.com/reference/android/speech/tts/TextToSpeech

## Alcance no resuelto

No hay APK ni compilación Android aprobada en esta entrega. No se añadieron grabaciones humanas, evaluación fonética objetiva, calibración científica con participantes ni garantía de curso integral C2. El snapshot de contenido anterior conserva sus límites. No se ha publicado una revisión remota ni ejecutado GitHub Actions; los workflows sólo están incluidos como código.
