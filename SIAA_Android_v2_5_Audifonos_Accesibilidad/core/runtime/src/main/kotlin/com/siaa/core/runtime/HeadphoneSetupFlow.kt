package com.siaa.core.runtime

import com.siaa.core.model.DeviceProfile

enum class SetupStep { IDLE, PRIMARY, CONFIRM_PRIMARY, SECONDARY, CONFIRM_SECONDARY, BACK, CONFIRM_BACK, REHEARSAL, READY }
data class HeadphoneSetupState(
    val step: SetupStep = SetupStep.IDLE,
    val primary: Int? = null, val secondary: Int? = null, val back: Int? = null,
    val message: String = "Conecta tus audífonos y comienza la comprobación.",
    val successfulChecks: Int = 0
) {
    val active: Boolean get() = step !in setOf(SetupStep.IDLE, SetupStep.READY)
    fun profile(name: String, now: Long): DeviceProfile? = if (step != SetupStep.READY || primary == null) null else DeviceProfile(
        name = name.trim().take(80).ifBlank { "Mis audífonos" }, primaryKeyCode = primary,
        secondaryKeyCode = secondary, backKeyCode = back, stopKeyCode = null,
        playPauseAvailable = true, nextAvailable = secondary != null, previousAvailable = back != null,
        lastSeenAtEpochMs = now
    )
}

/** Deterministic, ungraded rehearsal: never touches the learning repository. */
object HeadphoneSetupFlow {
    fun begin() = HeadphoneSetupState(step = SetupStep.PRIMARY, message = "Haz el gesto de tus audífonos que usarás para elegir una respuesta.")
    fun receive(s: HeadphoneSetupState, code: Int): HeadphoneSetupState {
        if (!s.active) return s
        if (code in setOf(86,127)) return s.copy(message = "Ese control pausa o detiene. Prueba el gesto de reproducir/pausar, no una orden de pausa independiente.")
        if (code !in setOf(79,85,87,88,126)) return s.copy(message="Ese botón no sirve para responder. Prueba un gesto multimedia de tus audífonos.")
        return when(s.step) {
            SetupStep.PRIMARY -> s.copy(step=SetupStep.CONFIRM_PRIMARY, primary=code, message="Recibido. Repite el mismo gesto para comprobarlo.")
            SetupStep.CONFIRM_PRIMARY -> if(code == s.primary) s.copy(step=SetupStep.SECONDARY, successfulChecks=s.successfulChecks+1, message="Primer gesto comprobado. Prueba otro gesto distinto o pulsa ‘No tengo otro’. ") else s.copy(message="Llegó un control distinto. Repite el primer gesto o comienza de nuevo.")
            SetupStep.SECONDARY -> if(code == s.primary) s.copy(message="Es el mismo control. Prueba otro o pulsa ‘No tengo otro’.") else s.copy(step=SetupStep.CONFIRM_SECONDARY,secondary=code,message="Repite el segundo gesto.")
            SetupStep.CONFIRM_SECONDARY -> if(code == s.secondary) s.copy(step=SetupStep.BACK,successfulChecks=s.successfulChecks+1,message="Segundo gesto comprobado. Prueba un tercero para repetir, o pulsa ‘No tengo otro’.") else s.copy(message="No coincide. Repite el segundo gesto o continúa sin él.")
            SetupStep.BACK -> if(code in listOf(s.primary,s.secondary)) s.copy(message="Este control ya tiene una función. Prueba otro o continúa sin él.") else s.copy(step=SetupStep.CONFIRM_BACK,back=code,message="Repite el tercer gesto.")
            SetupStep.CONFIRM_BACK -> if(code == s.back) rehearsal(s.copy(successfulChecks=s.successfulChecks+1)) else s.copy(message="No coincide. Repite el tercer gesto o continúa sin él.")
            SetupStep.REHEARSAL -> if(code == s.primary) s.copy(step=SetupStep.READY,successfulChecks=s.successfulChecks+1,message="Ensayo completado. Puedes guardar tus controles; no se ha modificado tu progreso.") else s.copy(message="Para terminar el ensayo, usa tu primer gesto.")
            else -> s
        }
    }
    fun skip(s: HeadphoneSetupState): HeadphoneSetupState = when(s.step) {
        SetupStep.SECONDARY, SetupStep.CONFIRM_SECONDARY -> rehearsal(s.copy(secondary=null,back=null))
        SetupStep.BACK, SetupStep.CONFIRM_BACK -> rehearsal(s.copy(back=null))
        else -> s
    }
    private fun rehearsal(s: HeadphoneSetupState) = s.copy(step=SetupStep.REHEARSAL,message="Ensayo sin nota: imagina que eliges una respuesta. Usa de nuevo tu primer gesto.")
}
