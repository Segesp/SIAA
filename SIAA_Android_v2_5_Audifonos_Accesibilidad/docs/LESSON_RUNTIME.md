> Actualización: para el flujo de audífonos y accesibilidad de 2.5.0-dev, consulta `GUIA_USUARIO_V25.md` y `AUDIFONOS_ARQUITECTURA_V25.md`. Este documento conserva antecedentes; no acredita la compilación Android nueva.

# Máquina de estados

```text
IDLE
 ↓ start
PREPARING
 ↓
SPEAKING
 ├─ TEACH ───────────────→ next
 ├─ binary ─→ WAITING_BINARY
 └─ recall ─→ WAITING_SELF_ASSESSMENT

WAITING_BINARY
  PRIMARY   = A
  SECONDARY = B
  BACK      = repetir

WAITING_SELF_ASSESSMENT
  PRIMARY   = correcto
  SECONDARY = dudé
  BACK      = fallé

respuesta → FEEDBACK → update state → planner → siguiente
```

`LessonRuntime` almacena el prompt activo para repetirlo tras pausa o `Previous`.
