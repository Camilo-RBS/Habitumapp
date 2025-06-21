package com.wayneenterprises.habitum.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class RouteApiService {

    companion object {
        private const val OSRM_BASE_URL = "https://router.project-osrm.org/route/v1"
    }

    suspend fun getWalkingRoute(start: GeoPoint, end: GeoPoint): List<GeoPoint> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$OSRM_BASE_URL/foot/" +
                        "${start.longitude},${start.latitude};" +
                        "${end.longitude},${end.latitude}" +
                        "?overview=full&geometries=geojson&steps=true"

                println("🗺️ Solicitando ruta: $url")

                val response = makeHttpRequest(url)
                val route = parseOSRMResponse(response)

                if (route.isNotEmpty()) {
                    println("✅ Ruta obtenida: ${route.size} puntos")
                    route
                } else {
                    println("⚠️ Ruta vacía, usando fallback")
                    createFallbackRoute(start, end)
                }

            } catch (e: Exception) {
                println("❌ Error obteniendo ruta: ${e.message}")
                createFallbackRoute(start, end)
            }
        }
    }

    private fun makeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("User-Agent", "HABITUM-App/1.0")
            }

            val responseCode = connection.responseCode
            println("🌐 Respuesta HTTP: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                response
            } else {
                throw Exception("HTTP Error: $responseCode")
            }

        } finally {
            connection.disconnect()
        }
    }

    private fun parseOSRMResponse(jsonResponse: String): List<GeoPoint> {
        return try {
            val json = JSONObject(jsonResponse)
            val routes = json.getJSONArray("routes")

            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val geometry = route.getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")

                val points = mutableListOf<GeoPoint>()

                for (i in 0 until coordinates.length()) {
                    val coord = coordinates.getJSONArray(i)
                    val longitude = coord.getDouble(0)
                    val latitude = coord.getDouble(1)
                    points.add(GeoPoint(latitude, longitude))
                }

                println("📍 Puntos parseados: ${points.size}")
                points
            } else {
                emptyList()
            }

        } catch (e: Exception) {
            println("❌ Error parseando respuesta: ${e.message}")
            emptyList()
        }
    }

    private fun createFallbackRoute(start: GeoPoint, end: GeoPoint): List<GeoPoint> {
        println("🔄 Usando ruta fallback (línea recta mejorada)")

        val points = mutableListOf<GeoPoint>()
        val steps = 100

        for (i in 0..steps) {
            val ratio = i.toDouble() / steps

            val lat = start.latitude + (end.latitude - start.latitude) * ratio
            val lon = start.longitude + (end.longitude - start.longitude) * ratio

            val variation = 0.0001 * kotlin.math.sin(ratio * kotlin.math.PI * 3)

            points.add(GeoPoint(lat + variation, lon + variation))
        }

        return points
    }

    suspend fun getRouteInfo(start: GeoPoint, end: GeoPoint): RouteInfo {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$OSRM_BASE_URL/foot/" +
                        "${start.longitude},${start.latitude};" +
                        "${end.longitude},${end.latitude}" +
                        "?overview=false"

                val response = makeHttpRequest(url)
                parseRouteInfo(response)

            } catch (e: Exception) {
                // Fallback con cálculo directo
                val distance = calculateDirectDistance(start, end)
                RouteInfo(
                    distance = distance,
                    duration = (distance / 1.4).toInt(), // 1.4 m/s velocidad promedio caminando
                    steps = emptyList()
                )
            }
        }
    }

    private fun parseRouteInfo(jsonResponse: String): RouteInfo {
        return try {
            val json = JSONObject(jsonResponse)
            val routes = json.getJSONArray("routes")

            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val distance = route.getDouble("distance") // en metros
                val duration = route.getDouble("duration") // en segundos

                val steps = mutableListOf<String>()
                if (route.has("legs")) {
                    val legs = route.getJSONArray("legs")
                    for (i in 0 until legs.length()) {
                        val leg = legs.getJSONObject(i)
                        if (leg.has("steps")) {
                            val legSteps = leg.getJSONArray("steps")
                            for (j in 0 until legSteps.length()) {
                                val step = legSteps.getJSONObject(j)
                                if (step.has("maneuver")) {
                                    val maneuver = step.getJSONObject("maneuver")
                                    val instruction = maneuver.optString("instruction", "Continúa")
                                    steps.add(instruction)
                                }
                            }
                        }
                    }
                }

                RouteInfo(
                    distance = distance,
                    duration = duration.toInt(),
                    steps = steps
                )
            } else {
                throw Exception("No routes found")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun calculateDirectDistance(start: GeoPoint, end: GeoPoint): Double {
        val R = 6371000.0
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return R * c
    }
}

data class RouteInfo(
    val distance: Double,  // Distancia en metros
    val duration: Int,     // Duración en segundos
    val steps: List<String> // Instrucciones paso a paso
)