# SIAA: primera sesión con audífonos

Versión de la aplicación: 2.5.0-dev. Esta guía acompaña un paquete de código fuente. **No es un APK**, ni acredita pruebas físicas de un teléfono o audífono concreto. Los pasos siguientes describen la interfaz implementada, una vez compilada e instalada la aplicación.

## Antes de empezar

Configura SIAA en un lugar tranquilo, antes de subir al autobús. Conecta los audífonos que realmente usarás. El modo de viaje no necesita micrófono, hablar en voz alta ni mirar preguntas en la pantalla. No uses una sesión al conducir o al cruzar la calle. Ajusta un volumen cómodo; la app no aumenta el volumen por su cuenta.

## Primera configuración

En **Inicio**, pulsa **Configurar mis audífonos**. También puedes entrar directamente en la pestaña **Audífonos**.

Pulsa **Comprobar conexión y voces**. Si el teléfono tiene el volumen multimedia a cero, súbelo. Si falta una voz sin conexión, pulsa **Instalar o revisar voces del teléfono** y descarga español e inglés en el motor de texto a voz de tu dispositivo. Vuelve a SIAA y repite la comprobación.

Pulsa **Escuchar una prueba**. Oirás una frase en español y otra en inglés. Confirma personalmente que el sonido sale por tus audífonos, no por el altavoz. Marca **Escuché la prueba por mis audífonos** solamente después de escucharla.

Pulsa **Comenzar comprobación guiada**. Haz el gesto que utilizarás para elegir. SIAA te pedirá repetirlo, después te ofrecerá comprobar otro gesto. Si no tienes otro, pulsa **No tengo otro gesto: continuar**. Cada control debe llegar dos veces de forma consistente; no se permite asignar el mismo control a dos funciones. El último paso es un ensayo sin nota: vuelve a realizar el primer gesto.

No hagas la calibración con los botones de la notificación ni de la pantalla bloqueada. Esos controles reproducen, pausan o repiten, pero no se aceptan como prueba de que un gesto del audífono sirve para responder. Si Android sólo entrega órdenes de reproducción que no permiten identificar un botón, prueba otro gesto configurable en la aplicación del fabricante. No marques el dispositivo como compatible si la comprobación no termina.

Pon un nombre a tus audífonos y describe los gestos con tus propias palabras: por ejemplo, “dos toques a la derecha”. Estas descripciones se usarán en los recordatorios hablados. SIAA no inventa ni cambia los gestos del fabricante.

Deja **Cómo elegir respuestas** en **Automático** al principio. Pulsa **Guardar controles y confirmación de sonido**. Si cambió la conexión desde la prueba, SIAA te pedirá repetirla. Vuelve a Inicio y termina la bienvenida.

## Tu sesión diaria

En Inicio, elige la duración y pulsa **Empezar … minutos de inglés**. El tiempo es un objetivo: el sistema decide si inicia otra actividad, no corta una pregunta a mitad para coincidir exactamente con un reloj.

Puedes hacer **Encontrar mi nivel inicial** o comenzar desde lo básico. Ese diagnóstico orienta el aprendizaje; no es una certificación. Para el uso diario no es necesario conocer siglas, modelos estadísticos ni códigos del contenido.

### Con un solo control o con dos en Automático

SIAA lee las opciones por turnos. En una pregunta A/B vuelve a leer el contenido de cada alternativa, no sólo su letra. Después de cada opción se oye una señal y se abre un silencio para elegir.

Si quieres esa opción, realiza tu gesto principal durante el silencio. Si no haces nada, escucharás la siguiente. El mismo mecanismo permite contestar “Sí, lo resolví”, “Tuve dudas” o “No lo resolví”. También incluye **Repetir la pregunta**, **Escuchar más despacio** y **Pausar la sesión**.

No pulses mientras se está anunciando la alternativa: todavía no se ha abierto la ventana de respuesta. Después de dos recorridos completos sin elegir, la sesión queda pausada, sin registrar el silencio como una respuesta incorrecta. Usa el gesto principal para reanudar cuando estés listo.

### Con tres controles en Automático

Se usa selección directa. En preguntas A/B, el primer gesto elige A, el segundo B y el tercero permite repetir/recibir ayuda. En autoevaluación, los tres gestos corresponden a sí, dudas y no. Escucha el recordatorio contextual, porque el tercer gesto no significa lo mismo en ambas actividades.

Puedes elegir **Opciones por turnos: un solo control** aunque tengas tres controles. En modo directo con sólo dos controles, la sesión excluye ejercicios que necesiten tres respuestas diferentes.

### Pausar, repetir y volver

Durante la explicación, el gesto principal pausa. Al reanudar se repite la actividad pendiente. Durante la selección por turnos, elige la opción **Pausar la sesión**. En pantalla y en la notificación hay controles explícitos para pausar/reanudar, repetir o finalizar. Las órdenes normales Play/Pause de la pantalla bloqueada no contestan una pregunta.

Al desconectar los audífonos o perder el uso del audio frente a otra aplicación, la sesión se pausa. No se reanuda sola. Antes de continuar, comprueba la conexión y dónde se escucha. Si cierras una sesión pausada desde las aplicaciones recientes, puede cerrarse el servicio; el progreso ya guardado se conserva.

## Ajustes útiles

En **Más → Ajustes y accesibilidad** puedes cambiar velocidad, tiempo de respuesta directa de 12 a 90 segundos, silencio por opción de 2 a 10 segundos y separación entre actividades de 0,3 a 2,5 segundos. Los ajustes de sesión se aplican al siguiente inicio; en la pregunta actual está disponible Más despacio.

También puedes mantener el Inicio simplificado, activar Contraste alto y abrir el texto de lo que se acaba de reproducir. El texto conserva las últimas intervenciones de la actividad, no constituye una transcripción completa exportada de toda la sesión. El tamaño de letra se ajusta desde el teléfono. Las pantallas se desplazan y los controles principales tienen etiquetas escritas y una altura mínima de 56 dp.

Con TalkBack activado, la guía de configuración no se reproduce automáticamente; puedes solicitarla con **Escuchar instrucciones de este paso**. El lector y el motor de voz requieren comprobación práctica en tu teléfono. La interfaz implementa ayudas de accesibilidad, pero todavía no está certificada ni validada con personas usuarias.

## Fuera del autobús

**Más → Práctica con pantalla y micrófono** separa lectura, escritura y voz del modo de viaje. Sólo al pulsar Hablar se solicita el micrófono. La transcripción reconocida no es una evaluación exacta de pronunciación; la revisión escrita es orientativa.

En Ajustes puedes exportar tu progreso antes de restaurar una copia o cambiar el contenido. La restauración exige comprobar otra vez los audífonos: la aprobación de salida del teléfono anterior no se considera válida. Las copias pueden contener respuestas y transcripciones personales; elige dónde guardarlas.

## Límites que conviene conocer

Detectar una salida Bluetooth conectada no demuestra por sí solo que sea un audífono y no un parlante, ni garantiza la ruta física de cada sonido. Por eso existe la confirmación escuchada. La identificación local usa tipo y nombre del dispositivo; dos equipos del mismo nombre pueden coincidir. No hay una garantía universal para todos los fabricantes, modos multipunto o controles del sistema. Las pruebas con pantalla bloqueada, TalkBack, modo avión y reconexiones siguen pendientes de ejecución física.
