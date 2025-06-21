package com.wayneenterprises.habitum.ui.screens.physical

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wayneenterprises.habitum.sensors.AccelerometerSensorManager
import com.wayneenterprises.habitum.sensors.StepDetector
import com.wayneenterprises.habitum.sensors.StepListener
import com.wayneenterprises.habitum.ui.componets.maps.RunControlButtons
import com.wayneenterprises.habitum.ui.componets.maps.RunStatusCard
import com.wayneenterprises.habitum.viewmodel.RunMapViewModel
import com.google.accompanist.permissions.*
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// 🎨 Paleta de colores mejorada
object RunMapColors {
    val PrimaryPink = Color(0xFFE91E63)
    val SecondaryBlue = Color(0xFF2196F3)
    val AccentPurple = Color(0xFF9C27B0)
    val SoftCyan = Color(0xFF00BCD4)
    val WarmOrange = Color(0xFFFF9800)
    val SuccessGreen = Color(0xFF4CAF50)

    // Gradientes
    val PrimaryGradient = listOf(Color(0xFFE91E63), Color(0xFFAD1457))
    val SecondaryGradient = listOf(Color(0xFF2196F3), Color(0xFF1565C0))
    val SuccessGradient = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
    val SetupGradient = listOf(Color(0xFF9C27B0), Color(0xFF6A1B9A))
    val BackgroundGradient = listOf(Color(0xFFF8F9FA), Color(0xFFE3F2FD))
}

enum class RunState { SETUP, READY, RUNNING, FINISHED }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RunMapScreen(viewModel: RunMapViewModel = viewModel()) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val mapView = remember { mutableStateOf<MapView?>(null) }
    val routePolyline = remember { Polyline() }
    val plannedRoutePolyline = remember { Polyline() }
    val startMarker = remember { mutableStateOf<Marker?>(null) }
    val endMarker = remember { mutableStateOf<Marker?>(null) }

    val hasInitiallycenteredMap = remember { mutableStateOf(false) }

    val runState by viewModel.runState
    val isLoadingRoute by viewModel.isLoadingRoute
    val routeLoadError by viewModel.routeLoadError
    val currentLocation by viewModel.currentLocation

    val stepDetector = remember { StepDetector() }
    val accelerometerManager = remember { AccelerometerSensorManager(context, stepDetector) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    DisposableEffect(Unit) {
        stepDetector.registerListener(object : StepListener {
            override fun step(timeNs: Long) {
                viewModel.incrementStep()
            }
        })
        onDispose {
            accelerometerManager.stop()
        }
    }

    LaunchedEffect(runState) {
        if (runState == RunState.RUNNING) {
            accelerometerManager.start()
        } else {
            accelerometerManager.stop()
        }
    }

    @SuppressLint("MissingPermission")
    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val geoPoint = GeoPoint(it.latitude, it.longitude)
                    viewModel.updateLocation(geoPoint)
                    if (runState == RunState.SETUP) {
                        showStartMarker(context, geoPoint, mapView.value, startMarker)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    LaunchedEffect(locationPermissionState.status, runState) {
        if (locationPermissionState.status.isGranted) {
            startLocationUpdates(context, fusedLocationClient) { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                when (runState) {
                    RunState.SETUP -> {
                        if (viewModel.currentLocation.value == null) {
                            viewModel.updateLocation(geoPoint)
                        }
                    }
                    RunState.READY, RunState.RUNNING -> {
                        viewModel.updateLocation(geoPoint)
                    }
                    else -> {}
                }

                if (runState == RunState.SETUP && startMarker.value == null) {
                    showStartMarker(context, geoPoint, mapView.value, startMarker)
                }
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(viewModel.startTime) {
        while (viewModel.startTime != null && runState == RunState.RUNNING) {
            viewModel.updateElapsedTime()
            kotlinx.coroutines.delay(1000)
        }
    }

    LaunchedEffect(runState) {
        if (runState == RunState.SETUP) {
            hasInitiallycenteredMap.value = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(RunMapColors.BackgroundGradient)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ModernRunHeader(runState = runState)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedStatusMessage(
                    runState = runState,
                    isSelectingDestination = viewModel.isSelectingDestination.value,
                    isLoadingRoute = isLoadingRoute,
                    routeLoadError = routeLoadError,
                    currentLocation = currentLocation
                )

                RunStatusCard(
                    runState = runState,
                    isSelectingStart = false,
                    elapsedTime = viewModel.elapsedTime.value,
                    totalDistance = viewModel.totalDistance.value,
                    stepCount = viewModel.stepCount.value
                )

                RunControlButtons(
                    viewModel = viewModel,
                    mapView = mapView.value,
                    polyline = routePolyline
                )

                EnhancedMapContainer(
                    viewModel = viewModel,
                    mapView = mapView,
                    routePolyline = routePolyline,
                    plannedRoutePolyline = plannedRoutePolyline,
                    startMarker = startMarker,
                    endMarker = endMarker,
                    hasInitiallycenteredMap = hasInitiallycenteredMap,
                    runState = runState,
                    currentLocation = currentLocation,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun ModernRunHeader(runState: RunState) {
    val statusText = when (runState) {
        RunState.SETUP -> "🎯 Configura tu Ruta"
        RunState.READY -> "✅ ¡Todo Listo!"
        RunState.RUNNING -> "🏃‍♂️ En Marcha"
        RunState.FINISHED -> "🎉 ¡Completado!"
    }

    val gradient = when (runState) {
        RunState.SETUP -> RunMapColors.SetupGradient
        RunState.READY -> RunMapColors.SecondaryGradient
        RunState.RUNNING -> RunMapColors.PrimaryGradient
        RunState.FINISHED -> RunMapColors.SuccessGradient
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
                .padding(24.dp)
        ) {
            Text(
                text = statusText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun EnhancedStatusMessage(
    runState: RunState,
    isSelectingDestination: Boolean,
    isLoadingRoute: Boolean,
    routeLoadError: String?,
    currentLocation: GeoPoint?
) {
    when {
        runState == RunState.SETUP && currentLocation != null -> {
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
                                listOf(RunMapColors.AccentPurple.copy(alpha = 0.1f), RunMapColors.SoftCyan.copy(alpha = 0.1f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "🎯 Tu Punto de Partida",
                            color = RunMapColors.AccentPurple,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ubicación actual confirmada. Toca en el mapa para seleccionar tu destino.",
                            color = RunMapColors.AccentPurple.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        isLoadingRoute -> {
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
                                listOf(RunMapColors.SecondaryBlue.copy(alpha = 0.1f), RunMapColors.SoftCyan.copy(alpha = 0.1f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = RunMapColors.SecondaryBlue,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🗺️ Calculando Ruta",
                                color = RunMapColors.SecondaryBlue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Buscando el mejor camino por las calles...",
                                color = RunMapColors.SecondaryBlue.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        routeLoadError != null -> {
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
                                listOf(RunMapColors.WarmOrange.copy(alpha = 0.1f), RunMapColors.PrimaryPink.copy(alpha = 0.1f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "⚠️ Ruta Alternativa",
                            color = RunMapColors.WarmOrange,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Usando ruta directa. La funcionalidad completa está disponible.",
                            color = RunMapColors.WarmOrange.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedMapContainer(
    viewModel: RunMapViewModel,
    mapView: MutableState<MapView?>,
    routePolyline: Polyline,
    plannedRoutePolyline: Polyline,
    startMarker: MutableState<Marker?>,
    endMarker: MutableState<Marker?>,
    hasInitiallycenteredMap: MutableState<Boolean>,
    runState: RunState,
    currentLocation: GeoPoint?,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        controller.setCenter(currentLocation ?: GeoPoint(13.7, -89.2))
                        mapView.value = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    updateMapWithNewColors(
                        view, viewModel, runState, hasInitiallycenteredMap,
                        currentLocation, context, startMarker, endMarker,
                        plannedRoutePolyline, routePolyline
                    )
                }
            )

            DecorativeMapOverlay()
        }
    }
}

@Composable
private fun DecorativeMapOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Esquina superior izquierda
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    Brush.radialGradient(
                        listOf(RunMapColors.PrimaryPink.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(topStart = 20.dp)
                )
        )

        // Esquina inferior derecha
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    Brush.radialGradient(
                        listOf(RunMapColors.SecondaryBlue.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(bottomEnd = 20.dp)
                )
                .align(Alignment.BottomEnd)
        )
    }
}

private fun updateMapWithNewColors(
    view: MapView,
    viewModel: RunMapViewModel,
    runState: RunState,
    hasInitiallycenteredMap: MutableState<Boolean>,
    currentLocation: GeoPoint?,
    context: Context,
    startMarker: MutableState<Marker?>,
    endMarker: MutableState<Marker?>,
    plannedRoutePolyline: Polyline,
    routePolyline: Polyline
) {
    view.overlays.clear()

    // Agregar eventos de mapa solo si estamos en modo setup
    if (runState == RunState.SETUP) {
        view.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                handleDestinationClick(context, p, viewModel, view, endMarker)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }))
    }

    if (runState != RunState.SETUP && viewModel.plannedRoute.isNotEmpty()) {
        plannedRoutePolyline.setPoints(viewModel.plannedRoute)
        plannedRoutePolyline.outlinePaint.apply {
            color = android.graphics.Color.parseColor("#2196F3") // Azul vibrante
            strokeWidth = 12f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(25f, 15f), 0f)
            alpha = 180
        }
        view.overlays.add(plannedRoutePolyline)
    }

    if ((runState == RunState.RUNNING || runState == RunState.FINISHED) && viewModel.routePoints.isNotEmpty()) {
        routePolyline.setPoints(viewModel.routePoints)
        routePolyline.outlinePaint.apply {
            color = if (runState == RunState.FINISHED)
                android.graphics.Color.parseColor("#4CAF50") // Verde éxito
            else
                android.graphics.Color.parseColor("#E91E63") // Rosa vibrante
            strokeWidth = 14f
            alpha = 255
        }
        view.overlays.add(routePolyline)
    }

    startMarker.value?.let { view.overlays.add(it) }
    endMarker.value?.let { view.overlays.add(it) }

    if (runState == RunState.RUNNING && currentLocation != null) {
        val currentMarker = Marker(view).apply {
            position = currentLocation
            title = "Tu ubicación"
            icon = createEnhancedCurrentLocationMarker(context)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        view.overlays.add(currentMarker)
    }

    // Lógica de centrado del mapa (sin cambios)
    when (runState) {
        RunState.SETUP -> {
            if (!hasInitiallycenteredMap.value && currentLocation != null) {
                currentLocation.let { location ->
                    view.controller.setCenter(location)
                    view.controller.setZoom(15.0)
                    hasInitiallycenteredMap.value = true
                }
            }
        }
        RunState.READY -> {
            if (viewModel.startPoint.value != null && viewModel.endPoint.value != null) {
                val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(listOf(viewModel.startPoint.value!!, viewModel.endPoint.value!!))
                view.zoomToBoundingBox(bounds, true, 100)
            }
        }
        RunState.RUNNING -> {
            currentLocation?.let {
                view.controller.setCenter(it)
                view.controller.setZoom(17.0)
            }
        }
        RunState.FINISHED -> {
            if (viewModel.routePoints.isNotEmpty()) {
                val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(viewModel.routePoints)
                view.zoomToBoundingBox(bounds, true, 100)
                if (view.zoomLevelDouble < 14.0) {
                    view.controller.setZoom(14.0)
                }
            }
        }
    }

    view.invalidate()
}

// Resto de funciones auxiliares
@SuppressLint("MissingPermission")
private fun startLocationUpdates(
    context: Context,
    client: FusedLocationProviderClient,
    onUpdate: (Location) -> Unit
) {
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
        .setMinUpdateIntervalMillis(1000L)
        .build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(onUpdate)
        }
    }

    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
}

fun showStartMarker(
    context: Context,
    location: GeoPoint,
    mapView: MapView?,
    markerState: MutableState<Marker?>
) {
    mapView?.let { map ->
        val marker = Marker(map).apply {
            position = location
            title = "🚀 Punto de Inicio"
            icon = createEnhancedStartMarker(context)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        markerState.value = marker
    }
}

fun handleDestinationClick(
    context: Context,
    location: GeoPoint,
    viewModel: RunMapViewModel,
    mapView: MapView?,
    markerState: MutableState<Marker?>
) {
    if (viewModel.startPoint.value == null) {
        viewModel.setStartPointAutomatically()
    }

    viewModel.endPoint.value = location
    viewModel.isSelectingDestination.value = false

    mapView?.let { map ->
        val marker = Marker(map).apply {
            position = location
            title = "🏁 Meta / Destino"
            icon = createEnhancedFinishMarker(context)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        markerState.value = marker
    }

    if (viewModel.startPoint.value != null && viewModel.endPoint.value != null) {
        viewModel.calculateRealRouteByStreets()
    }
}

fun createEnhancedStartMarker(context: Context): android.graphics.drawable.Drawable {
    val size = 70
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val bitmap = android.graphics.Bitmap.createBitmap(size, size + 25, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    paint.color = android.graphics.Color.BLACK
    paint.alpha = 60
    canvas.drawCircle(size / 2f + 3, size / 2f + 3, size / 2f - 8, paint)

    paint.color = android.graphics.Color.parseColor("#E91E63") // Rosa primario
    paint.alpha = 255
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8, paint)

    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 6f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8, paint)

    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    val playPath = android.graphics.Path().apply {
        moveTo(size / 2f - 8, size / 2f - 12)
        lineTo(size / 2f + 12, size / 2f)
        lineTo(size / 2f - 8, size / 2f + 12)
        close()
    }
    canvas.drawPath(playPath, paint)

    paint.color = android.graphics.Color.parseColor("#E91E63")
    val trianglePath = android.graphics.Path().apply {
        moveTo(size / 2f, size.toFloat())
        lineTo(size / 2f - 12, size.toFloat() - 18)
        lineTo(size / 2f + 12, size.toFloat() - 18)
        close()
    }
    canvas.drawPath(trianglePath, paint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

fun createEnhancedFinishMarker(context: Context): android.graphics.drawable.Drawable {
    val size = 70
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val bitmap = android.graphics.Bitmap.createBitmap(size, size + 25, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    paint.color = android.graphics.Color.BLACK
    paint.alpha = 60
    canvas.drawCircle(size / 2f + 3, size / 2f + 3, size / 2f - 8, paint)

    paint.color = android.graphics.Color.parseColor("#2196F3")
    paint.alpha = 255
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8, paint)

    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 6f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8, paint)

    paint.style = android.graphics.Paint.Style.FILL
    val squareSize = 4f
    for (i in 0..6) {
        for (j in 0..6) {
            paint.color = if ((i + j) % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            canvas.drawRect(
                size / 2f - 14 + i * squareSize,
                size / 2f - 14 + j * squareSize,
                size / 2f - 14 + (i + 1) * squareSize,
                size / 2f - 14 + (j + 1) * squareSize,
                paint
            )
        }
    }

    paint.color = android.graphics.Color.parseColor("#2196F3")
    val trianglePath = android.graphics.Path().apply {
        moveTo(size / 2f, size.toFloat())
        lineTo(size / 2f - 12, size.toFloat() - 18)
        lineTo(size / 2f + 12, size.toFloat() - 18)
        close()
    }
    canvas.drawPath(trianglePath, paint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

fun createEnhancedCurrentLocationMarker(context: Context): android.graphics.drawable.Drawable {
    val size = 50
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }

    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    paint.color = android.graphics.Color.parseColor("#E91E63")
    paint.alpha = 120
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.alpha = 255
    canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)

    paint.color = android.graphics.Color.parseColor("#E91E63")
    canvas.drawCircle(size / 2f, size / 2f, size / 6f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 2f
    canvas.drawCircle(size / 2f, size / 2f, size / 6f, paint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}