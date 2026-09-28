# Notas 2.5.0-dev · 25 de septiembre de 2026

Base: `SIAA_Android_v2_4_Working_Snapshot_2026-09-24.zip`.

Se implementaron protocolo por capacidades, asistente de audífonos con doble confirmación y ensayo, scan de alternativas completas con un solo control, ventana explícita de respuesta, controles de repetición/velocidad/ayuda, pausa por inactividad sin calificación, exclusión de la latencia motora asistida y del tiempo pausado del presupuesto de sesión.

Se simplificó Inicio/Audífonos/Más y se añadieron semántica accesible, tamaño mínimo de controles, opciones de contraste/texto, nombres de gestos, tiempos y navegación de regreso. Los apartados de voz/lectura/escritura siguen separados del modo de viaje. Se solicitan permisos en contexto, se confirman cambios de datos y se invalida la aprobación de salida al restaurar copias.

Se revisaron servicio, transport/lockscreen, rutas, foco, expiración de la calibración y generaciones de guía. Se comparten voces offline entre diagnóstico y reproducción, se limita la espera TTS y se exige confirmación escuchada. No se usa micrófono para detectar ruido ni se aumenta automáticamente el volumen.

Se añaden 12 grupos de regresión JVM del protocolo, tres tests instrumentados de componentes de accesibilidad y tres tests Android adicionales del router. Las pruebas instrumentadas están escritas, no ejecutadas. Los checksums de exercises.json y lexemes.json estaban desactualizados en el ZIP de entrada y fueron reparados; ambos archivos de contenido permanecen byte a byte idénticos.

La versión de app es 2.5.0-dev/versionCode 25; contenido y audio permanecen 2.4.0. Metadatos y auditor de consistencia ahora distinguen esas versiones. No se presenta la presencia de scripts ni un PASS estático como validación física o aprobación de release.

Se conserva la configuración Android de la base. La compilación fue intentada y quedó bloqueada por falta de SDK/Gradle/wrapper en este entorno. No se ha ejecutado CI remoto, no hay APK en la entrega, no se añaden voces humanas y no se certifican resultados de aprendizaje.
