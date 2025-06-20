package com.wayneenterprises.habitum.ui.screens.physical

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.wayneenterprises.habitum.sensors.AccelerometerSensorManager
import com.wayneenterprises.habitum.sensors.StepDetector
import com.wayneenterprises.habitum.sensors.StepListener
import com.wayneenterprises.habitum.viewmodel.StepViewModel
import com.wayneenterprises.habitum.viewmodel.StepCounter

@Composable
fun ActivityMenuScreen(
    navController: NavController,
    stepViewModel: StepViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // OBSERVAR DATOS GLOBALES PERSISTENTES
    val dailySteps by stepViewModel.dailySteps.collectAsState()
    val weeklySteps by stepViewModel.weeklySteps.collectAsState()
    val dailyGoal by stepViewModel.dailyGoal.collectAsState()

    // Estado para actividad seleccionada
    var selectedActivity by remember { mutableStateOf<String?>(null) }

    // USAR DETECTOR GLOBAL - NO reinicializar
    LaunchedEffect(Unit) {
        // Solo configurar si no existe ya
        if (StepCounter.getStepDetector() == null) {
            val stepDetector = StepDetector()
            val accelerometerManager = AccelerometerSensorManager(context, stepDetector)

            stepDetector.registerListener(object : StepListener {
                override fun step(timeNs: Long) {
                    stepViewModel.incrementDailyStep()
                }
            })

            // Guardar referencias globales
            StepCounter.setStepDetector(stepDetector)
            StepCounter.setAccelerometer(accelerometerManager)

            accelerometerManager.start()
            println("🏃 ActivityMenuScreen - Detector INICIADO (primera vez)")
        } else {
            println("🏃 ActivityMenuScreen - Usando detector existente")
        }

        // Inicializar datos (seguro de llamar múltiples veces)
        stepViewModel.initializeDailySteps()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Círculo principal con contador PERSISTENTE
        DailyStepsCirclePersistent(
            steps = dailySteps,
            goal = dailyGoal,
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Texto de meta y porcentaje CON DATOS PERSISTENTES
        val progress = if (dailyGoal > 0) (dailySteps.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f) else 0f
        val percentage = (progress * 100).toInt()

        Text(
            text = "Meta: ${String.format("%,d", dailyGoal)} pasos",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        if (dailySteps > 0) {
            Text(
                text = "¡Has alcanzado el $percentage% de tu meta!",
                fontSize = 16.sp,
                color = if (percentage >= 100) Color(0xFF4CAF50) else Color(0xFFFF9800),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "¡Comienza a caminar!",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botones de selección de actividad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActivitySelectionButton(
                text = "Caminata",
                backgroundColor = Color(0xFF4CAF50),
                isSelected = selectedActivity == "walking",
                modifier = Modifier.weight(1f),
                onClick = { selectedActivity = "walking" }
            )

            ActivitySelectionButton(
                text = "Carrera",
                backgroundColor = Color(0xFF2196F3),
                isSelected = selectedActivity == "running",
                modifier = Modifier.weight(1f),
                onClick = { selectedActivity = "running" }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // GRÁFICO SEMANAL FUNCIONAL CORREGIDO
        WeeklyStepsChartFunctional(
            weeklyData = weeklySteps,
            currentDaySteps = dailySteps, // Pasar pasos actuales
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp) // Más altura para mejor visualización
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón de iniciar actividad
        selectedActivity?.let { activity ->
            Button(
                onClick = {
                    when (activity) {
                        "walking" -> navController.navigate("walking_screen")
                        "running" -> navController.navigate("run_tracking")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (activity) {
                        "walking" -> Color(0xFF4CAF50)
                        "running" -> Color(0xFF2196F3)
                        else -> Color(0xFF4CAF50)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "▶ Iniciar ${if (activity == "walking") "Caminata" else "Carrera"}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        if (selectedActivity == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Selecciona una actividad para comenzar",
                    color = Color(0xFF1976D2),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DailyStepsCirclePersistent(
    steps: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Círculo de progreso
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 20.dp.toPx()

                // Círculo de fondo (gris claro)
                drawCircle(
                    color = Color(0xFFE0E0E0),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 16.dp.toPx())
                )

                // Círculo de progreso (naranja)
                if (progress > 0) {
                    drawArc(
                        color = Color(0xFFFF9800),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx()),
                        topLeft = Offset(
                            center.x - radius,
                            center.y - radius
                        ),
                        size = Size(radius * 2, radius * 2)
                    )
                }
            }

            // Contenido del círculo - MOSTRAR DATOS PERSISTENTES
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👣",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%,d", steps),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Pasos del día",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun ActivitySelectionButton(
    text: String,
    backgroundColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) backgroundColor else Color(0xFFE0E0E0)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun WeeklyStepsChartFunctional(
    weeklyData: List<Int>,
    currentDaySteps: Int,
    modifier: Modifier = Modifier
) {
    // Días de la semana ABREVIADOS
    val days = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do")

    // SOLO DATOS REALES - SIN SIMULACIÓN
    val realWeeklyData = remember(weeklyData, currentDaySteps) {
        val data = weeklyData.toMutableList()

        // Asegurar que tenemos 7 días
        while (data.size < 7) {
            data.add(0)
        }

        // Obtener día actual (0 = Lunes, 6 = Domingo)
        val calendar = java.util.Calendar.getInstance()
        val currentDay = when(calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> 0
            java.util.Calendar.TUESDAY -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY -> 3
            java.util.Calendar.FRIDAY -> 4
            java.util.Calendar.SATURDAY -> 5
            java.util.Calendar.SUNDAY -> 6
            else -> 0
        }

        // SOLO asegurar que el día actual tenga los pasos correctos
        data[currentDay] = currentDaySteps

        // NO SIMULAR DATOS - usar solo datos reales del StepViewModel
        println("📊 Datos REALES del gráfico - Día actual: $currentDay, Pasos hoy: $currentDaySteps")
        println("📊 Array REAL completo: $data")

        data
    }

    // Calcular máximo solo de datos reales (no ceros)
    val maxSteps = realWeeklyData.filter { it > 0 }.maxOrNull() ?: 1000

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Pasos Semanales",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Contenedor para las barras
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(7) { index ->
                    val steps = realWeeklyData.getOrNull(index) ?: 0
                    val barHeight = if (steps > 0 && maxSteps > 0) {
                        (steps.toFloat() / maxSteps * 100).dp.coerceAtLeast(8.dp)
                    } else {
                        8.dp // Altura mínima para días sin datos
                    }

                    // Obtener día actual para destacarlo
                    val calendar = java.util.Calendar.getInstance()
                    val currentDay = when(calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                        java.util.Calendar.MONDAY -> 0
                        java.util.Calendar.TUESDAY -> 1
                        java.util.Calendar.WEDNESDAY -> 2
                        java.util.Calendar.THURSDAY -> 3
                        java.util.Calendar.FRIDAY -> 4
                        java.util.Calendar.SATURDAY -> 5
                        java.util.Calendar.SUNDAY -> 6
                        else -> 0
                    }

                    val isToday = index == currentDay

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Valor de pasos SOLO si hay datos reales
                        if (steps > 0) {
                            Text(
                                text = if (steps >= 1000) "${steps / 1000}k" else steps.toString(),
                                fontSize = 10.sp,
                                color = if (isToday) Color(0xFF4CAF50) else Color.Gray,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Barra SOLO con datos reales
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(barHeight)
                                .background(
                                    color = when {
                                        isToday && steps > 0 -> Color(0xFF4CAF50) // Verde para hoy con pasos
                                        isToday && steps == 0 -> Color(0xFF81C784).copy(alpha = 0.3f) // Verde claro para hoy sin pasos
                                        steps > 0 -> Color(0xFF81C784) // Verde claro para otros días con pasos
                                        else -> Color(0xFFE0E0E0) // Gris para días sin datos
                                    },
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Día de la semana
                        Text(
                            text = days[index],
                            fontSize = 12.sp,
                            color = if (isToday) Color(0xFF4CAF50) else Color.Gray,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Información adicional SOLO con datos reales
            Spacer(modifier = Modifier.height(12.dp))

            val totalWeeklySteps = realWeeklyData.sum()
            val daysWithRealData = realWeeklyData.count { it > 0 }
            val averageSteps = if (daysWithRealData > 0) {
                totalWeeklySteps / daysWithRealData
            } else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total: ${String.format("%,d", totalWeeklySteps)}",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "Promedio: ${String.format("%,d", averageSteps)}",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}