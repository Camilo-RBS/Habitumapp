package com.wayneenterprises.habitum.ui.screens.physical

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    // 🔥 NUEVA VARIABLE: Para controlar si ya centramos el mapa una vez
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

    // 🔥 RESETEAR el flag cuando se reinicia la carrera
    LaunchedEffect(runState) {
        if (runState == RunState.SETUP) {
            hasInitiallycenteredMap.value = false
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when (runState) {
                            RunState.SETUP -> "📍 Selecciona tu destino"
                            RunState.READY -> "✅ ¡Listo para correr!"
                            RunState.RUNNING -> "🏃‍♂️ Corriendo..."
                            RunState.FINISHED -> "🎉 ¡Completado!"
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (runState == RunState.SETUP && currentLocation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🎯 Tu carrera comenzará desde tu ubicación actual", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Toca en el mapa para marcar tu destino", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }

            if (isLoadingRoute) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🗺️ Calculando ruta por calles...")
                    }
                }
            }

            routeLoadError?.let {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text("⚠️ $it. Usando ruta alternativa.", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

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

            Card(
                modifier = Modifier.fillMaxWidth().height(450.dp).padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        view.overlays.clear()

                        if (runState == RunState.SETUP) {
                            view.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                    handleDestinationClick(context, p, viewModel, mapView.value, endMarker)
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint): Boolean = false
                            }))
                        }

                        if (runState != RunState.SETUP && viewModel.plannedRoute.isNotEmpty()) {
                            plannedRoutePolyline.setPoints(viewModel.plannedRoute)
                            plannedRoutePolyline.outlinePaint.color = android.graphics.Color.parseColor("#2196F3")
                            plannedRoutePolyline.outlinePaint.strokeWidth = 10f
                            view.overlays.add(plannedRoutePolyline)
                        }

                        if ((runState == RunState.RUNNING || runState == RunState.FINISHED) && viewModel.routePoints.isNotEmpty()) {
                            routePolyline.setPoints(viewModel.routePoints)
                            routePolyline.outlinePaint.color = if (runState == RunState.FINISHED) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#FF5722")
                            routePolyline.outlinePaint.strokeWidth = 12f
                            view.overlays.add(routePolyline)
                        }

                        startMarker.value?.let { view.overlays.add(it) }
                        endMarker.value?.let { view.overlays.add(it) }

                        // 🔥 CAMBIO IMPORTANTE: Solo mostrar marcador de ubicación actual durante RUNNING, NO en FINISHED
                        if (runState == RunState.RUNNING && currentLocation != null) {
                            val currentMarker = Marker(view).apply {
                                position = currentLocation!!
                                title = "Tu ubicación"
                                icon = createCurrentLocationMarker(context)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            }
                            view.overlays.add(currentMarker)
                        }

                        when (runState) {
                            RunState.SETUP -> {
                                // 🔥 CENTRAR SOLO UNA VEZ cuando llegue la ubicación por primera vez
                                if (!hasInitiallycenteredMap.value && currentLocation != null) {
                                    currentLocation?.let { location ->
                                        view.controller.setCenter(location)
                                        view.controller.setZoom(15.0)
                                        hasInitiallycenteredMap.value = true
                                        println("🗺️ Mapa centrado por primera vez en ubicación actual")
                                    }
                                }
                                // NO hacer nada más - el usuario puede mover el mapa libremente
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
                                // 🔥 MOSTRAR SOLO LA RUTA COMPLETADA sin marcador de ubicación actual
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
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

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

// 🔥 MARCADOR DE INICIO MEJORADO - Diseño más atractivo
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
            icon = createStartMarker(context)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        markerState.value = marker
    }
}

// 🔥 MARCADOR DE DESTINO MEJORADO - Diseño más atractivo
fun handleDestinationClick(
    context: Context,
    location: GeoPoint,
    viewModel: RunMapViewModel,
    mapView: MapView?,
    markerState: MutableState<Marker?>
) {
    println("🎯 Destino seleccionado: ${location.latitude}, ${location.longitude}")

    // ✅ ASEGURAR que tenemos punto de inicio antes de continuar
    if (viewModel.startPoint.value == null) {
        viewModel.setStartPointAutomatically()
        println("📍 Punto de inicio configurado automáticamente")
    }

    viewModel.endPoint.value = location
    viewModel.isSelectingDestination.value = false

    mapView?.let { map ->
        val marker = Marker(map).apply {
            position = location
            title = "🏁 Meta / Destino"
            icon = createFinishMarker(context)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        markerState.value = marker
    }

    // ✅ CALCULAR RUTA solo si tenemos ambos puntos
    if (viewModel.startPoint.value != null && viewModel.endPoint.value != null) {
        println("🗺️ Calculando ruta desde inicio hasta destino...")
        viewModel.calculateRealRouteByStreets()
    } else {
        println("⚠️ Error: Faltan puntos para calcular ruta")
    }
}

// 🔥 NUEVO: Crear marcador de inicio personalizado con diseño atractivo
fun createStartMarker(context: Context): android.graphics.drawable.Drawable {
    val size = 70
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val bitmap = android.graphics.Bitmap.createBitmap(size, size + 25, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Sombra del marcador
    paint.color = android.graphics.Color.BLACK
    paint.alpha = 60
    canvas.drawCircle(size / 2f + 3, size / 2f + 3, size / 2f - 8, paint)

    // Círculo principal verde brillante
    paint.color = android.graphics.Color.parseColor("#4CAF50") // Verde vibrante
    paint.alpha = 255
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8, paint)

    // Borde blanco grueso
    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 6f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8, paint)

    // Ícono de "Play" / Inicio
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    val playPath = android.graphics.Path().apply {
        moveTo(size / 2f - 8, size / 2f - 12)
        lineTo(size / 2f + 12, size / 2f)
        lineTo(size / 2f - 8, size / 2f + 12)
        close()
    }
    canvas.drawPath(playPath, paint)

    // Punta del marcador (triángulo verde)
    paint.color = android.graphics.Color.parseColor("#4CAF50")
    val trianglePath = android.graphics.Path().apply {
        moveTo(size / 2f, size.toFloat())
        lineTo(size / 2f - 12, size.toFloat() - 18)
        lineTo(size / 2f + 12, size.toFloat() - 18)
        close()
    }
    canvas.drawPath(trianglePath, paint)

    // Texto "START" debajo
    paint.color = android.graphics.Color.parseColor("#2E7D32")
    paint.textSize = 12f
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawText("START", size / 2f, size + 20f, paint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

// 🔥 NUEVO: Crear marcador de meta personalizado con diseño atractivo
fun createFinishMarker(context: Context): android.graphics.drawable.Drawable {
    val size = 70
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val bitmap = android.graphics.Bitmap.createBitmap(size, size + 25, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Sombra del marcador
    paint.color = android.graphics.Color.BLACK
    paint.alpha = 60
    canvas.drawCircle(size / 2f + 3, size / 2f + 3, size / 2f - 8, paint)

    // Círculo principal rojo brillante
    paint.color = android.graphics.Color.parseColor("#F44336") // Rojo vibrante
    paint.alpha = 255
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8, paint)

    // Borde blanco grueso
    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 6f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8, paint)

    // Bandera a cuadros (efecto finish line)
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

    // Punta del marcador (triángulo rojo)
    paint.color = android.graphics.Color.parseColor("#F44336")
    val trianglePath = android.graphics.Path().apply {
        moveTo(size / 2f, size.toFloat())
        lineTo(size / 2f - 12, size.toFloat() - 18)
        lineTo(size / 2f + 12, size.toFloat() - 18)
        close()
    }
    canvas.drawPath(trianglePath, paint)

    // Texto "FINISH" debajo
    paint.color = android.graphics.Color.parseColor("#C62828")
    paint.textSize = 12f
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawText("FINISH", size / 2f, size + 20f, paint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

// Crea un marcador para la ubicación actual (círculo azul con blanco) - SIN CAMBIOS
fun createCurrentLocationMarker(context: Context): android.graphics.drawable.Drawable {
    val size = 40
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }

    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    paint.color = android.graphics.Color.parseColor("#4285F4")
    paint.alpha = 100
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.alpha = 255
    canvas.drawCircle(size / 2f, size / 2f, size / 4f, paint)

    paint.color = android.graphics.Color.parseColor("#4285F4")
    canvas.drawCircle(size / 2f, size / 2f, size / 8f, paint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}