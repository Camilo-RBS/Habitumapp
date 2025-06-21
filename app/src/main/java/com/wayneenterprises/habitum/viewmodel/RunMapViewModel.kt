package com.wayneenterprises.habitum.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayneenterprises.habitum.ui.screens.physical.RunState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import kotlin.math.*

class RunMapViewModel : ViewModel() {

    var runState = mutableStateOf(RunState.SETUP)
    var isSelectingDestination = mutableStateOf(true)

    var currentLocation = mutableStateOf<GeoPoint?>(null)
    var startPoint = mutableStateOf<GeoPoint?>(null)
    var endPoint = mutableStateOf<GeoPoint?>(null)

    var stepCount = mutableStateOf(0)
    var totalDistance = mutableStateOf(0.0)
    var elapsedTime = mutableStateOf(0L)
    var startTime: Long? = null

    var routePoints = mutableStateListOf<GeoPoint>()
    var plannedRoute = mutableStateListOf<GeoPoint>()
    var distanceToDestination = mutableStateOf(0.0)
    var estimatedTimeToArrival = mutableStateOf(0L)
    var isNavigating = mutableStateOf(false)
    var hasArrivedAtDestination = mutableStateOf(false)

    var isLoadingRoute = mutableStateOf(false)
    var routeLoadError = mutableStateOf<String?>(null)

    private var routeCalculationJob: Job? = null

    private val arrivalThreshold = 15.0
    private val averageStepLength = 0.78 // en metros
    private val averageRunningSpeed = 3.0 // m/s

    fun reset() {
        println("🔄 Reseteando ViewModel...")

        routeCalculationJob?.cancel()
        routeCalculationJob = null

        runState.value = RunState.SETUP
        isSelectingDestination.value = true
        startPoint.value = null
        endPoint.value = null
        stepCount.value = 0
        totalDistance.value = 0.0
        elapsedTime.value = 0
        startTime = null
        routePoints.clear()
        plannedRoute.clear()
        distanceToDestination.value = 0.0
        estimatedTimeToArrival.value = 0L
        isNavigating.value = false
        hasArrivedAtDestination.value = false

        isLoadingRoute.value = false
        routeLoadError.value = null

        println("✅ Reset completado")
    }

    fun setStartPointAutomatically() {
        currentLocation.value?.let { location ->
            startPoint.value = location
            println("📍 Punto de inicio configurado: ${location.latitude}, ${location.longitude}")
        }
    }

    fun startRun() {
        if (startPoint.value == null) {
            setStartPointAutomatically()
        }
        runState.value = RunState.RUNNING
        isNavigating.value = true
        stepCount.value = 0
        totalDistance.value = 0.0
        elapsedTime.value = 0
        startTime = System.currentTimeMillis()
        routePoints.clear()
        hasArrivedAtDestination.value = false

        println("🚀 Carrera iniciada")
    }

    fun finishRun() {
        runState.value = RunState.FINISHED
        isNavigating.value = false
        startTime = null
        println("🏁 Carrera terminada")
    }

    fun updateElapsedTime() {
        startTime?.let {
            elapsedTime.value = (System.currentTimeMillis() - it) / 1000
        }
    }

    fun updateLocation(newPoint: GeoPoint) {
        currentLocation.value = newPoint
        if (runState.value == RunState.SETUP && startPoint.value == null) {
            setStartPointAutomatically()
        }
        if (runState.value == RunState.RUNNING) {
            routePoints.add(newPoint)
            updateNavigationData(newPoint)
        }
    }

    fun incrementStep() {
        if (runState.value == RunState.RUNNING) {
            stepCount.value++
            totalDistance.value = stepCount.value * averageStepLength
        }
    }

    fun calculateRealRouteByStreets() {
        val start = startPoint.value
        val end = endPoint.value

        println("🗺️ Iniciando cálculo de ruta...")
        println("📍 Inicio: $start")
        println("🎯 Destino: $end")

        if (start != null && end != null) {
            routeCalculationJob?.cancel()

            routeCalculationJob = viewModelScope.launch {
                isLoadingRoute.value = true
                routeLoadError.value = null

                try {
                    println("🌐 Obteniendo ruta desde API...")
                    val route = fetchRouteFromAPI(start, end)

                    if (route.isNotEmpty()) {
                        println("✅ Ruta obtenida exitosamente: ${route.size} puntos")
                        plannedRoute.clear()
                        plannedRoute.addAll(route)
                        runState.value = RunState.READY
                        println("🎉 Estado cambiado a READY")
                    } else {
                        throw Exception("Ruta vacía desde API")
                    }

                } catch (e: Exception) {
                    println("⚠️ Error en API: ${e.message}")
                    routeLoadError.value = "Error calculando ruta"
                    createFallbackRoute(start, end)
                    runState.value = RunState.READY
                    println("🛡️ Usando ruta de respaldo")
                } finally {
                    isLoadingRoute.value = false
                    println("🏁 Cálculo de ruta terminado")
                }
            }
        } else {
            println("❌ Error: Faltan puntos de inicio o destino")
            routeLoadError.value = "Faltan puntos de inicio o destino"
        }
    }

    private suspend fun fetchRouteFromAPI(start: GeoPoint, end: GeoPoint): List<GeoPoint> {
        return try {
            val url = "https://router.project-osrm.org/route/v1/foot/" +
                    "${start.longitude},${start.latitude};" +
                    "${end.longitude},${end.latitude}" +
                    "?overview=full&geometries=geojson&steps=true"

            println("🌐 URL de solicitud: $url")
            val response = makeHttpRequest(url)
            parseOSRMResponse(response)

        } catch (e: Exception) {
            println("❌ Error en fetchRouteFromAPI: ${e.message}")
            emptyList()
        }
    }

    private suspend fun makeHttpRequest(urlString: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("User-Agent", "HABITUM-App")
            connection.setRequestProperty("Accept", "application/json")

            if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val stream = connection.inputStream
                val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
                val response = reader.readText()
                connection.disconnect()
                response
            } else {
                connection.disconnect()
                throw Exception("HTTP Error: ${connection.responseCode}")
            }
        }
    }

    private fun parseOSRMResponse(json: String): List<GeoPoint> {
        return try {
            val jsonObj = org.json.JSONObject(json)
            val routes = jsonObj.getJSONArray("routes")
            if (routes.length() > 0) {
                val coords = routes.getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates")

                List(coords.length()) { i ->
                    val c = coords.getJSONArray(i)
                    GeoPoint(c.getDouble(1), c.getDouble(0))
                }
            } else emptyList()
        } catch (e: Exception) {
            println("❌ Error parseando respuesta OSRM: ${e.message}")
            emptyList()
        }
    }

    private fun createFallbackRoute(start: GeoPoint, end: GeoPoint) {
        println("🛡️ Creando ruta de respaldo...")
        val steps = 50
        val points = MutableList(steps + 1) { i ->
            val t = i.toDouble() / steps
            val lat = start.latitude + t * (end.latitude - start.latitude)
            val lon = start.longitude + t * (end.longitude - start.longitude)
            GeoPoint(lat, lon)
        }
        plannedRoute.clear()
        plannedRoute.addAll(points)
        println("✅ Ruta de respaldo creada con ${points.size} puntos")
    }

    private fun updateNavigationData(current: GeoPoint) {
        endPoint.value?.let { end ->
            val dist = distanceBetween(current, end)
            distanceToDestination.value = dist
            estimatedTimeToArrival.value = (dist / averageRunningSpeed).toLong()

            if (dist <= arrivalThreshold && !hasArrivedAtDestination.value) {
                hasArrivedAtDestination.value = true
                finishRun()
            }
        }
    }

    private fun distanceBetween(p1: GeoPoint, p2: GeoPoint): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)

        val a = sin(dLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun getCompleteRoute(): List<GeoPoint> {
        val completeRoute = mutableListOf<GeoPoint>()

        startPoint.value?.let { start ->
            completeRoute.add(start)
        }

        if (plannedRoute.isNotEmpty()) {
            completeRoute.addAll(plannedRoute)
        }

        if (routePoints.isNotEmpty()) {
            completeRoute.clear()
            completeRoute.addAll(routePoints)
        }

        endPoint.value?.let { end ->
            if (completeRoute.isEmpty() || distanceBetween(completeRoute.last(), end) > 10) {
                completeRoute.add(end)
            }
        }

        return completeRoute
    }
}