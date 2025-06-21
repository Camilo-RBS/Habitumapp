package com.wayneenterprises.habitum.ui.screens.physical

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.wayneenterprises.habitum.services.StepCounterService
import com.wayneenterprises.habitum.viewmodel.StepViewModel

// 🎨 Nueva paleta de colores para ActivityMenu
object ActivityMenuColors {
    val PrimaryPink = Color(0xFFE91E63)
    val SecondaryBlue = Color(0xFF2196F3)
    val AccentPurple = Color(0xFF9C27B0)
    val SoftCyan = Color(0xFF00BCD4)
    val WarmOrange = Color(0xFFFF9800)
    val SuccessGreen = Color(0xFF4CAF50)
    val LightGray = Color(0xFFF5F5F5)

    // Gradientes para el círculo de progreso
    val ProgressGradient = listOf(PrimaryPink, WarmOrange, SecondaryBlue)
    val BackgroundGradient = listOf(Color.White, LightGray)
}

@Composable
fun ActivityMenuScreen(
    navController: NavController,
    stepViewModel: StepViewModel = StepViewModel.getInstance()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        println("🏃 ActivityMenuScreen - Conectando con servicio global")

        // Actualizar la referencia del ViewModel en el servicio
        StepCounterService.updateViewModel(stepViewModel)

        // Asegurar que el servicio esté activo
        if (!StepCounterService.isActive()) {
            println("🔄 ActivityMenuScreen - Reactivando servicio")
            StepCounterService.startDetection()
        }

        println("📊 ActivityMenuScreen - Status del servicio: ${StepCounterService.getStatus()}")

        // Inicializar datos si es necesario
        stepViewModel.initializeDailySteps()
    }

    // OBSERVAR DATOS GLOBALES PERSISTENTES
    val dailySteps by stepViewModel.dailySteps.collectAsState()
    val weeklySteps by stepViewModel.weeklySteps.collectAsState()
    val dailyGoal by stepViewModel.dailyGoal.collectAsState()
    val isLoading by stepViewModel.isLoading.collectAsState()

    var selectedActivity by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(ActivityMenuColors.BackgroundGradient)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            EnhancedActivityHeader()

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ActivityMenuColors.PrimaryPink
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sincronizando datos...",
                            color = ActivityMenuColors.AccentPurple,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            EnhancedDailyStepsCircle(
                steps = dailySteps,
                goal = dailyGoal,
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            EnhancedProgressInfo(
                steps = dailySteps,
                goal = dailyGoal
            )

            Spacer(modifier = Modifier.height(28.dp))

            EnhancedActivitySelection(
                selectedActivity = selectedActivity,
                onActivitySelected = { selectedActivity = it }
            )

            Spacer(modifier = Modifier.height(28.dp))

            EnhancedWeeklyStepsChart(
                weeklyData = weeklySteps,
                currentDaySteps = dailySteps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            selectedActivity?.let { activity ->
                EnhancedStartActivityButton(
                    activity = activity,
                    onClick = {
                        when (activity) {
                            "walking" -> navController.navigate("walking_screen")
                            "running" -> navController.navigate("run_tracking")
                        }
                    }
                )
            }

            if (selectedActivity == null) {
                EnhancedPromptCard()
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EnhancedActivityHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ActivityMenuColors.AccentPurple.copy(alpha = 0.1f),
                            ActivityMenuColors.SoftCyan.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏃‍♂️ Actividad Física",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ActivityMenuColors.AccentPurple
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mantén tu cuerpo en movimiento",
                    fontSize = 14.sp,
                    color = ActivityMenuColors.AccentPurple.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun EnhancedDailyStepsCircle(
    steps: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f

    // Animación mejorada del progreso
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 1500,
            easing = EaseInOutCubic
        ), label = ""
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 24.dp.toPx()

                drawCircle(
                    color = Color(0xFFE0E0E0),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 20.dp.toPx())
                )

                if (animatedProgress > 0) {
                    val sweepAngle = 360f * animatedProgress

                    val colors = ActivityMenuColors.ProgressGradient
                    val segmentAngle = sweepAngle / colors.size

                    colors.forEachIndexed { index, color ->
                        val startAngle = -90f + (index * segmentAngle)
                        val currentSegmentAngle = if (index == colors.size - 1) {
                            sweepAngle - (index * segmentAngle)
                        } else {
                            segmentAngle
                        }

                        if (currentSegmentAngle > 0) {
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = currentSegmentAngle,
                                useCenter = false,
                                style = Stroke(width = 20.dp.toPx()),
                                topLeft = Offset(
                                    center.x - radius,
                                    center.y - radius
                                ),
                                size = Size(radius * 2, radius * 2)
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👣",
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format("%,d", steps),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = ActivityMenuColors.PrimaryPink
                )
                Text(
                    text = "Pasos de Hoy",
                    fontSize = 10.sp,
                    color = ActivityMenuColors.AccentPurple,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EnhancedProgressInfo(
    steps: Int,
    goal: Int
) {
    val progress = if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).toInt()
    val remaining = (goal - steps).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressInfoItem(
                    label = "Meta",
                    value = String.format("%,d", goal),
                    icon = "🎯",
                    color = ActivityMenuColors.SecondaryBlue
                )
                ProgressInfoItem(
                    label = "Progreso",
                    value = "$percentage%",
                    icon = "📊",
                    color = ActivityMenuColors.WarmOrange
                )
                ProgressInfoItem(
                    label = "Restantes",
                    value = String.format("%,d", remaining),
                    icon = "⏳",
                    color = ActivityMenuColors.AccentPurple
                )
            }

            if (steps > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when {
                        percentage >= 100 -> "🎉 ¡Meta alcanzada! ¡Excelente trabajo!"
                        percentage >= 75 -> "💪 ¡Casi ahí! Solo un poco más"
                        percentage >= 50 -> "🚀 ¡Vas por buen camino!"
                        percentage >= 25 -> "👍 ¡Buen comienzo! Sigue así"
                        else -> "🌟 ¡Cada paso cuenta! ¡Empecemos!"
                    },
                    fontSize = 14.sp,
                    color = when {
                        percentage >= 100 -> ActivityMenuColors.SuccessGreen
                        percentage >= 50 -> ActivityMenuColors.WarmOrange
                        else -> ActivityMenuColors.AccentPurple
                    },
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProgressInfoItem(
    label: String,
    value: String,
    icon: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun EnhancedActivitySelection(
    selectedActivity: String?,
    onActivitySelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Selecciona tu Actividad",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ActivityMenuColors.AccentPurple,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedActivityButton(
                    text = "🚶‍♂️ Caminata",
                    activityId = "walking",
                    backgroundColor = ActivityMenuColors.SuccessGreen,
                    isSelected = selectedActivity == "walking",
                    modifier = Modifier.weight(1f),
                    onClick = { onActivitySelected("walking") }
                )

                EnhancedActivityButton(
                    text = "🏃‍♂️ Carrera",
                    activityId = "running",
                    backgroundColor = ActivityMenuColors.SecondaryBlue,
                    isSelected = selectedActivity == "running",
                    modifier = Modifier.weight(1f),
                    onClick = { onActivitySelected("running") }
                )
            }
        }
    }
}

@Composable
private fun EnhancedActivityButton(
    text: String,
    activityId: String,
    backgroundColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = if (isSelected) backgroundColor else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp,
            pressedElevation = 12.dp
        ),
        border = if (!isSelected) BorderStroke(2.dp, backgroundColor.copy(alpha = 0.3f)) else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else backgroundColor,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun EnhancedWeeklyStepsChart(
    weeklyData: List<Int>,
    currentDaySteps: Int,
    modifier: Modifier = Modifier
) {
    val days = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do")

    val realWeeklyData = remember(weeklyData, currentDaySteps) {
        val data = weeklyData.toMutableList()

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

        data[currentDay] = currentDaySteps

        println("📊 Gráfico DINÁMICO - Día actual: $currentDay, Pasos hoy: $currentDaySteps")
        println("📊 Array DINÁMICO: $data")

        data
    }

    val maxSteps = realWeeklyData.filter { it > 0 }.maxOrNull() ?: 1000

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "📊 Pasos de la Semana",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ActivityMenuColors.AccentPurple
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Contenedor para las barras
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(7) { index ->
                    val steps = realWeeklyData.getOrNull(index) ?: 0
                    val barHeight = if (steps > 0 && maxSteps > 0) {
                        (steps.toFloat() / maxSteps * 120).dp.coerceAtLeast(12.dp)
                    } else {
                        12.dp
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
                        if (steps > 0) {
                            Text(
                                text = if (steps >= 1000) "${steps / 1000}k" else steps.toString(),
                                fontSize = 11.sp,
                                color = if (isToday) ActivityMenuColors.PrimaryPink else ActivityMenuColors.AccentPurple,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        } else {
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(barHeight)
                                .background(
                                    brush = if (steps > 0) {
                                        if (isToday) {
                                            Brush.verticalGradient(
                                                listOf(ActivityMenuColors.PrimaryPink, ActivityMenuColors.WarmOrange)
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(ActivityMenuColors.SecondaryBlue, ActivityMenuColors.SoftCyan)
                                            )
                                        }
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5))
                                        )
                                    },
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = days[index],
                            fontSize = 13.sp,
                            color = if (isToday) ActivityMenuColors.PrimaryPink else Color.Gray,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    fontSize = 13.sp,
                    color = ActivityMenuColors.AccentPurple,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Promedio: ${String.format("%,d", averageSteps)}",
                    fontSize = 13.sp,
                    color = ActivityMenuColors.AccentPurple,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Días activos: $daysWithRealData",
                    fontSize = 13.sp,
                    color = ActivityMenuColors.AccentPurple,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EnhancedStartActivityButton(
    activity: String,
    onClick: () -> Unit
) {
    val (icon, text, gradient) = when (activity) {
        "walking" -> Triple(
            "🚶‍♂️",
            "Iniciar Caminata",
            listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
        )
        "running" -> Triple(
            "🏃‍♂️",
            "Iniciar Carrera",
            listOf(Color(0xFF2196F3), Color(0xFF1565C0))
        )
        else -> Triple(
            "🎯",
            "Iniciar Actividad",
            listOf(Color(0xFFE91E63), Color(0xFFAD1457))
        )
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(gradient),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Toca para comenzar",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedPromptCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ActivityMenuColors.SoftCyan.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ActivityMenuColors.SoftCyan.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Selecciona una actividad",
                color = ActivityMenuColors.SoftCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Elige caminata o carrera para comenzar tu actividad física",
                color = ActivityMenuColors.SoftCyan.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}