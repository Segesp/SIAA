> Actualización: para el flujo de audífonos y accesibilidad de 2.5.0-dev, consulta `GUIA_USUARIO_V25.md` y `AUDIFONOS_ARQUITECTURA_V25.md`. Este documento conserva antecedentes; no acredita la compilación Android nueva.

# Audífonos y MediaSession

Los taps físicos no son una API portable. El firmware del audífono los convierte en comandos de transporte que Android entrega a la MediaSession.

Mapping inicial:

| Evento | Runtime |
|---|---|
| Play/Pause | PRIMARY |
| Play | PRIMARY |
| Next | SECONDARY |
| Previous | BACK |
| Pause | PAUSE |
| Stop | STOP |

El significado de PRIMARY/SECONDARY/BACK depende del estado del tutor. Esto permite reutilizar los mismos 2–3 comandos para preguntas, autoevaluación y navegación.

La pestaña `Audífonos` muestra el último keycode recibido para calibración real por dispositivo.
