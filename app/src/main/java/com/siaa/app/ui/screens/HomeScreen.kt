package com.siaa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siaa.app.ui.ActionButton
import com.siaa.app.ui.AppTab
import com.siaa.app.ui.Heading
import com.siaa.app.ui.MainViewModel
import com.siaa.core.model.AudioRouteType

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val route by viewModel.audioPlayer.currentRoute.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val selectedModality by viewModel.selectedModality.collectAsState()
    val selectedDuration by viewModel.selectedDuration.collectAsState()
    val history by viewModel.sessionHistory.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()

    val levels = listOf("TODOS", "A1", "A2", "B1", "B2", "C1", "C2")
    val modalities = listOf("Mixto", "Escucha", "Fonología", "Frases")
    val durations = listOf(5, 10, 15, 20)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Heading(
                text = "Aprende en tu viaje",
                subtitle = "Práctica auditiva con audífonos, sin mirar el móvil ni hablar en voz alta."
            )
        }

        // Audio Route Status Banner
        item {
            AudioRouteCard(
                route = route,
                onConfigureClick = { viewModel.selectTab(AppTab.HEADPHONES) }
            )
        }

        // Quick Earbud Setup Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectTab(AppTab.HEADPHONES) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Configurar mis audífonos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (profile.singleButtonMode) "Modo: 1 botón (opciones cíclicas)" else "Modo: 3 controles calibrados",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ir a configuración de audífonos",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Level Selector
        item {
            Column {
                Text(
                    text = "Nivel MCER",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(levels) { level ->
                        FilterChip(
                            selected = (selectedLevel == level),
                            onClick = { viewModel.setLevel(level) },
                            label = { Text(level) },
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

        // Modality Selector
        item {
            Column {
                Text(
                    text = "Modalidad de Práctica",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modalities.forEach { modality ->
                        FilterChip(
                            selected = (selectedModality == modality),
                            onClick = { viewModel.setModality(modality) },
                            label = { Text(modality) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // Duration Selector
        item {
            Column {
                Text(
                    text = "Duración del trayecto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durations.forEach { minutes ->
                        FilterChip(
                            selected = (selectedDuration == minutes),
                            onClick = { viewModel.setDuration(minutes) },
                            label = { Text("$minutes min") },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // Start Session ActionButton
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ActionButton(
                text = "Comenzar mi sesión",
                modifier = Modifier.testTag("start_session_button"),
                onClick = { viewModel.startSession(isTrial = false) }
            )
        }

        // Recent stats summary
        item {
            StatsSummaryCard(history = history)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AudioRouteCard(
    route: AudioRouteType,
    onConfigureClick: () -> Unit
) {
    val isSecure = route == AudioRouteType.HEADPHONES_BLUETOOTH || route == AudioRouteType.HEADPHONES_WIRED
    val containerColor = if (isSecure) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    }

    val icon = when (route) {
        AudioRouteType.HEADPHONES_BLUETOOTH -> Icons.Default.Headphones
        AudioRouteType.HEADPHONES_WIRED -> Icons.Default.Headphones
        AudioRouteType.DEVICE_SPEAKER -> Icons.Default.VolumeUp
        AudioRouteType.UNKNOWN -> Icons.Default.Warning
    }

    val title = when (route) {
        AudioRouteType.HEADPHONES_BLUETOOTH -> "Audífonos Bluetooth conectados"
        AudioRouteType.HEADPHONES_WIRED -> "Audífonos con cable conectados"
        AudioRouteType.DEVICE_SPEAKER -> "¡Atención! Altavoz del teléfono activo"
        AudioRouteType.UNKNOWN -> "Estado de audio desconocido"
    }

    val subtitle = if (isSecure) {
        "Ruta privada confirmada. Listo para interactuar con botones."
    } else {
        "Conecta tus audífonos para practicar con privacidad en el transporte."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConfigureClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatsSummaryCard(history: List<com.siaa.core.model.SessionResult>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Progreso de Práctica",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatColumn(label = "Sesiones", value = history.size.toString())
                val totalAnswers = history.sumOf { it.totalQuestions }
                val totalCorrect = history.sumOf { it.correctAnswers }
                val accuracy = if (totalAnswers > 0) "${(totalCorrect * 100) / totalAnswers}%" else "0%"
                StatColumn(label = "Aciertos", value = accuracy)
                val totalMinutes = history.sumOf { it.durationMinutes }
                StatColumn(label = "Tiempo", value = "$totalMinutes min")
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
