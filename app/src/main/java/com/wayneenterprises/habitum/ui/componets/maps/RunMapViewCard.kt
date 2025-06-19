package com.wayneenterprises.habitum.ui.componets.maps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wayneenterprises.habitum.viewmodel.RunMapViewModel
import com.wayneenterprises.habitum.ui.screens.physical.RunState
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun RunMapViewCard(
    viewModel: RunMapViewModel,
    mapView: MutableState<MapView?>,
    startMarker: MutableState<Marker?>,
    endMarker: MutableState<Marker?>,
    routePolyline: Polyline,
    plannedRoutePolyline: Polyline,
    onMapClick: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val runState by remember { viewModel.runState }
    val isNavigating by remember { viewModel.isNavigating }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)

                    // MEJORA PARA FLUIDEZ DEL MAPA
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                    viewModel.currentLocation.value?.let(controller::setCenter)
                    mapView.value = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.overlays.clear()

                // Agregar eventos de mapa solo si estamos en modo setup
                if (runState == RunState.SETUP) {
                    view.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            onMapClick(p)
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    }))
                }

                // Mostrar ruta planificada (línea punteada azul) cuando esté lista para correr
                if (runState == RunState.READY || runState == RunState.RUNNING || runState == RunState.FINISHED) {
                    if (viewModel.plannedRoute.isNotEmpty()) {
                        plannedRoutePolyline.setPoints(viewModel.plannedRoute)

                        // Configurar estilo de la ruta planificada
                        plannedRoutePolyline.outlinePaint.apply {
                            color = android.graphics.Color.parseColor("#4285F4") // Azul Google
                            strokeWidth = 8f
                            pathEffect = android.graphics.DashPathEffect(floatArrayOf(20f, 10f), 0f) // Línea punteada
                            alpha = 150 // Semi-transparente
                        }

                        view.overlays.add(plannedRoutePolyline)
                    }
                }

                // Mostrar ruta recorrida (línea sólida verde) durante la carrera
                if (runState == RunState.RUNNING || runState == RunState.FINISHED) {
                    if (viewModel.routePoints.isNotEmpty()) {
                        routePolyline.setPoints(viewModel.routePoints)

                        // Configurar estilo de la ruta recorrida
                        routePolyline.outlinePaint.apply {
                            color = android.graphics.Color.parseColor("#34A853") // Verde Google
                            strokeWidth = 12f
                            alpha = 255 // Completamente opaco
                        }

                        view.overlays.add(routePolyline)
                    }
                }

                // Agregar marcadores de inicio y fin
                startMarker.value?.let { marker ->
                    view.overlays.add(marker)
                }

                endMarker.value?.let { marker ->
                    view.overlays.add(marker)
                }

                // Agregar marcador de ubicación actual durante la carrera
                if (runState == RunState.RUNNING) {
                    viewModel.currentLocation.value?.let { currentPos ->
                        val currentMarker = Marker(view).apply {
                            position = currentPos
                            title = "Tu ubicación"
                            icon = createCurrentLocationMarker(context)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                        view.overlays.add(currentMarker)
                    }
                }

                // Centrar el mapa según el estado (comportamiento original)
                when (runState) {
                    RunState.SETUP, RunState.READY -> {
                        // Mostrar ambos puntos si están configurados
                        if (viewModel.startPoint.value != null && viewModel.endPoint.value != null) {
                            val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(
                                listOf(viewModel.startPoint.value!!, viewModel.endPoint.value!!)
                            )
                            view.zoomToBoundingBox(bounds, true, 100)
                        } else {
                            viewModel.currentLocation.value?.let(view.controller::setCenter)
                        }
                    }

                    RunState.RUNNING -> {
                        // Seguir la ubicación actual durante la carrera (modo navegación)
                        if (isNavigating) {
                            viewModel.currentLocation.value?.let { current ->
                                view.controller.setCenter(current)
                                view.controller.setZoom(18.0) // Zoom más cercano para navegación
                            }
                        }
                    }

                    RunState.FINISHED -> {
                        // Mostrar toda la ruta completada
                        if (viewModel.routePoints.isNotEmpty()) {
                            val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(viewModel.routePoints)
                            view.zoomToBoundingBox(bounds, true, 100)
                        }
                    }
                }

                view.invalidate()
            }
        )
    }
}

// Función para crear marcador de ubicación actual
private fun createCurrentLocationMarker(context: android.content.Context): android.graphics.drawable.Drawable {
    val size = 40
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }

    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Círculo exterior azul
    paint.color = android.graphics.Color.parseColor("#4285F4")
    paint.alpha = 100
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    // Círculo interior blanco
    paint.color = android.graphics.Color.WHITE
    paint.alpha = 255
    canvas.drawCircle(size / 2f, size / 2f, size / 4f, paint)

    // Punto central azul
    paint.color = android.graphics.Color.parseColor("#4285F4")
    canvas.drawCircle(size / 2f, size / 2f, size / 8f, paint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

// Función para crear marcadores personalizados mejorados
fun createCustomMarker(
    context: android.content.Context,
    color: Int,
    text: String,
    isLarge: Boolean = false
): android.graphics.drawable.Drawable {
    val size = if (isLarge) 80 else 60
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val bitmap = android.graphics.Bitmap.createBitmap(size, size + 20, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Sombra del marcador
    paint.color = android.graphics.Color.BLACK
    paint.alpha = 50
    canvas.drawCircle(size / 2f + 2, size / 2f + 2, size / 2f - 4, paint)

    // Círculo principal del marcador
    paint.color = color
    paint.alpha = 255
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

    // Borde blanco
    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 4f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

    // Texto del marcador
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    paint.textSize = if (isLarge) 24f else 20f
    canvas.drawText(text, size / 2f, size / 2f + paint.textSize / 3, paint)

    // Punta del marcador (triángulo)
    paint.color = color
    val trianglePath = android.graphics.Path().apply {
        moveTo(size / 2f, size.toFloat())
        lineTo(size / 2f - 8, size.toFloat() - 12)
        lineTo(size / 2f + 8, size.toFloat() - 12)
        close()
    }
    canvas.drawPath(trianglePath, paint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}