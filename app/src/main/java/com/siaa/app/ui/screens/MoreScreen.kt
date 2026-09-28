package com.siaa.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siaa.app.ui.Heading
import com.siaa.app.ui.MainViewModel
import com.siaa.app.ui.ToggleRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val highContrast by viewModel.highContrast.collectAsState()
    val history by viewModel.sessionHistory.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Heading(
                text = "Más opciones y Accesibilidad",
                subtitle = "Configura el contraste visual, revisa tu historial y conoce el método SIAA."
            )
        }

        // Accessibility Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessibilityNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Opciones de Accesibilidad",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    ToggleRow(
                        label = "Modo de alto contraste (fondo oscuro y amarillo)",
                        checked = highContrast,
                        onCheckedChange = { viewModel.toggleHighContrast(it) }
                    )

                    ToggleRow(
                        label = "Silenciar avisos automáticos si TalkBack está activo",
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            }
        }

        // History Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
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
                            text = "Historial de Práctica",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (history.isEmpty()) {
                        Text(
                            text = "Aún no has completado ninguna sesión. ¡Empieza una desde la pestaña de Inicio!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        history.take(10).forEachIndexed { index, session ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = DividerDefaults.color.copy(alpha = 0.3f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${session.modality} · Nivel ${session.level}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                                        .format(Date(session.timestamp))
                                    Text(
                                        text = "$dateStr · ${session.durationMinutes} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${session.correctAnswers}/${session.totalQuestions} aciertos",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // About & Methodology Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Método SIAA (Audífonos y Accesibilidad)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Diseñado específicamente para aprender inglés durante traslados en transporte público.\n" +
                                "• El teléfono puede permanecer guardado en el bolsillo o bolso.\n" +
                                "• No requiere hablar en voz alta ni activar micrófonos en público.\n" +
                                "• Utiliza banco offline con miles de expresiones auténticas indexadas según el marco MCER (A1 a C2).\n" +
                                "• Controles físicos de 56 dp o más y compatibilidad completa con TalkBack.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
