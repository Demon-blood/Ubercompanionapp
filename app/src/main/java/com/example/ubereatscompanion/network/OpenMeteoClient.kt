package com.example.ubereatscompanion.network

import com.example.ubereatscompanion.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenMeteoClient {
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherSnapshot = withContext(Dispatchers.IO) {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,precipitation,rain,weather_code,wind_speed_10m" +
                "&timezone=auto"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).getJSONObject("current")
            val rain = current.optDouble("rain", current.optDouble("precipitation", 0.0))
            val wind = current.optDouble("wind_speed_10m", 0.0)
            val temp = current.optDouble("temperature_2m", Double.NaN)
            val code = current.optInt("weather_code", -1)
            WeatherSnapshot(
                condition = weatherCodeToCondition(code, rain),
                rainMmPerHour = rain,
                windKph = wind,
                temperatureC = if (temp.isNaN()) null else temp
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun weatherCodeToCondition(code: Int, rain: Double): String = when {
        rain >= 4.0 -> "heavy rain"
        rain > 0.0 -> "rain"
        code in 95..99 -> "thunderstorm"
        code in 80..82 -> "showers"
        code in 61..67 -> "rain"
        code in 45..48 -> "fog"
        code in 1..3 -> "cloudy"
        code == 0 -> "clear"
        else -> "unknown"
    }
}
