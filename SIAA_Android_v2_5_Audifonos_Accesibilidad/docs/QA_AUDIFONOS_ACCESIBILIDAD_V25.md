# SIAA 2.5.0-dev — aceptación Android y accesibilidad

**Estado de las pruebas de esta matriz: PENDIENTE.** Son procedimientos preparados, no resultados aprobados. Las pruebas JVM ejecutadas se describen por separado en `RESULTADOS_V25.md`.

## Requisitos y evidencia

Compilar aplicación y APK de pruebas con el SDK indicado en `app/build.gradle.kts`. Registrar revisión/código fuente, SHA-256 del APK, teléfono, Android, motor TTS y voces, fabricante/modelo/firmware de audífonos, modo de controles y configuración de ahorro de batería. No compartir MAC, cuentas, tokens ni transcripciones personales sin consentimiento.

Usar al menos un dispositivo API 26–30, uno API 33–34 y uno API 35–37 cuando estén disponibles. Repetir con cable/USB, Bluetooth y controles de uno/dos/tres comandos; incluir un equipo que sólo entregue controles de transporte. La matriz es una propuesta de cobertura, no una lista de modelos garantizados.

| Caso | Procedimiento | Resultado esperado | Estado |
|---|---|---|---|
| A01 | Instalar limpio y abrir sin audífonos | Inicio explica tres pasos; no inicia audio ni pide micrófono | PENDIENTE |
| A02 | Volumen multimedia en cero; falta una voz offline | Prueba no lista; ofrece instrucciones para corregir | PENDIENTE |
| A03 | Probar salida con voz española e inglesa; cambiar dispositivo antes de guardar | No acepta el resultado antiguo | PENDIENTE |
| A04 | Calibrar un control; repetir otro código en la confirmación | No avanza hasta coincidencia; ensayo sin evidencia pedagógica | PENDIENTE |
| A05 | Calibrar dos/tres controles, intentando duplicarlos | Rechaza funciones duplicadas; AUTO elige protocolo adecuado | PENDIENTE |
| A06 | Usar sólo transport Play/Pause/Next desde notificación | No certifica un botón de respuesta inexistente | PENDIENTE |
| A07 | Responder A/B y sí/dudas/no con un solo control | Lee alternativas completas; acepta durante el silencio; guarda una sola respuesta | PENDIENTE |
| A08 | Pulsar al anunciar opción, mantener botón, recibir KEY_UP o duplicado | No registra una respuesta anticipada o duplicada | PENDIENTE |
| A09 | Elegir Repetir/Más despacio; luego contestar | No inventa respuesta por pedir ayuda; no penaliza latencia de asistencia | PENDIENTE |
| A10 | Ignorar dos recorridos de opciones | Pausa por inactividad; no registra error de conocimiento | PENDIENTE |
| A11 | Contestar desde pantalla durante scan A eligiendo B | Registra B, nunca la opción que casualmente estaba en voz | PENDIENTE |
| A12 | Pantalla bloqueada 10–30 min, teléfono guardado | Voz y comandos siguen respondiendo; recoger batería y eventos | PENDIENTE |
| A13 | Play/Pause/Next desde pantalla bloqueada | Reanuda/pausa/repite; no califica una respuesta accidental | PENDIENTE |
| A14 | Desconectar cable/Bluetooth, multipunto, conectar otro dispositivo | Se pausa; no reanuda sola; comprobar posibles fugas al altavoz | PENDIENTE |
| A15 | Llamada entrante y otra app que toma AudioFocus | Pausa/interrumpe sin perder progreso; reanudación expresa | PENDIENTE |
| A16 | Detener desde notificación, salir, reiniciar proceso | No queda servicio huérfano; estado/progreso coherentes | PENDIENTE |
| A17 | TalkBack, navegación por encabezados y controles | Etiquetas/estado comprensibles, una acción por interruptor, sin guía automática superpuesta | PENDIENTE |
| A18 | Letra 200 %, pantalla pequeña, horizontal, teclado abierto | Sin botones cortados, textos esenciales accesibles al desplazar; documentar capturas | PENDIENTE |
| A19 | Contraste alto claro/oscuro y daltonismo | Medir contrastes reales; no depender exclusivamente del color | PENDIENTE |
| A20 | Restaurar copia en otro teléfono y cambiar contenido | Confirmación previa; invalida aprobación de audífonos; progreso íntegro | PENDIENTE |
| A21 | Modo avión con voces instaladas | Sesión y guía funcionan sin red; no se elige voz que requiera conexión | PENDIENTE |
| A22 | Abrir/cancelar/repetir prueba de voz muy rápido; abandonar calibración | No se superponen coaches; limpieza por timeout/Stop; no inicia lección accidentalmente | PENDIENTE |

## Tests instrumentados incluidos

`EarbudCommandRouterInstrumentedTest`: mapeo, perfil calibrado, KEY_UP, teclas desconocidas, repeticiones al mantener pulsado, teclas sin calibrar, prioridad Stop/Pause ante perfiles corruptos.

`AccessibilityComponentsInstrumentedTest`: botones de 56 dp con acción/etiqueta, encabezado semántico, interruptor con etiqueta larga y fuente al 200 %. No sustituyen la navegación de todo el producto con TalkBack ni una auditoría de contraste.

```sh
./scripts/bootstrap_gradle.sh :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest
./scripts/device_qa_collect.sh
```

Ejecutar también la migración Room instrumentada y los protocolos D01–D18 históricos. Ningún script de presencia de texto en fuentes acredita el funcionamiento real de Android.
