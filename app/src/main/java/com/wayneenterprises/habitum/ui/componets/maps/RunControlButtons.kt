package com.wayneenterprises.habitum.ui.componets.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayneenterprises.habitum.ui.screens.physical.RunState
import com.wayneenterprises.habitum.viewmodel.RunMapViewModel
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

// 🎨 Paleta de colores actualizada
object EnhancedRunColors {
    val PrimaryPink = Color(0xFFE91E63)
    val SecondaryBlue = Color(0xFF2196F3)
    val AccentPurple = Color(0xFF9C27B0)
    val SoftCyan = Color(0xFF00BCD4)
    val WarmOrange = Color(0xFFFF9800)
    val SuccessGreen = Color(0xFF4CAF50)
    val DangerRed = Color(0xFFFF5722)

    // Gradientes mejorados SIN sombras problemáticas
    val StartGradient = listOf(SuccessGreen, Color(0xFF388E3C))
    val StopGradient = listOf(DangerRed, Color(0xFFD32F2F))
    val SecondaryGradient = listOf(SecondaryBlue, Color(0xFF1976D2))
    val InfoGradient = listOf(SoftCyan, Color(0xFF00ACC1))
}

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (runState) {
            RunState.SETUP -> {
                EnhancedInfoCard(
                    message = if (viewModel.isSelectingDestination.value) {
                        "🎯 Toca en el mapa para marcar tu DESTINO"
                    } else {
                        "⏳ Preparando ruta..."
                    },
                    gradient = EnhancedRunColors.InfoGradient
                )
            }

            RunState.READY -> {
                // 🎨 Botón principal MEJORADO - Sin sombras problemáticas
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
                        .height(60.dp),
                    enabled = !isLoadingRoute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isLoadingRoute) {
                                    Brush.horizontalGradient(
                                        listOf(Color.Gray.copy(alpha = 0.6f), Color.Gray.copy(alpha = 0.4f))
                                    )
                                } else {
                                    Brush.horizontalGradient(EnhancedRunColors.StartGradient)
                                },
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingRoute) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Calculando...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "🚀 INICIAR CARRERA",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 🎨 Botón secundario MEJORADO
                OutlinedButton(
                    onClick = {
                        viewModel.reset()
                        mapView?.let { map ->
                            map.overlays.clear()
                            map.invalidate()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoadingRoute,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        EnhancedRunColors.SecondaryBlue
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EnhancedRunColors.SecondaryBlue
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = EnhancedRunColors.SecondaryBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seleccionar Nuevo Destino",
                            color = EnhancedRunColors.SecondaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            RunState.RUNNING -> {
                if (!hasArrivedAtDestination) {
                    // 🎨 Card de estado en progreso
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            EnhancedRunColors.PrimaryPink.copy(alpha = 0.1f),
                                            EnhancedRunColors.WarmOrange.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Text(
                                    text = "🏃‍♂️ Carrera en Progreso",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EnhancedRunColors.PrimaryPink
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Sigue la línea rosa hasta llegar a tu destino. La carrera terminará automáticamente al llegar.",
                                    fontSize = 14.sp,
                                    color = EnhancedRunColors.PrimaryPink.copy(alpha = 0.8f),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 🎨 Botón de terminar CORREGIDO - Sin sombras problemáticas
                    Button(
                        onClick = { viewModel.finishRun() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(EnhancedRunColors.StopGradient),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "⏹️ TERMINAR CARRERA",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            RunState.FINISHED -> {
                // 🎨 Botones finales COMPLETAMENTE CORREGIDOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ✅ Botón Nueva Carrera - MEJORADO
                    Button(
                        onClick = {
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
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(EnhancedRunColors.SecondaryGradient),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Nueva Carrera",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // ✅ Botón Ver Ruta - MEJORADO
                    OutlinedButton(
                        onClick = {
                            val completeRoute = viewModel.getCompleteRoute()
                            if (completeRoute.isNotEmpty()) {
                                val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(completeRoute)
                                mapView?.zoomToBoundingBox(bounds, true, 100)
                                mapView?.let { map ->
                                    if (map.zoomLevelDouble < 14.0) {
                                        map.controller.setZoom(14.0)
                                    }
                                }
                            } else {
                                val start = viewModel.startPoint.value
                                val end = viewModel.endPoint.value
                                if (start != null && end != null) {
                                    val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(listOf(start, end))
                                    mapView?.zoomToBoundingBox(bounds, true, 100)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            EnhancedRunColors.AccentPurple
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EnhancedRunColors.AccentPurple,
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = EnhancedRunColors.AccentPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ver Ruta",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EnhancedRunColors.AccentPurple
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedInfoCard(
    message: String,
    gradient: List<Color>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        gradient.map { it.copy(alpha = 0.1f) }
                    )
                )
                .padding(20.dp)
        ) {
            Text(
                text = message,
                color = gradient.first(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            )
        }
    }
}