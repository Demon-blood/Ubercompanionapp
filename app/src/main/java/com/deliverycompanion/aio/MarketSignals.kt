package com.deliverycompanion.aio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.util.Locale

data class WeatherSignal(
    val location: String = "Turnhout",
    val temperatureC: Double = 0.0,
    val precipitationMm: Double = 0.0,
    val windKmh: Double = 0.0,
    val demandBoost: Int = 0,
    val scooterRisk: String = "Unknown",
    val summary: String = "No weather loaded yet."
)

data class EventSignal(
    val title: String,
    val location: String,
    val date: String,
    val demandBoost: Int
)

data class MarketSignals(
    val weather: WeatherSignal = WeatherSignal(),
    val events: List<EventSignal> = emptyList(),
    val error: String? = null
)

object MarketSignalService {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadTurnhoutSignals(): MarketSignals = withContext(Dispatchers.IO) {
        runCatching {
            val weather = fetchWeather()
            val events = fetchEvents()
            MarketSignals(weather = weather, events = events)
        }.getOrElse { error ->
            MarketSignals(error = error.message ?: "Could not load market signals.")
        }
    }

    private fun fetchWeather(): WeatherSignal {
        // Turnhout approximate coordinates.
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=51.3225&longitude=4.9447" +
            "&current=temperature_2m,precipitation,wind_speed_10m" +
            "&timezone=Europe%2FBrussels"

        val root = json.parseToJsonElement(httpGet(url)).jsonObject
        val current = root["current"]?.jsonObject ?: JsonObject(emptyMap())

        val temp = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val rain = current["precipitation"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val wind = current["wind_speed_10m"]?.jsonPrimitive?.doubleOrNull ?: 0.0

        val weatherBoost = when {
            rain >= 3.0 -> 18
            rain >= 0.5 -> 12
            wind >= 35.0 -> -12
            wind >= 25.0 -> -6
            temp <= 3.0 -> 8
            temp >= 27.0 -> 6
            else -> 0
        }

        val risk = when {
            wind >= 35.0 -> "High wind risk for e-scooter"
            rain >= 3.0 -> "Wet roads, higher scooter risk"
            rain >= 0.5 -> "Light rain, ride carefully"
            temp <= 2.0 -> "Cold conditions, battery range may drop"
            else -> "Normal"
        }

        return WeatherSignal(
            temperatureC = temp,
            precipitationMm = rain,
            windKmh = wind,
            demandBoost = weatherBoost,
            scooterRisk = risk,
            summary = "${round1(temp)}°C · ${round1(rain)} mm rain · ${round1(wind)} km/h wind"
        )
    }

    private fun fetchEvents(): List<EventSignal> {
        val today = LocalDate.now().toString()
        val nextWeek = LocalDate.now().plusDays(7).toString()

        // UiTdatabank Search API. If the public API changes or rate-limits, the app will fail gracefully.
        val q = URLEncoder.encode("Turnhout", "UTF-8")
        val url = "https://search.uitdatabank.be/events/" +
            "?q=$q&startDateFrom=$today&startDateTo=$nextWeek&limit=5"

        val root = json.parseToJsonElement(httpGet(url)).jsonObject
        val items = findEventArray(root)

        return items.take(5).mapNotNull { element ->
            val obj = element.jsonObject
            val title = extractText(obj["name"] ?: obj["title"]) ?: return@mapNotNull null
            val location = extractText(obj["location"]) ?: "Turnhout"
            val date = extractText(obj["startDate"] ?: obj["calendarSummary"]) ?: "Upcoming"
            EventSignal(
                title = title.take(60),
                location = location.take(60),
                date = date.take(40),
                demandBoost = 8
            )
        }
    }

    private fun findEventArray(root: JsonObject): JsonArray {
        val possibleKeys = listOf("member", "items", "events", "data")
        for (key in possibleKeys) {
            val value = root[key]
            if (value is JsonArray) return value
        }
        return JsonArray(emptyList())
    }

    private fun extractText(value: kotlinx.serialization.json.JsonElement?): String? {
        if (value == null) return null
        return when (value) {
            is JsonObject -> {
                value["nl"]?.jsonPrimitive?.contentOrNull
                    ?: value["en"]?.jsonPrimitive?.contentOrNull
                    ?: value["name"]?.let { extractText(it) }
                    ?: value["address"]?.let { extractText(it) }
            }
            else -> value.jsonPrimitive.contentOrNull
        }
    }

    private fun httpGet(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "DeliveryCompanionAIO/1.0")
        }

        return connection.inputStream.bufferedReader().use { it.readText() }
    }
}