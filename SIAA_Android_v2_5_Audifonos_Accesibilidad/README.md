# SIAA Android — audífonos y accesibilidad

**Aplicación 2.5.0-dev · Contenido 2.4.0 · Paquete de desarrollo, no APK validado.**

SIAA está diseñado para aprender inglés con audífonos durante un viaje, con el teléfono guardado y sin hablar en voz alta. Esta versión parte del snapshot v2.4 entregado el 24 de septiembre y mejora su configuración y uso por personas que no conocen el código.

Empieza por [START_HERE](START_HERE.md). Para el uso diario: [guía de primera sesión](docs/GUIA_USUARIO_V25.md). Para revisar lo comprobado: [resultados](docs/RESULTADOS_V25.md) y [matriz Android pendiente](docs/QA_AUDIFONOS_ACCESIBILIDAD_V25.md).

## Cambios principales

Configuración de audífonos guiada con confirmación y ensayo sin nota. Adaptación a uno, dos o tres controles. Selección por opciones habladas con un solo control, con repetición de las alternativas completas. Gestos nombrados por la persona, tiempos configurables, pausa no calificada por silencio y acciones separadas para pausar, repetir y responder.

Inicio/Audífonos/Más como navegación básica. Instrucciones comprensibles, campos etiquetados, encabezados accesibles, controles principales de al menos 56 dp, contraste alto opcional, texto del audio, listas virtualizadas y soporte del tamaño de letra del sistema. La guía automática de configuración se silencia cuando la exploración táctil está activa; queda disponible bajo petición.

Preflight de salida candidata, volumen, estabilidad y voces declaradas offline; confirmación de escucha por la persona. Pausa ante cambios de salida y pérdida de foco, sin reanudar automáticamente. Se conservan Stop/Pause, se filtran KEY_UP/repeticiones, se separan transport/lockscreen de respuestas y se vinculan respuestas de pantalla al turno y la sesión.

## Estado real

El núcleo Kotlin y los tests sin Android se compilan/ejecutan; consulta los logs incluidos. Se añadieron tests instrumentados de controles y componentes accesibles, pero **no se ejecutaron** en este entorno. No hay SDK/adb, Gradle instalado ni wrapper JAR; el intento de compilación y su bloqueo se conservan en `validation/v25/android_build_attempt.log`.

Por tanto, esta entrega no acredita compatibilidad universal de audífonos, pruebas TalkBack/OEM, un APK estable, evaluación humana del contenido, eficacia pedagógica ni dominio certificado C2. Detectar Bluetooth conectado no prueba por sí mismo la ruta física privada del sonido. La confirmación de escucha no debe omitirse.

El contenido del snapshot se conserva: 3.787 componentes de conocimiento, 10.739 ejercicios estáticos y 2.882 entradas léxicas, más su catálogo de práctica y banco de audio. Se repararon dos checksums desactualizados de ese snapshot sin modificar las lecciones. El banco sigue siendo sintético.

## Desarrollo y validación

JDK 17 recomendado para el build Android; configuración heredada: SDK compile 37/target 36, Gradle 9.6.0 y AGP 9.4.0. La primera preparación requiere internet en tu equipo. Los scripts de bootstrap descargan y verifican el wrapper; no incluyen el SDK.

```sh
./scripts/bootstrap_gradle.sh :app:assembleDebug
./scripts/bootstrap_gradle.sh :app:connectedDebugAndroidTest
python scripts/validate_project.py
python scripts/headphone_accessibility_audit.py
./scripts/smoke_headphones_accessibility.sh
```

El smoke JVM requiere `kotlinc`, `kotlin` y el JAR de coroutines incluido en la distribución Kotlin. La auditoría estática comprueba integración, no funcionamiento Android ni certificación de accesibilidad.

Estructura: `app/`, `core/{model,algorithm,runtime,data,audio,content}`, `tools/`, `scripts/`, `docs/`, `validation/v25/` y `.github/workflows/`. La arquitectura completa y referencias previas se conservan; para la interacción nueva, prevalece [AUDIFONOS_ARQUITECTURA_V25](docs/AUDIFONOS_ARQUITECTURA_V25.md).

No se cambia la licencia general del proyecto ni las atribuciones de las referencias heredadas. No se ha subido esta revisión a un repositorio remoto ni ejecutado sus workflows.
