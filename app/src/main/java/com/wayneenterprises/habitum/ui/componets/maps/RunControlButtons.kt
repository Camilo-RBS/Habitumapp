package com.wayneenterprises.habitum.ui.componets.maps

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayneenterprises.habitum.ui.screens.physical.RunState
import com.wayneenterprises.habitum.viewmodel.RunMapViewModel
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

@Composable
fun RunControlButtons(
    viewModel: RunMapViewModel,
    mapView: MapView?,
    polyline: Polyline
) {
    val runState = viewModel.runState.value
    val hasArrivedAtDestination = viewModel.hasArrivedAtDestination.value
    val isLoadingRoute = viewModel.isLoadingRoute.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (runState) {
            RunState.SETUP -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Text(
                        text = if (viewModel.isSelectingDestination.value) {
                            "🎯 Toca en el mapa para marcar tu DESTINO"
                        } else {
                            "⏳ Preparando ruta..."
                        },
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF1976D2),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            RunState.READY -> {
                Button(
                    onClick = {
                        viewModel.startRun()
                        polyline.setPoints(emptyList())
                        mapView?.overlays?.forEach { overlay ->
                            if (overlay is Polyline && overlay != polyline) {
                                mapView.overlays.remove(overlay)
                            }
                        }
                        mapView?.overlays?.add(polyline)
                        mapView?.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    enabled = !isLoadingRoute
                ) {
                    if (isLoadingRoute) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calculando...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🚀 INICIAR CARRERA",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        println("🔄 Seleccionando nuevo destino...")
                        viewModel.reset()
                        mapView?.let { map ->
                            map.overlays.clear()
                            map.invalidate()
                        }
                        println("✅ Listo para nuevo destino")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoadingRoute
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seleccionar Nuevo Destino")
                }
            }

            RunState.RUNNING -> {
                if (!hasArrivedAtDestination) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🏃‍♂️ Carrera en progreso",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sigue la línea azul hasta llegar a tu destino. La carrera terminará automáticamente.",
                                fontSize = 12.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.finishRun() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⏹️ TERMINAR CARRERA",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            RunState.FINISHED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            println("🔄 Iniciando nueva carrera...")
                            viewModel.reset()
                            mapView?.let { map ->
                                map.overlays.clear()
                                map.invalidate()
                                viewModel.currentLocation.value?.let { location ->
                                    viewModel.setStartPointAutomatically()
                                    map.controller.setCenter(location)
                                    map.controller.setZoom(15.0)
                                }
                            }
                            println("✅ Nueva carrera lista")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Nueva Carrera", fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            println("🗺️ Mostrando ruta completa...")

                            // 🔥 MEJORADO: Usar la nueva función getCompleteRoute()
                            val completeRoute = viewModel.getCompleteRoute()

                            if (completeRoute.isNotEmpty()) {
                                println("📍 Puntos de ruta: ${completeRoute.size}")

                                // Crear bounding box con todos los puntos de la ruta
                                val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(completeRoute)
                                mapView?.zoomToBoundingBox(bounds, true, 100)

                                // Asegurar zoom mínimo para que se vea bien
                                mapView?.let { map ->
                                    if (map.zoomLevelDouble < 14.0) {
                                        map.controller.setZoom(14.0)
                                    }
                                }

                                println("✅ Ruta completa mostrada")
                            } else {
                                println("⚠️ No hay ruta para mostrar")

                                // Fallback: mostrar entre inicio y fin si existen
                                val start = viewModel.startPoint.value
                                val end = viewModel.endPoint.value

                                if (start != null && end != null) {
                                    val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(listOf(start, end))
                                    mapView?.zoomToBoundingBox(bounds, true, 100)
                                    println("🔄 Mostrando entre inicio y fin")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Ver Ruta", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}