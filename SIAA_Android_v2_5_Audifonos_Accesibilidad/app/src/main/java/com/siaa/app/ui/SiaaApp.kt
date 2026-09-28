package com.siaa.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siaa.app.*
import com.siaa.core.model.*
import com.siaa.core.runtime.*

private enum class AppTab { HOME, HEADPHONES, MORE, SETTINGS, PRACTICE, PROGRESS, CURRICULUM }
private data class Confirmation(val title: String, val detail: String, val action: () -> Unit)

@Composable
fun SiaaApp(
    viewModel: MainViewModel,
    onStartMode: (SessionMode) -> Unit,
    onStop: () -> Unit,
    onStartCalibration: () -> Unit,
    onStopCalibration: () -> Unit,
    onStartSpeaking: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onImportContentPack: () -> Unit,
    onRollbackContent: () -> Unit,
    onRuntimeAction: (String, Long) -> Unit,
    onSpeakGuide: () -> Unit,
    onOpenAudioSettings: () -> Unit,
    onOpenVoiceSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var confirmation by remember { mutableStateOf<Confirmation?>(null) }
    BackHandler(enabled=tab!=AppTab.HOME) { tab=if(tab in setOf(AppTab.HEADPHONES,AppTab.MORE)) AppTab.HOME else AppTab.MORE }
    val dark = isSystemInDarkTheme()
    val high = state.preferences.headphones.highContrast
    val colors = when {
        high && dark -> darkColorScheme(primary=Color.White,onPrimary=Color.Black,surface=Color.Black,onSurface=Color.White,onSurfaceVariant=Color.White)
        high -> lightColorScheme(primary=Color.Black,onPrimary=Color.White,surface=Color.White,onSurface=Color.Black,onSurfaceVariant=Color.Black)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme=colors) {
        Scaffold(bottomBar={
            Surface(tonalElevation=3.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(6.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf(AppTab.HOME to "Inicio",AppTab.HEADPHONES to "Audífonos",AppTab.MORE to "Más").forEach { (dest,label) ->
                        val selected=tab==dest || (dest==AppTab.MORE && tab !in setOf(AppTab.HOME,AppTab.HEADPHONES))
                        OutlinedButton(onClick={tab=dest},modifier=Modifier.weight(1f).heightIn(min=56.dp).semantics { this.selected=selected }) { Text(label) }
                    }
                }
            }
        }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when(tab) {
                    AppTab.HOME -> HomeScreen(state,viewModel,onStartMode,onStop,onRuntimeAction,{tab=AppTab.HEADPHONES},{tab=AppTab.SETTINGS})
                    AppTab.HEADPHONES -> HeadphoneScreen(state,viewModel,onStartCalibration,onStopCalibration,onSpeakGuide,onOpenAudioSettings,onOpenVoiceSettings)
                    AppTab.MORE -> Page {
                        Heading("Más opciones")
                        Text("La sesión del viaje se inicia desde Inicio. Estas opciones son para cuando puedas mirar el teléfono.")
                        ActionButton("Práctica con pantalla y micrófono",enabled=state.preferences.extendedSkillsEnabled){tab=AppTab.PRACTICE}
                        ActionButton("Mi progreso"){tab=AppTab.PROGRESS}
                        ActionButton("Explorar lo que puedo aprender"){tab=AppTab.CURRICULUM}
                        ActionButton("Ajustes y accesibilidad"){tab=AppTab.SETTINGS}
                        ActionButton("Volver a ver la bienvenida"){viewModel.showOnboarding();tab=AppTab.HOME}
                    }
                    AppTab.SETTINGS -> SettingsScreen(state,viewModel,onSpeakGuide,onOpenVoiceSettings,onExportBackup,
                        {confirmation=Confirmation("Restaurar una copia", "La copia combinará o reemplazará datos de aprendizaje. Exporta primero tu progreso actual. Después de restaurarla debes volver a confirmar tus audífonos.",onImportBackup)},
                        {confirmation=Confirmation("Actualizar contenido", "Elige un paquete de contenido firmado. No inicies una sesión durante la instalación.",onImportContentPack)},
                        {confirmation=Confirmation("Volver al contenido incluido", "Se desactivará el paquete instalado. Se conservará el progreso registrado.",onRollbackContent)},
                        {confirmation=Confirmation("Borrar mi progreso", "Se eliminarán tus sesiones y respuestas. Esta acción no se puede deshacer sin una copia de seguridad.",viewModel::resetLearningData)})
                    AppTab.PRACTICE -> PracticeScreen(state,onStartSpeaking,viewModel)
                    AppTab.PROGRESS -> ProgressScreen(state,viewModel::refresh)
                    AppTab.CURRICULUM -> CurriculumScreen(state.curriculum,!state.preferences.headphones.simpleHome)
                }
            }
        }
        confirmation?.let { c -> AlertDialog(onDismissRequest={confirmation=null},title={Text(c.title)},text={Text(c.detail)},
            confirmButton={TextButton(onClick={confirmation=null;c.action()}){Text("Continuar")}},
            dismissButton={TextButton(onClick={confirmation=null}){Text("Cancelar")}}) }
    }
}

@Composable private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)
}
@Composable internal fun Heading(text:String) { Text(text,modifier=Modifier.semantics { heading() },style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
@Composable private fun Subheading(text:String) { Text(text,modifier=Modifier.semantics { heading() },style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold) }
@Composable internal fun ActionButton(text:String,enabled:Boolean=true,onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text(text) }
}
@Composable private fun Notice(text:String, announce:Boolean=true) {
    if(text.isNotBlank()) OutlinedCard(Modifier.fillMaxWidth()) {
        Text(text,Modifier.padding(14.dp).semantics { if(announce) liveRegion=LiveRegionMode.Polite })
    }
}
private fun active(state:MainUiState) = state.runtime.state !in setOf(LessonState.IDLE,LessonState.SESSION_END,LessonState.ERROR)

@Composable private fun HomeScreen(
    state:MainUiState, vm:MainViewModel, start:(SessionMode)->Unit, stop:()->Unit,
    command:(String,Long)->Unit, headphones:()->Unit, settings:()->Unit
) {
    val p=state.preferences
    Page {
        Heading("SIAA · Aprende escuchando")
        if(!state.contentReady) {
            if(state.contentError==null){LinearProgressIndicator(Modifier.fillMaxWidth());Text("Preparando las lecciones en el teléfono…")}
            else Notice("No se pudieron preparar las lecciones. Reabre la aplicación. Detalle: ${state.contentError}")
        }
        if(!p.onboardingCompleted) {
            Subheading("Tu primera sesión, paso a paso")
            Text("Primero configurarás el teléfono aquí. Después podrás guardarlo y aprender sin hablar ni mirar la pantalla.")
            Text("1. Conecta los audífonos y prueba el sonido.\n2. Comprueba los gestos que realmente funcionan.\n3. Elige el tiempo de tu viaje e inicia una sesión.")
            Text("No hace falta saber tu nivel: puedes empezar desde lo básico o hacer el diagnóstico inicial. Usa SIAA en un lugar seguro; no mientras conduces o cruzas la calle.")
            ActionButton(if(p.headphones.setupCompleted) "Revisar mis audífonos" else "Configurar mis audífonos",onClick=headphones)
            DurationChooser(p.targetDurationMinutes,vm::setTargetDuration)
            ActionButton("Terminar bienvenida",enabled=p.headphones.setupCompleted){vm.completeOnboarding()}
            TextButton(onClick=vm::completeOnboarding){Text("Explorar la aplicación sin iniciar audio")}
            HorizontalDivider()
        }
        if(state.systemMessage.isNotBlank()) Notice(state.systemMessage)
        // Operational errors must not be hidden in the calibration screen.
        if(state.lastMediaEvent.isNotBlank() && !state.lastMediaEvent.startsWith("keyCode") &&
            !state.lastMediaEvent.startsWith("raw") && state.lastMediaEvent!="Sin eventos") Notice(state.lastMediaEvent)
        if(active(state)) {
            SessionCard(state,stop,command)
        } else {
            if(!p.headphones.setupCompleted) {
                Notice("Falta comprobar tus audífonos. No iniciaremos una sesión suponiendo que tus controles funcionan.")
                ActionButton("Comprobar mis audífonos",onClick=headphones)
            }
            DurationChooser(p.targetDurationMinutes,vm::setTargetDuration)
            ActionButton("Empezar ${p.targetDurationMinutes} minutos de inglés",state.contentReady && p.headphones.setupCompleted){start(SessionMode.ADAPTIVE)}
            Text("Escucharás una explicación antes de practicar. Si necesitas más tiempo, ajusta el ritmo sin cambiar de nivel.")
            TextButton(onClick=settings){Text("Cambiar velocidad o tiempo para responder")}
            if(!p.placementCompleted) ActionButton("Encontrar mi nivel inicial (opcional)",state.contentReady && p.headphones.setupCompleted){start(SessionMode.PLACEMENT)}
            var more by rememberSaveable { mutableStateOf(false) }
            TextButton(onClick={more=!more}){Text(if(more) "Ocultar tipos de práctica" else "Elegir un tipo de práctica")}
            if(more || !p.headphones.simpleHome) listOf(SessionMode.VOCABULARY,SessionMode.GRAMMAR,SessionMode.LISTENING,SessionMode.SPELLING,SessionMode.PRONUNCIATION).forEach { mode ->
                ActionButton(label(mode),state.contentReady && p.headphones.setupCompleted){start(mode)}
            }
        }
        if(state.runtime.lastSessionSummary.isNotBlank()) {Subheading("Tu última sesión");Text(state.runtime.lastSessionSummary)}
        state.stats?.let { stats ->
            Subheading("Cómo vas")
            Text("Punto de partida estimado: ${stats.currentCefrEstimate}. Es una orientación, no una certificación.")
            Text("${stats.dueKcs} temas listos para repasar. ${stats.masteredKcs} temas con progreso consolidado.")
        }
    }
}

@Composable private fun SessionCard(state:MainUiState,stop:()->Unit,command:(String,Long)->Unit) {
    val r=state.runtime
    val waiting=r.state in setOf(LessonState.WAITING_BINARY,LessonState.WAITING_SELF_ASSESSMENT)
    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Subheading(if(r.state==LessonState.PAUSED) "Sesión pausada" else "Tu sesión está en marcha")
        Text(r.message,Modifier.semantics { if(r.state==LessonState.PAUSED) liveRegion=LiveRegionMode.Polite })
        Text("${r.turnsCompleted} actividades completadas")
        if(r.state==LessonState.PAUSED) {
            Text(when(r.pauseReason){PauseReason.AUDIO_ROUTE_LOST->"Comprueba la conexión y dónde se oye el audio. La sesión no se reanuda sola.";PauseReason.AUDIO_FOCUS_LOST->"Otra aplicación necesitó el audio. Reanuda cuando estés listo.";PauseReason.INACTIVITY->"No recibimos respuesta. No lo contamos como un error.";else->"Al reanudar repetiremos la actividad pendiente."})
            ActionButton("Reanudar la sesión"){command("PLAY",r.turnId)}
        } else ActionButton("Pausar la sesión"){command("PAUSE",r.turnId)}
        if(waiting) {
            if(r.singleSwitchScanning) Notice(if(r.responseWindowOpen) "Puedes elegir ahora: ${r.scanLabel}" else "Escucha la opción antes de elegir: ${r.scanLabel}", announce=false)
            Text(r.controlHint)
            if(r.state==LessonState.WAITING_BINARY) {
                ActionButton("Elegir opción A",r.responseWindowOpen){command("ANSWER_A",r.turnId)}
                ActionButton("Elegir opción B",r.responseWindowOpen){command("ANSWER_B",r.turnId)}
            } else {
                ActionButton("Sí, lo resolví",r.responseWindowOpen){command("ANSWER_YES",r.turnId)}
                ActionButton("Tuve dudas",r.responseWindowOpen){command("ANSWER_UNSURE",r.turnId)}
                ActionButton("No lo resolví",r.responseWindowOpen){command("ANSWER_NO",r.turnId)}
            }
            ActionButton("Repetir la pregunta"){command("REPEAT",r.turnId)}
            ActionButton("Escuchar más despacio"){command("SLOWER",r.turnId)}
            ActionButton("Recordarme los controles"){command("CONTROLS",r.turnId)}
        }
        if(state.preferences.headphones.captionsEnabled) {
            var show by rememberSaveable { mutableStateOf(false) }
            TextButton(onClick={show=!show}){Text(if(show) "Ocultar texto del audio" else "Ver texto del audio")}
            if(show) r.captions.forEach { line -> Text(line) }
        }
        OutlinedButton(onClick=stop,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("Finalizar sesión")}
    }}
}

@Composable private fun DurationChooser(minutes:Int,change:(Int)->Unit) {
    Choice("Duración de la sesión",minutes,listOf(5,10,15,20,25,30,45,60),{"$it minutos"},change)
}

@Composable private fun HeadphoneScreen(state:MainUiState,vm:MainViewModel,start:()->Unit,stop:()->Unit,speak:()->Unit,sound:()->Unit,voices:()->Unit) {
    val h=state.preferences.headphones
    val setup=state.setup
    var name by rememberSaveable { mutableStateOf(state.deviceProfile?.name ?: "Mis audífonos") }
    var firstLabel by rememberSaveable { mutableStateOf(h.primaryGesture) }
    var secondLabel by rememberSaveable { mutableStateOf(h.secondaryGesture) }
    var thirdLabel by rememberSaveable { mutableStateOf(h.backGesture) }
    var heard by rememberSaveable { mutableStateOf(false) }
    var technical by rememberSaveable { mutableStateOf(false) }
    val busy=active(state)
    Page {
        Heading("Tus audífonos, tus controles")
        Text("No todos los audífonos envían lo mismo. Aquí probaremos tus gestos sin modificar tu nivel ni tu progreso.")
        if(busy){Notice("Finaliza la sesión desde Inicio antes de cambiar los controles.");return@Page}
        Subheading("1. Comprueba el sonido")
        Text("Conecta tus audífonos y ajusta el volumen multimedia a un nivel cómodo. Comprueba que no se oye por el altavoz del teléfono.")
        ActionButton("Comprobar conexión y voces"){heard=false;vm.runAudioPreflight()}
        state.preflight?.let { report -> Notice(report.message) }
        ActionButton("Escuchar una prueba",state.preflight?.ready==true){heard=false;speak()}
        ToggleRow("Escuché la prueba por mis audífonos",heard){heard=it}
        if(state.preflight?.ready!=true) {
            TextButton(onClick=sound){Text("Abrir ajustes de sonido")}
            TextButton(onClick=voices){Text("Instalar o revisar voces del teléfono")}
        }
        Subheading("2. Comprueba cada gesto")
        Text("Puedes usar los gestos configurados en la aplicación de tus audífonos. SIAA comprueba las órdenes que recibe; no puede cambiar los gestos del fabricante.")
        ActionButton(if(setup.step==SetupStep.IDLE) "Comenzar comprobación guiada" else "Empezar la comprobación de nuevo",state.preflight?.ready==true && heard,onClick=start)
        Notice(setup.message)
        if(setup.active) {
            Text("Comprobaciones completadas: ${setup.successfulChecks}")
            ActionButton("Escuchar instrucciones de este paso",onClick=speak)
            if(setup.step in setOf(SetupStep.SECONDARY,SetupStep.CONFIRM_SECONDARY,SetupStep.BACK,SetupStep.CONFIRM_BACK))
                ActionButton("No tengo otro gesto: continuar"){vm.skipSetupGesture()}
            OutlinedButton(onClick=stop,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("Detener comprobación")}
        }
        Subheading("3. Pon nombres fáciles de recordar")
        OutlinedTextField(name,{name=it.take(80)},label={Text("Nombre de estos audífonos")},modifier=Modifier.fillMaxWidth(),singleLine=false)
        OutlinedTextField(firstLabel,{v->firstLabel=v;vm.updateHeadphones{it.copy(primaryGesture=v)}},label={Text("Primer gesto, por ejemplo: dos toques a la derecha")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(secondLabel,{v->secondLabel=v;vm.updateHeadphones{it.copy(secondaryGesture=v)}},label={Text("Segundo gesto, si tienes uno")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(thirdLabel,{v->thirdLabel=v;vm.updateHeadphones{it.copy(backGesture=v)}},label={Text("Tercer gesto, si tienes uno")},modifier=Modifier.fillMaxWidth())
        Choice("Cómo elegir respuestas",h.mode,HeadphoneMode.entries,{label(it)}){v->vm.updateHeadphones{it.copy(mode=v)}}
        Text("Automático: con tres controles se elige directamente; con uno o dos, SIAA lee las opciones por turnos. Usa tu primer gesto durante el silencio de la opción que quieras. También escucharás Repetir, Más despacio y Pausar.")
        Text("El modo directo con dos controles sólo incluye actividades que puedan resolverse con ellos. No anuncia un tercer gesto inexistente.")
        ActionButton("Guardar controles y confirmación de sonido",setup.step==SetupStep.READY && heard && state.preflight?.ready==true){vm.saveGuidedCalibration(name);stop()}
        state.deviceProfile?.let { profile ->
            Notice("Perfil guardado: ${profile.name}. Controles comprobados: ${listOfNotNull(profile.primaryKeyCode,profile.secondaryKeyCode,profile.backKeyCode).size}.")
        }
        if(state.systemMessage.isNotBlank()) Notice(state.systemMessage)
        if(state.lastMediaEvent!="Sin eventos" && !state.lastMediaEvent.startsWith("raw") && !state.lastMediaEvent.startsWith("keyCode")) Notice(state.lastMediaEvent)
        Text("Si cambia el modelo de audífonos, vuelve a comprobarlo. Tras una desconexión no se reproduce audio automáticamente.")
        TextButton(onClick={technical=!technical}){Text(if(technical) "Ocultar detalles técnicos" else "Ver detalles para solucionar un problema")}
        if(technical) Text(state.lastMediaEvent)
    }
}

@Composable private fun SettingsScreen(state:MainUiState,vm:MainViewModel,speak:()->Unit,voices:()->Unit,export:()->Unit,restoreBackup:()->Unit,pack:()->Unit,rollback:()->Unit,reset:()->Unit) {
    val p=state.preferences; val h=p.headphones
    val canChangeData=!active(state) && !state.setup.active
    Page {
        Heading("Ajustes y accesibilidad")
        Text("Los cambios de ritmo y controles se aplican al empezar la siguiente sesión. Dentro de una pregunta puedes usar ‘Más despacio’.")
        Subheading("Escuchar y responder con calma")
        Text("Velocidad de voz: ${"%.2f".format(p.speechRate)}")
        Slider(value=p.speechRate,onValueChange=vm::setSpeechRate,valueRange=0.65f..1.35f,
            modifier=Modifier.semantics { contentDescription="Velocidad de la voz";stateDescription="${"%.2f".format(p.speechRate)} veces la velocidad normal" })
        ActionButton("Escuchar una prueba de velocidad",!active(state),speak)
        Choice("Tiempo para responder",h.responseSeconds,listOf(12,20,30,45,60,90),{"$it segundos"}){v->vm.updateHeadphones{it.copy(responseSeconds=v)}}
        Choice("Tiempo de cada opción con un solo control",h.scanSeconds,listOf(2,3,4,5,6,8,10),{"$it segundos"}){v->vm.updateHeadphones{it.copy(scanSeconds=v)}}
        Choice("Pausa entre actividades",h.transitionMillis,listOf(300L,700L,1200L,1800L,2500L),{"${it/1000.0} segundos"}){v->vm.updateHeadphones{it.copy(transitionMillis=v)}}
        ToggleRow("Pausar si dejo de responder",h.pauseOnSilence){v->vm.updateHeadphones{it.copy(pauseOnSilence=v)}}
        Text("En el modo de un solo control siempre se pausa después de dos recorridos sin respuesta. El silencio no se califica como error.")
        ToggleRow("Recordarme cómo responder",p.announceControls,vm::setAnnounceControls)
        ToggleRow("Explicarme la respuesta",p.feedbackExplanations,vm::setFeedbackExplanations)
        Subheading("Ver y navegar")
        ToggleRow("Inicio simplificado",h.simpleHome){v->vm.updateHeadphones{it.copy(simpleHome=v)}}
        ToggleRow("Mostrar texto del audio cuando lo abra",h.captionsEnabled){v->vm.updateHeadphones{it.copy(captionsEnabled=v)}}
        ToggleRow("Contraste alto",h.highContrast){v->vm.updateHeadphones{it.copy(highContrast=v)}}
        Text("SIAA respeta el tamaño de letra del teléfono. Con TalkBack, la guía de configuración no habla automáticamente encima del lector: usa ‘Escuchar instrucciones’ cuando lo necesites.")
        ActionButton("Revisar voces del teléfono",onClick=voices)
        Subheading("Mi aprendizaje")
        DurationChooser(p.targetDurationMinutes,vm::setTargetDuration)
        Choice("Objetivo",p.learningGoal,LearningGoal.entries,{label(it)},vm::setLearningGoal)
        Choice("Ritmo de contenido nuevo",p.intensity,SessionIntensity.entries,{label(it)},vm::setIntensity)
        Choice("Meta de nivel",p.targetCefr,listOf("PRE-A1","A1","A2","B1","B2","C1","C2"),{it},vm::setTargetCefr)
        ToggleRow("Habilitar práctica con pantalla y micrófono",p.extendedSkillsEnabled,vm::setExtendedSkills)
        Text("El micrófono sólo se pide al pulsar ‘Hablar’. El modo viaje no necesita que hables.")
        Subheading("Mis datos")
        if(!canChangeData) Notice("Finaliza la sesión o comprobación antes de cambiar datos o contenido.")
        ActionButton("Guardar una copia de mi progreso",canChangeData,export)
        ActionButton("Restaurar una copia",canChangeData,restoreBackup)
        ActionButton("Instalar una actualización de contenido",canChangeData,pack)
        ActionButton("Volver al contenido original",canChangeData,rollback)
        OutlinedButton(onClick=reset,enabled=canChangeData,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("Borrar mi progreso…")}
        Text("No se envían estadísticas de aprendizaje automáticamente. Las copias se guardan donde tú elijas y pueden contener respuestas y transcripciones personales.")
        Notice(state.systemMessage)
    }
}

@Composable internal fun ToggleRow(label:String,value:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp).toggleable(value=value,role=Role.Switch,onValueChange=onChange)
        .semantics(mergeDescendants=true){stateDescription=if(value) "Activado" else "Desactivado"}.padding(vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(label,Modifier.weight(1f))
        Switch(checked=value,onCheckedChange=null)
    }
}
@Composable private fun <T> Choice(title:String,value:T,values:List<T>,label:(T)->String,change:(T)->Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("$title: ${label(value)}")}
        DropdownMenu(expanded=open,onDismissRequest={open=false}) {
            values.forEach { v -> DropdownMenuItem(text={Text(label(v))},onClick={change(v);open=false},modifier=Modifier.heightIn(min=56.dp)) }
        }
    }
}

@Composable private fun PracticeScreen(state:MainUiState,speak:()->Unit,vm:MainViewModel) {
    val practice=state.practice
    var writing by rememberSaveable(practice.writing?.id) { mutableStateOf("") }
    Page {
        Heading("Práctica fuera del viaje")
        if(active(state)){Notice("Finaliza la sesión auditiva antes de abrir el micrófono o practicar con pantalla.");return@Page}
        if(!state.preferences.extendedSkillsEnabled){Text("Puedes activar esta práctica desde Ajustes.");return@Page}
        Text("Esta sección sí necesita mirar el teléfono. Para el autobús, usa Inicio.")
        Notice(practice.lastResult)
        practice.speaking?.let { x ->
            Subheading("Hablar y comparar")
            Text(x.promptEs);Text(x.targetEn)
            ActionButton("Hablar y comparar lo reconocido",onClick=speak)
            TextButton(onClick=vm::nextSpeaking){Text("Otra frase para hablar")}
            Text("La comparación usa la transcripción reconocida, no una medida exacta del acento ni de la pronunciación.")
        }
        practice.reading?.let { x ->
            Subheading("Leer y comprender");Text(x.passage);Text(x.questionEs)
            ActionButton("A: ${x.optionA}"){vm.answerReading("A")}
            ActionButton("B: ${x.optionB}"){vm.answerReading("B")}
            TextButton(onClick=vm::nextReading){Text("Otra lectura")}
        }
        practice.writing?.let { x ->
            Subheading("Practicar escritura");Text(x.promptEs)
            OutlinedTextField(value=writing,onValueChange={writing=it},label={Text("Tu respuesta en inglés")},minLines=3,modifier=Modifier.fillMaxWidth())
            ActionButton("Comprobar mi texto",writing.isNotBlank()){vm.submitWriting(writing);writing=""}
            TextButton(onClick=vm::nextWriting){Text("Otra actividad de escritura")}
            Text("La comprobación de escritura es orientativa y no sustituye una corrección lingüística completa.")
        }
    }
}

@Composable private fun ProgressScreen(state:MainUiState,refresh:()->Unit) {
    val names=remember(state.curriculum){state.curriculum.associateBy{it.id}}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=20.dp)) {
        item {Heading("Mi progreso")}
        item {TextButton(onClick=refresh){Text("Actualizar progreso")}}
        item {Text("Estas estimaciones orientan los próximos repasos; no certifican un nivel de inglés.")}
        if(state.sessions.isNotEmpty()) item {Subheading("Sesiones recientes")}
        items(state.sessions.take(10),key={"session-${it.id}"}) { s ->
            OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text(label(s.mode),fontWeight=FontWeight.Bold);Text("${s.completedItems} actividades completadas")
                Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(s.startedAtEpochMs)))
            }}
        }
        item {Subheading("Lo que has practicado")}
        if(state.states.isEmpty()) item {Text("Tu progreso aparecerá después de practicar.")}
        items(state.states.sortedByDescending{it.totalAttempts},key={"kc-${it.kcId}"}) { s ->
            OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Text(names[s.kcId]?.name ?: "Tema de aprendizaje",fontWeight=FontWeight.SemiBold)
                LinearProgressIndicator(progress={s.mastery.toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().semantics{contentDescription="Progreso estimado en este tema"})
                Text("${(s.mastery*100).toInt()} % de dominio estimado · ${s.totalAttempts} intentos")
                if(!state.preferences.headphones.simpleHome) Text("Referencia técnica: ${s.kcId}")
            }}
        }
    }
}

@Composable private fun CurriculumScreen(curriculum:List<CurriculumItem>,technical:Boolean) {
    var search by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf("Todos") }
    val filtered=remember(curriculum,search,level){curriculum.filter{(level=="Todos" || it.cefr.equals(level,true)) && it.name.contains(search,true)}}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=20.dp)) {
        item {Heading("Lo que puedes aprender")}
        item {OutlinedTextField(search,{search=it},label={Text("Buscar un tema")},modifier=Modifier.fillMaxWidth())}
        item {Choice("Nivel",level,listOf("Todos","PRE-A1","A1","A2","B1","B2","C1","C2"),{it}){level=it}}
        item {Text("${filtered.size} temas. SIAA elige los siguientes según tus necesidades y los conocimientos previos.")}
        items(filtered,key={it.id}) { x -> OutlinedCard(Modifier.fillMaxWidth()){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text(x.name,fontWeight=FontWeight.SemiBold);Text("${x.cefr} · ${label(x.domain)}")
                Text(if(x.unlocked) "Disponible para practicar" else "Primero practicaremos temas anteriores")
                if(technical) Text("Referencia: ${x.id}; preparación ${(x.readiness*100).toInt()} %")
            }
        }}
    }
}

private fun label(value:Enum<*>):String = when(value) {
    HeadphoneMode.AUTO->"Automático (recomendado)";HeadphoneMode.DIRECT->"Elegir con controles distintos";HeadphoneMode.SINGLE_SWITCH->"Opciones por turnos: un solo control"
    SessionMode.ADAPTIVE->"Práctica adaptada a mí";SessionMode.PLACEMENT->"Diagnóstico inicial";SessionMode.VOCABULARY->"Vocabulario";SessionMode.GRAMMAR->"Gramática";SessionMode.LISTENING->"Comprensión auditiva";SessionMode.SPELLING->"Letras y deletreo";SessionMode.PRONUNCIATION->"Distinguir sonidos";SessionMode.SPEAKING->"Hablar";SessionMode.READING->"Lectura";SessionMode.WRITING->"Escritura"
    SessionIntensity.GENTLE->"Tranquilo, más repaso";SessionIntensity.BALANCED->"Equilibrado";SessionIntensity.CHALLENGING->"Más contenido nuevo"
    else->when(value.name){"GENERAL"->"Inglés cotidiano";"TRAVEL"->"Viajes";"WORK"->"Trabajo";"ACADEMIC"->"Estudios";"EXAM"->"Exámenes";"GRAMMAR"->"Gramática";"VOCABULARY"->"Vocabulario";"LEXICON"->"Vocabulario";"LISTENING"->"Comprensión auditiva";"PHONOLOGY"->"Sonidos del inglés";"ORTHOGRAPHY"->"Escritura y deletreo";"PRAGMATICS"->"Intención y uso en contexto";"FORMULAIC"->"Expresiones frecuentes";else->value.name.lowercase().replace('_',' ').replaceFirstChar{it.titlecase()}}
}
