import com.siaa.core.algorithm.StateUpdater
import com.siaa.core.model.*
import com.siaa.core.runtime.*
import kotlinx.coroutines.*

private suspend fun until(ms:Long=5_000, p:()->Boolean) { withTimeout(ms) { while(!p()) delay(5) } }
private fun one()=DeviceProfile(name="one",primaryKeyCode=85,nextAvailable=false,previousAvailable=false)
private fun two()=one().copy(secondaryKeyCode=87,nextAvailable=true)
private fun three()=two().copy(backKeyCode=88,previousAvailable=true)
private fun cfg(scan:Boolean=true)=SessionConfig(SessionMode.GRAMMAR,maxItems=1,announceControls=false,
    headphonePreferences=HeadphonePreferences(scanSeconds=2,transitionMillis=300),
    capabilities=if(scan) HeadphoneCompatibility.capabilities(HeadphoneMode.AUTO,one()) else SessionCapabilities())
private fun tutor(r:R, sp:SpeechPort=SlowSpeech(0))=LessonRuntime(r,P(),StateUpdater(),sp,E(),dispatcher=Dispatchers.Default)

fun main()=runBlocking {
    var tests=0
    fun passed(name:String){tests++;println("PASS $name")}
    check(HeadphoneCompatibility.validate(one())==null)
    check(HeadphoneCompatibility.capabilities(HeadphoneMode.AUTO,one()).supportsThreeWay)
    check(HeadphoneCompatibility.useScanning(HeadphoneMode.AUTO,two()))
    check(!HeadphoneCompatibility.useScanning(HeadphoneMode.AUTO,three()))
    check(!HeadphoneCompatibility.capabilities(HeadphoneMode.DIRECT,two()).supportsThreeWay)
    check(!HeadphoneCompatibility.capabilities(HeadphoneMode.DIRECT,one()).supportsBinary)
    check(HeadphoneCompatibility.validate(three().copy(backKeyCode=85))!=null)
    check(HeadphoneCompatibility.validate(one().copy(primaryKeyCode=86))!=null)
    check(HeadphoneCompatibility.validate(two().copy(secondaryKeyCode=127))!=null)
    check(HeadphoneCompatibility.validate(one().copy(primaryKeyCode=42))!=null)
    passed("CAPABILITIES_AND_INVALID_PROFILES")

    var setup=HeadphoneSetupFlow.begin()
    check(setup.profile("name",1)==null)
    check(HeadphoneSetupFlow.receive(setup,86).step==SetupStep.PRIMARY)
    check(HeadphoneSetupFlow.receive(setup,42).step==SetupStep.PRIMARY)
    setup=HeadphoneSetupFlow.receive(setup,85)
    check(setup.step==SetupStep.CONFIRM_PRIMARY)
    check(HeadphoneSetupFlow.receive(setup,87).step==SetupStep.CONFIRM_PRIMARY)
    setup=HeadphoneSetupFlow.receive(setup,85)
    setup=HeadphoneSetupFlow.skip(setup)
    check(setup.step==SetupStep.REHEARSAL)
    check(HeadphoneSetupFlow.receive(setup,87).step==SetupStep.REHEARSAL)
    setup=HeadphoneSetupFlow.receive(setup,85)
    check(setup.step==SetupStep.READY && setup.profile("one",1)?.nextAvailable==false)
    passed("SETUP_REQUIRES_REPEAT_AND_UNGRADED_REHEARSAL")

    setup=HeadphoneSetupFlow.begin()
    listOf(85,85,87,87,88,88,85).forEach{setup=HeadphoneSetupFlow.receive(setup,it)}
    check(setup.step==SetupStep.READY)
    check(HeadphoneCompatibility.validate(setup.profile("three",2))==null)
    passed("THREE_CONTROL_SETUP")

    val gate=MediaInputGate()
    check(!gate.accept(85,false,0,100));check(!gate.accept(85,true,1,100))
    check(gate.accept(85,true,0,100));check(!gate.accept(85,true,0,200))
    check(gate.accept(87,true,0,210));gate.reset();check(gate.accept(85,true,0,0));check(gate.accept(85,true,0,400))
    check(CalibratedMediaMapper().map(86,one().copy(primaryKeyCode=86))==MediaControlEvent.STOP)
    check(CalibratedMediaMapper().map(127,one().copy(primaryKeyCode=127))==MediaControlEvent.PAUSE)
    passed("DEBOUNCE_KEY_UP_REPEATS_AND_SAFETY")

    val prefs=HeadphonePreferences(primaryGesture="  dos\ntoques  ",scanSeconds=-5,responseSeconds=999).normalized()
    check(prefs.primaryGesture=="dos toques" && prefs.scanSeconds==2 && prefs.responseSeconds==90)
    check(!HeadphoneProtocol.hint(false,HeadphoneCompatibility.capabilities(HeadphoneMode.DIRECT,two()),prefs).contains("tercer"))
    check(HeadphoneProtocol.choices(true).map{it.id}.containsAll(listOf("YES","UNSURE","NO","REPEAT","SLOWER","PAUSE")))
    passed("SPOKEN_PROTOCOL_AND_PREFERENCE_LIMITS")

    run {
        val r=R(listOf(bin()));val rt=tutor(r)
        try {
            rt.start(cfg());until{rt.snapshot.value.scanChoice=="A" && rt.snapshot.value.responseWindowOpen}
            check(rt.onCommand(RuntimeCommand.PRIMARY));waitState(rt,LessonState.SESSION_END,5000)
            check(r.interactions.size==1 && r.interactions.single().response=="A")
            check(r.interactions.single().latencyMs==null);check(!rt.snapshot.value.responseWindowOpen)
            passed("SINGLE_CONTROL_ANSWER_NO_MOTOR_LATENCY_PENALTY")
        } finally {rt.shutdown()}
    }
    run {
        val r=R(listOf(bin()));val rt=tutor(r)
        try {
            rt.start(cfg());until{rt.snapshot.value.scanChoice=="A" && rt.snapshot.value.responseWindowOpen}
            check(rt.onScreenAnswer("B"));waitState(rt,LessonState.SESSION_END,5000)
            check(r.interactions.single().response=="B" && r.interactions.single().correct)
            passed("SCREEN_ANSWER_NOT_CURRENT_SCAN_CHOICE")
        }finally{rt.shutdown()}
    }
    run {
        val r=R(listOf(bin().copy(id="SELF",type=ExerciseType.SELF_ASSESS)));val rt=tutor(r)
        try {
            rt.start(cfg());until(10_000){rt.snapshot.value.scanChoice=="NO" && rt.snapshot.value.responseWindowOpen}
            rt.onCommand(RuntimeCommand.PRIMARY);waitState(rt,LessonState.SESSION_END,5000)
            check(r.interactions.single().confidence==ResponseConfidence.WRONG)
            passed("THREE_WAY_SELF_ASSESSMENT_ONE_CONTROL")
        }finally{rt.shutdown()}
    }
    run {
        val initial=LearnerKcState("G",mastery=.33,lastReviewedAtEpochMs=1234L,totalAttempts=2)
        val r=R(listOf(bin()),initial);val rt=tutor(r)
        try {
            rt.start(cfg(false).copy(policy=SessionPolicy(binaryResponseTimeoutMs=50,selfAssessmentTimeoutMs=50,maxTimeoutRetries=1)))
            waitState(rt,LessonState.PAUSED,5000)
            check(rt.snapshot.value.pauseReason==PauseReason.INACTIVITY)
            check(r.interactions.isEmpty() && r.states["G"]==initial)
            rt.stop();waitState(rt,LessonState.SESSION_END,3000)
            passed("DIRECT_SILENCE_PAUSES_WITHOUT_GRADING")
        }finally{rt.shutdown()}
    }
    run {
        class Rates:SpeechPort {val rates=mutableListOf<Float>();override suspend fun speak(text:String,languageTag:String,rate:Float){rates+=rate};override fun stop(){};override fun shutdown(){}}
        val speech=Rates();val r=R(listOf(bin()));val rt=tutor(r,speech)
        try {
            rt.start(cfg(false));waitState(rt,LessonState.WAITING_BINARY)
            rt.onCommand(RuntimeCommand.SLOWER)
            until{rt.snapshot.value.state==LessonState.WAITING_BINARY && rt.snapshot.value.helpDepth>0}
            check(r.interactions.isEmpty());check(speech.rates.any{it<.8f})
            check(rt.snapshot.value.captions.any{it.contains("despacio")})
            rt.onScreenAnswer("B");waitState(rt,LessonState.SESSION_END,5000)
            check(r.interactions.single().latencyMs==null)
            passed("REPLAY_SLOWER_CAPTIONS_NO_FAKE_ANSWER")
        }finally{rt.shutdown()}
    }
    run {
        val r=R(listOf(bin()));val rt=tutor(r)
        try {
            rt.start(cfg());until{rt.snapshot.value.responseWindowOpen}
            rt.pauseForRouteChange();delay(400)
            check(rt.snapshot.value.state==LessonState.PAUSED && !rt.snapshot.value.responseWindowOpen)
            check(!rt.onScreenAnswer("A"));check(r.interactions.isEmpty())
            rt.onCommand(RuntimeCommand.PLAY);until{rt.snapshot.value.responseWindowOpen}
            rt.stop();delay(500)
            check(rt.snapshot.value.state==LessonState.SESSION_END);check(r.interactions.isEmpty())
            passed("SCAN_CANCEL_ROUTE_LOSS_RESUME_STOP")
        }finally{rt.shutdown()}
    }
    run {
        val r=R(listOf(bin()));val rt=tutor(r)
        try {
            rt.start(cfg());waitState(rt,LessonState.PAUSED,30_000)
            check(rt.snapshot.value.pauseReason==PauseReason.INACTIVITY);check(r.interactions.isEmpty())
            passed("TWO_SCAN_ROUNDS_UNGRADED_PAUSE")
        }finally{rt.shutdown()}
    }
    println("HEADPHONES_ACCESSIBILITY: $tests test groups passed")
}
