package com.wayneenterprises.habitum.ui.componets.maps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wayneenterprises.habitum.ui.screens.physical.RunState

@Composable
fun RunStatusCard(
    runState: RunState,
    isSelectingStart: Boolean, // Mantengo por compatibilidad pero no se usa
    elapsedTime: Long,
    totalDistance: Double,
    stepCount: Int
) {
    // ✅ DEBUG: Para detectar duplicaciones
    println("🔍 RunStatusCard renderizada - Estado: $runState - Tiempo: ${System.currentTimeMillis()}")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (runState) {
                RunState.SETUP -> MaterialTheme.colorScheme.primaryContainer
                RunState.READY -> MaterialTheme.colorScheme.secondaryContainer
                RunState.RUNNING -> MaterialTheme.colorScheme.errorContainer
                RunState.FINISHED -> MaterialTheme.colorScheme.tertiaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Estado principal
            val status = when (runState) {
                RunState.SETUP -> "📍 Selecciona tu destino en el mapa"
                RunState.READY -> "✅ ¡Todo listo! Presiona Iniciar Carrera"
                RunState.RUNNING -> "🏃‍♂️ Carrera en progreso"
                RunState.FINISHED -> "🎉 ¡Carrera completada!"
            }

            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (runState) {
                    RunState.SETUP -> MaterialTheme.colorScheme.onPrimaryContainer
                    RunState.READY -> MaterialTheme.colorScheme.onSecondaryContainer
                    RunState.RUNNING -> MaterialTheme.colorScheme.onErrorContainer
                    RunState.FINISHED -> MaterialTheme.colorScheme.onTertiaryContainer
                }
            )

            // Métricas durante la carrera
            if (runState in listOf(RunState.RUNNING, RunState.FINISHED)) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoColumn(
                        label = "Tiempo",
                        value = "${elapsedTime / 60}:${String.format("%02d", elapsedTime % 60)}",
                        icon = "⏱️"
                    )
                    InfoColumn(
                        label = "Distancia (Pasos)",
                        value = "${String.format("%.0f", totalDistance)}m",
                        icon = "📏"
                    )
                    InfoColumn(
                        label = "Pasos",
                        value = stepCount.toString(),
                        icon = "👣"
                    )
                }
            }

            // Información adicional según el estado
            when (runState) {
                RunState.SETUP -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tu carrera comenzará desde tu ubicación actual automáticamente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                RunState.READY -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ruta calculada por calles. Inicio: Tu ubicación → Destino: Punto seleccionado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }

                RunState.RUNNING -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Distancia calculada por pasos detectados. Sigue la línea azul.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }

                RunState.FINISHED -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    val avgPace = if (elapsedTime > 0) (elapsedTime.toDouble() / (totalDistance / 1000)) / 60 else 0.0
                    Text(
                        text = "Ritmo promedio: ${String.format("%.1f", avgPace)} min/km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}