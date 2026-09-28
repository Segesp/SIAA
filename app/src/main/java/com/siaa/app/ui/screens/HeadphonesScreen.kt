package com.siaa.app.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siaa.app.ui.ActionButton
import com.siaa.app.ui.CalibrationStep
import com.siaa.app.ui.Heading
import com.siaa.app.ui.MainViewModel
import com.siaa.app.ui.ToggleRow
import com.siaa.core.model.AudioRouteType

@Composable
fun HeadphonesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val route by viewModel.audioPlayer.currentRoute.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()
    val step by viewModel.calibrationStep.collectAsState()
    val detectedKeyName by viewModel.lastDetectedKeyName.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Heading(
                text = "Tus audífonos, tus controles",
                subtitle = "Configura y ensaya tus gestos para responder durante tu trayecto con el teléfono guardado."
            )
        }

        // Live Audio Output Card
        item {
            AudioRouteCard(
                route = route,
                onConfigureClick = { viewModel.playSoundCheck() }
            )
        }

        // Guided Configuration Wizard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Guía de calibración de audífonos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 1: Sound Check
                    CalibrationStepRow(
                        stepNumber = "1",
                        title = "Prueba de sonido privada",
                        description = "Escucha una frase de confirmación en tus audífonos.",
                        isActive = (step == CalibrationStep.STEP_SOUND_CHECK),
                        action = {
                            Button(
                                onClick = { viewModel.playSoundCheck() },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Probar sonido")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step 2: Button Detection
                    CalibrationStepRow(
                        stepNumber = "2",
                        title = "Detección de controles",
                        description = "Presiona el botón de tus audífonos para reconocerlo.",
                        isActive = (step == CalibrationStep.STEP_BUTTON_MAPPING),
                        action = {
                            Column {
                                if (detectedKeyName != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$detectedKeyName asignado",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.recordDetectedKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                                        },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Presiona botón o Pulsa aquí")
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step 3: Ungraded Practice Trial
                    CalibrationStepRow(
                        stepNumber = "3",
                        title = "Ensayo sin nota",
                        description = "Prueba responder una pregunta con tus audífonos sin calificar.",
                        isActive = (step == CalibrationStep.STEP_TRIAL),
                        action = {
                            Button(
                                onClick = { viewModel.setCalibrationStep(CalibrationStep.STEP_TRIAL) },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Iniciar ensayo")
                            }
                        }
                    )
                }
            }
        }

        // Behavior and Mode Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = "Comportamiento de los controles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    ToggleRow(
                        label = "Modo de un solo botón (locución cíclica)",
                        checked = profile.singleButtonMode,
                        onCheckedChange = { viewModel.updateProfile(profile.copy(singleButtonMode = it)) }
                    )

                    ToggleRow(
                        label = "Pausar mi sesión cuando deje de responder a las opciones",
                        checked = !profile.autoRepeatOptions,
                        onCheckedChange = { viewModel.updateProfile(profile.copy(autoRepeatOptions = !it)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Silence timeout
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Tiempo de espera por silencio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(8, 12, 16).forEach { sec ->
                                FilterChip(
                                    selected = (profile.silenceTimeoutSeconds == sec),
                                    onClick = { viewModel.updateProfile(profile.copy(silenceTimeoutSeconds = sec)) },
                                    label = { Text("$sec s") },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice speed
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Velocidad de locución de opciones",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.85f to "0.85x", 1.0f to "1.0x (Normal)", 1.15f to "1.15x").forEach { (speed, label) ->
                                FilterChip(
                                    selected = (profile.voiceSpeed == speed),
                                    onClick = { viewModel.updateProfile(profile.copy(voiceSpeed = speed)) },
                                    label = { Text(label) },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CalibrationStepRow(
    stepNumber: String,
    title: String,
    description: String,
    isActive: Boolean,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            action()
        }
    }
}
