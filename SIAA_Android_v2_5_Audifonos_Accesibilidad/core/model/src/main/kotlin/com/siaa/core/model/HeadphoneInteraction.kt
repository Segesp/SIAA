package com.siaa.core.model

/** Physical tap counts belong to headset firmware, never inferred from timing here. */
enum class HeadphoneMode { AUTO, DIRECT, SINGLE_SWITCH }

data class HeadphonePreferences(
    val mode: HeadphoneMode = HeadphoneMode.AUTO,
    val primaryGesture: String = "tu gesto principal",
    val secondaryGesture: String = "tu segundo gesto",
    val backGesture: String = "tu tercer gesto",
    val responseSeconds: Int = 20,
    val scanSeconds: Int = 4,
    val transitionMillis: Long = 700,
    val pauseOnSilence: Boolean = true,
    val captionsEnabled: Boolean = true,
    val simpleHome: Boolean = true,
    val highContrast: Boolean = false,
    val setupCompleted: Boolean = false,
    val confirmedRoute: String = ""
) {
    fun normalized() = copy(
        primaryGesture = cleanLabel(primaryGesture, "tu gesto principal"),
        secondaryGesture = cleanLabel(secondaryGesture, "tu segundo gesto"),
        backGesture = cleanLabel(backGesture, "tu tercer gesto"),
        responseSeconds = responseSeconds.coerceIn(12, 90),
        scanSeconds = scanSeconds.coerceIn(2, 10),
        transitionMillis = transitionMillis.coerceIn(300, 2500)
    )
    companion object {
        private fun cleanLabel(text: String, fallback: String): String =
            text.replace(Regex("[\\r\\n\\t]+"), " ").trim().take(70).ifBlank { fallback }
    }
}

object HeadphoneCompatibility {
    fun validate(profile: DeviceProfile?): String? {
        if (profile == null || !profile.playPauseAvailable || profile.primaryKeyCode == null)
            return "Primero comprueba un gesto de tus audífonos."
        if (listOfNotNull(profile.primaryKeyCode, profile.secondaryKeyCode, profile.backKeyCode).any { it !in setOf(79, 85, 87, 88, 126) })
            return "Ese control no es un botón multimedia compatible. Comprueba otro gesto."
        val codes = listOfNotNull(profile.primaryKeyCode, profile.secondaryKeyCode, profile.backKeyCode, profile.stopKeyCode)
        if (codes.distinct().size != codes.size) return "Dos funciones usan el mismo control. Repite la configuración."
        if (listOfNotNull(profile.primaryKeyCode, profile.secondaryKeyCode, profile.backKeyCode).any { it in setOf(86, 127) }) return "Detener y Pausa deben conservar su función de seguridad."
        if (profile.nextAvailable != (profile.secondaryKeyCode != null) ||
            profile.previousAvailable != (profile.backKeyCode != null)) return "El perfil de controles está incompleto."
        return null
    }

    fun useScanning(mode: HeadphoneMode, profile: DeviceProfile): Boolean = when (mode) {
        HeadphoneMode.SINGLE_SWITCH -> true
        HeadphoneMode.DIRECT -> false
        HeadphoneMode.AUTO -> !profile.nextAvailable || !profile.previousAvailable
    }

    fun capabilities(mode: HeadphoneMode, profile: DeviceProfile): SessionCapabilities = SessionCapabilities(
        hasPrimary = profile.playPauseAvailable,
        hasSecondary = profile.nextAvailable,
        hasBack = profile.previousAvailable,
        singleSwitchScanning = useScanning(mode, profile)
    )
}
