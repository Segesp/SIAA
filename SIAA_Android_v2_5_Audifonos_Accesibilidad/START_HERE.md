# Empieza aquí — SIAA 2.5.0-dev

Este ZIP contiene el proyecto Android actualizado. **No contiene una aplicación APK ya compilada.**

## Para quien va a usar SIAA

Una vez compilada e instalada, abre Inicio → Configurar mis audífonos. La aplicación guía la prueba de sonido, comprobación de gestos y ensayo sin nota. Deja el modo Automático para empezar. Después elige la duración y pulsa Empezar.

La [guía de uso](docs/GUIA_USUARIO_V25.md) explica las opciones habladas, pausas, accesibilidad y solución de problemas. No es necesario conocer KCs, grafos ni algoritmos para utilizar el recorrido principal.

## Para quien prepara el APK

Descomprime el ZIP completo. Abre la carpeta que contiene `settings.gradle.kts` en Android Studio. Instala el SDK que indica `app/build.gradle.kts` y usa JDK 17. Verifica la configuración de SDK local, que deliberadamente no se incluye en el ZIP.

En Windows PowerShell, desde la raíz:

```powershell
.\scripts\bootstrap_gradle.ps1 :app:assembleDebug
```

En Linux/macOS:

```sh
./scripts/bootstrap_gradle.sh :app:assembleDebug
```

El bootstrap descarga el wrapper oficial y comprueba su hash. Necesita internet. El APK, si la compilación termina correctamente, se genera en `app/build/outputs/apk/debug/`. La entrega actual no afirma que esa compilación haya sido exitosa.

Antes de entregar el APK a otra persona, ejecuta los tests instrumentados y completa la [matriz de validación Android](docs/QA_AUDIFONOS_ACCESIBILIDAD_V25.md), especialmente pantalla bloqueada, Bluetooth, llamadas, voces offline y TalkBack. Las pruebas JVM ya ejecutadas no reemplazan ese paso.

## Para auditar el paquete

`docs/RESULTADOS_V25.md` resume resultados y límites. `validation/v25/` guarda logs. `FILES_SHA256.txt` y `scripts/verify_release.py` verifican integridad. La versión del contenido sigue siendo 2.4.0: esta entrega cambia el sistema de interacción, no simula una ampliación curricular.

El PDF maestro y los documentos v2.3/v2.4 son referencias históricas conservadas, no certificados de la implementación nueva.
