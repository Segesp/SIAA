package com.siaa.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siaa.app.ui.ActionButton
import com.siaa.app.ui.MainViewModel
import com.siaa.app.ui.SessionState
import com.siaa.core.model.ExerciseOption

@Composable
fun SessionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val exercise by viewModel.currentExercise.collectAsState()
    val currentIndex by viewModel.currentExerciseIndex.collectAsState()
    val score by viewModel.sessionScore.collectAsState()
    val isTrial by viewModel.isTrialMode.collectAsState()
    val activeSpokenOptionIndex by viewModel.activeSpokenOptionIndex.collectAsState()
    val lastCorrect by viewModel.lastAnswerCorrect.collectAsState()
    val profile by viewModel.deviceProfile.collectAsState()

    if (sessionState == SessionState.FINISHED) {
        SessionFinishedView(
            score = score,
            isTrial = isTrial,
            onReturnHome = { viewModel.exitSessionToHome() }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.exitSessionToHome() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar y salir de la sesión"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isTrial) "Ensayo de audífonos" else "Ejercicio ${currentIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isTrial) "Sin calificación" else "Aciertos: $score",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = exercise?.level ?: "A1",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (isTrial) 1f else ((currentIndex + 1).toFloat() / 10f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main Exercise & Audio Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Audio Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when (sessionState) {
                                SessionState.PLAYING_PROMPT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                SessionState.ANNOUNCING_OPTIONS -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                SessionState.WAITING_ANSWER -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                SessionState.SHOWING_FEEDBACK -> if (lastCorrect == true) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (sessionState) {
                            SessionState.PLAYING_PROMPT -> "Reproduciendo audio original..."
                            SessionState.ANNOUNCING_OPTIONS -> "Locución de opciones..."
                            SessionState.WAITING_ANSWER -> "Esperando respuesta en audífonos..."
                            SessionState.SHOWING_FEEDBACK -> if (lastCorrect == true) "¡Respuesta Correcta!" else "Respuesta incorrecta"
                            SessionState.PAUSED -> "Sesión en Pausa"
                            else -> "Listo"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Transcript text (always accessible on screen)
                Text(
                    text = exercise?.text ?: "Cargando ejercicio...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = exercise?.questionPrompt ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hands-Free Guidance Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (profile.singleButtonMode)
                        "Modo 1 botón: Las opciones se anuncian una a una. Pulsa una vez cuando escuches tu respuesta."
                    else
                        "Presiona los botones de tus audífonos (Centro = Opción 1, Siguiente = Opción 2, Anterior = Opción 3) o toca la pantalla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Options List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val options = exercise?.options ?: emptyList()
            items(options) { opt ->
                val isHighlighted = (profile.singleButtonMode && activeSpokenOptionIndex == opt.index && sessionState == SessionState.WAITING_ANSWER)
                val isCorrectAnswer = (exercise?.correctIndex == opt.index)
                val isFeedback = (sessionState == SessionState.SHOWING_FEEDBACK)

                OptionItemCard(
                    option = opt,
                    isHighlighted = isHighlighted,
                    isFeedback = isFeedback,
                    isCorrect = isCorrectAnswer,
                    onClick = {
                        if (sessionState == SessionState.WAITING_ANSWER || sessionState == SessionState.ANNOUNCING_OPTIONS) {
                            viewModel.submitAnswer(opt.index)
                        }
                    }
                )
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.repeatCurrentAudio() },
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Replay, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Repetir")
            }

            Button(
                onClick = {
                    if (sessionState == SessionState.PAUSED) {
                        viewModel.resumeSession()
                    } else {
                        viewModel.pauseSession()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sessionState == SessionState.PAUSED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (sessionState == SessionState.PAUSED) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (sessionState == SessionState.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (sessionState == SessionState.PAUSED) "Continuar" else "Pausar")
            }
        }
    }
}

@Composable
fun OptionItemCard(
    option: ExerciseOption,
    isHighlighted: Boolean,
    isFeedback: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isFeedback && isCorrect -> Color(0xFF10B981)
        isFeedback && !isCorrect -> Color.Transparent
        isHighlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val containerColor = when {
        isFeedback && isCorrect -> Color(0xFF10B981).copy(alpha = 0.15f)
        isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .border(
                width = if (isHighlighted || (isFeedback && isCorrect)) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFeedback && isCorrect) Color(0xFF10B981)
                        else if (isHighlighted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.keyBadge,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFeedback && isCorrect || isHighlighted) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.earbudHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SessionFinishedView(
    score: Int,
    isTrial: Boolean,
    onReturnHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isTrial) "¡Ensayo de Audífonos Completado!" else "¡Sesión de Práctica Completada!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isTrial)
                "Tus controles de audífonos responden perfectamente. Ya estás listo para practicar en tu viaje diario sin mirar la pantalla."
            else
                "Has completado tu recorrido con $score respuestas correctas. Tu progreso ha sido guardado.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        ActionButton(
            text = "Volver al Inicio",
            onClick = onReturnHome
        )
    }
}
