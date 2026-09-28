# SIAA — Aprendizaje Auditivo, Audífonos y Accesibilidad

**SIAA (Sistema Interactivo de Aprendizaje Auditivo)** es una aplicación Android nativa construida con Kotlin y Jetpack Compose diseñada para practicar inglés durante trayectos y desplazamientos diarios, con el teléfono guardado y usando los controles físicos de los audífonos (auriculares).

## Características Principales

1. **Aprendizaje Auditivo sin Pantalla (Manos Libres)**:
   - Actividades auditivas organizadas por niveles del Marco Común Europeo de Referencia (MCER A1 a C2).
   - Modalidades de práctica: **Escucha**, **Fonología/Discriminación auditiva**, **Frases y expresiones comunicativas**, o **Mixto**.
   - Locución de alternativas habladas con repetición automática tras pausas de silencio.
   - Banco de audios offline integrado en la aplicación.

2. **Control Adaptativo de Audífonos y Auriculares**:
   - Detección en vivo de rutas de audio: audífonos Bluetooth (A2DP), auriculares con cable o advertencia de altavoz privado.
   - Enrutador de eventos de hardware de botones multimedia (`EarbudCommandRouter`) que procesa acciones de reproducción, siguiente, anterior y pausa, evitando pulsaciones repetidas por teclas sostenidas.
   - Soporte para **Modo 1 botón** (locución cíclica de opciones y pulsación única de confirmación) y **Modo 3 controles** (gestos dedicados).
   - Asistente guiado de configuración con prueba de sonido privado, detección de botones y **ensayo sin nota** (prueba interactiva sin impacto en la puntuación).

3. **Accesibilidad Integral**:
   - Elementos interactivos con área de contacto táctil accesible (mínimo 56 dp).
   - Semántica explícita de encabezados accesibles y compatibilidad total con TalkBack (silenciado inteligente de prompts automatizados para no interrumpir el lector de pantalla).
   - Modo de Alto Contraste visual seleccionable con combinación de colores optimizada para baja visión.
   - Transcripción y texto visible en pantalla en todo momento.

4. **Persistencia y Progreso Local**:
   - Registro de perfiles de dispositivos calibrados.
   - Historial de sesiones con conteo de aciertos, duración y nivel trabajado.

## Arquitectura y Tecnologías

- **Lenguaje**: Kotlin 2.2 con Jetpack Compose y Material 3 (M3).
- **Audio y Multimedia**: Android `MediaPlayer`, `TextToSpeech` (TTS) y `AudioManager` con manejo estricto de foco de audio y evento de desconexión ruidosa (`ACTION_AUDIO_BECOMING_NOISY`).
- **Nivel de SDK**: Compile SDK 36, Min SDK 26, Target SDK 36.
- **Herramienta de Construcción**: Gradle con Kotlin DSL y catálogo de versiones `libs.versions.toml`.
