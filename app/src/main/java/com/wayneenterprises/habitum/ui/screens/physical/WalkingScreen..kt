package com.wayneenterprises.habitum.ui.screens.physical

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wayneenterprises.habitum.sensors.AccelerometerSensorManager
import com.wayneenterprises.habitum.sensors.StepDetector
import com.wayneenterprises.habitum.sensors.StepListener
import com.wayneenterprises.habitum.viewmodel.WalkingViewModel
import kotlinx.coroutines.delay

@Composable
fun WalkingScreen(
    viewModel: WalkingViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val stepDetector = remember { StepDetector() }
    val accelerometerManager = remember {
        AccelerometerSensorManager(context, stepDetector)
    }

    LaunchedEffect(Unit) {
        stepDetector.registerListener(object : StepListener {
            override fun step(timeNs: Long) {
                viewModel.incrementStep()
            }
        })
    }

    LaunchedEffect(uiState.isRunning) {
        if (uiState.isRunning) {
            accelerometerManager.start()
            while (uiState.isRunning) {
                viewModel.updateTime()
                delay(1000)
            }
        } else {
            accelerometerManager.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            accelerometerManager.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        StepCard(
            steps = uiState.steps,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TimeCard(
            time = uiState.formattedTime,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        DistanceCard(
            distance = uiState.distanceKm,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botones horizontales como en la segunda imagen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Botón Pausar/Reanudar (izquierda)
            Button(
                onClick = { viewModel.pauseResume() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0E0E0) // Gris claro como en la imagen
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = if (uiState.isRunning) "Pausar" else "Reanudar",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Botón Detener (derecha)
            Button(
                onClick = { viewModel.stop() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252) // Rojo como en la imagen
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Detener",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StepCard(
    steps: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "👣 Pasos",
                color = Color(0xFFFF69B4),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("%,d", steps),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "pasos",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun TimeCard(
    time: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⏱ Tiempo",
                color = Color(0xFFFF69B4),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = time,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun DistanceCard(
    distance: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📏 Distancia",
                color = Color(0xFFFF69B4),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("%.2f", distance),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "km",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}