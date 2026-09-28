# Verificación realizada — SIAA 2.5.0-dev

## Dictamen de alcance

El paquete contiene cambios implementados en código, pruebas JVM ejecutadas y documentación para una primera configuración comprensible. **No es una aplicación Android compilada y aprobada.** La ejecución con Android, audífonos físicos y TalkBack sigue pendiente; no se declara certificación de accesibilidad ni compatibilidad universal.

Base utilizada: `SIAA_Android_v2_4_Working_Snapshot_2026-09-24.zip`. La huella del archivo de entrada, resultados por comando y diferencias de fuentes están en `validation/v25/test_results.json` y `source_changes.json`.

## Resultados ejecutados

| Comprobación | Resultado | Qué acredita |
|---|---|---|
| Nuevo protocolo de audífonos | PASS: 12 grupos JVM | Capacidades, calibración, filtros, respuestas, repeticiones, pausa y reanudación del runtime |
| Regresiones de runtime anteriores | PASS | Stop, pérdida simulada de ruta, gates, timeout no calificado, restauración y finalización |
| Smoke del runtime completo | PASS | Secuencia de respuestas y actualización del estado del alumno en JVM |
| Algoritmo | PASS | Ejecución del smoke previo sin Android |
| Generador de contenido | PASS | Compilación/ejecución de su smoke previo |
| Deletreo | PASS | Regresión del runtime de spelling |
| Placement/producción v2.4 | PASS | Smoke numérico de las funciones heredadas, no calidad fonética humana |
| Auditoría de integración nueva | PASS: 21/21 | Presencia y conexiones de código esperadas; no ejecución Android |
| Sintaxis Kotlin | PASS | Archivos .kt/.kts analizados por PSI; no comprobación completa de tipos Android |
| Estructura y contenido | PASS | Referencias y checksums coherentes |
| Profundidad/completitud de contenido | PASS | Reglas estructurales de cobertura, no dominio real de inglés |
| Patrones lingüísticos | PASS | Reglas automáticas; no revisión humana exhaustiva |
| Banco de audio | PASS | 3.954 assets indexados y checksums; cobertura según auditor existente |
| Señal de audio crítica | PASS | 123 assets revisados por el auditor de señal; no escucha humana |
| Migración Room | PASS | Smoke SQL de 16 sentencias, no test instrumentado en Android |
| Consistencia del paquete | PASS | App 2.5.0-dev separada de contenido/audio 2.4.0 |

Los archivos `.log` y `.exit` permiten distinguir cada comando ejecutado. El perfil CEFR-J es informativo: encontró 2.825 de 2.882 entradas y 2.516 coincidencias exactas de nivel entre las encontradas; esto no se convierte en una declaración de calibración lingüística.

## Casos de la batería nueva

Se comprobaron perfiles con uno/dos/tres controles y perfiles inválidos; ensayo y confirmaciones guiadas; repetición de teclas y prioridad de Stop/Pause; límites de preferencias e instrucciones; respuesta única con un control; elección explícita desde pantalla; autoevaluación de tres opciones con un control; pausa por silencio sin cambiar dominio; repetir/más despacio sin fabricar respuestas; cancelación por ruta, reanudación y Stop; y pausa tras dos recorridos sin elegir.

Los tests de ruta usan el runtime y puertos simulados. No simulan todas las peculiaridades de Bluetooth, firmware, AudioManager o MediaSession de un teléfono real. La latencia de scan y de respuesta asistida queda nula, en lugar de degradar la estimación de rapidez del alumno.

## Correcciones descubiertas durante el trabajo

`exercises.json` y `lexemes.json` del ZIP de entrada no coincidían con los hashes declarados en su manifest. Se verificó que ambos archivos de contenido eran idénticos a los del ZIP y se corrigió únicamente el manifest. La evidencia está en `inherited_checksum_repairs.json`. Se mantuvieron sin alteración KCs, relaciones y lecciones de la base.

Un test antiguo esperaba completar el cambio a una actividad de recuperación mental en tres segundos, por debajo de la suma del tiempo de recuperación y los nuevos intervalos. Se amplió su espera a ocho segundos, sin retirar las aserciones de aprendizaje. El test del timeout antiguo ahora selecciona explícitamente la política sin pausa; un test nuevo verifica el comportamiento por defecto de pausa sin nota.

Los logs de compilación conservan dos avisos anteriores del motor de placement por etiquetas `mapNotNull` ambiguas. No se afirman cero advertencias del compilador.

## No ejecutado o bloqueado

Se intentó `./gradlew :app:assembleDebug`. El wrapper informó que falta `gradle-wrapper.jar`; tampoco hay SDK, adb ni Gradle instalado en este entorno. El log exacto está en `android_build_attempt.log`. La JVM disponible es OpenJDK 21; el proyecto Android recomienda JDK 17 para su toolchain.

Se añadieron tres pruebas de componentes accesibles y tres casos adicionales del router Android, pero no se ejecutaron. No se hicieron capturas Android, mediciones reales de contraste ni pruebas de TalkBack, pantalla bloqueada, llamadas, batería, desconexión física o modo avión. Todos los casos de `QA_AUDIFONOS_ACCESIBILIDAD_V25.md` permanecen PENDIENTES.

No se publicaron archivos en GitHub ni se ejecutaron workflows remotos. El banco conserva cero entradas humanas activadas. No se completó revisión lingüística humana ni estudio con usuarios, y no se atribuye eficacia demostrada o nivel C2 a la aplicación.

## Integridad de entrega

El manifiesto `FILES_SHA256.txt` registra los archivos relevantes. `scripts/verify_release.py` comprueba hashes y documentos de referencia; es un control de integridad, no de ejecución Android. La entrega se vuelve a abrir como ZIP y verificar en una extracción nueva antes de compartirla. Se adjunta una huella SHA-256 del ZIP junto al archivo de descarga.
