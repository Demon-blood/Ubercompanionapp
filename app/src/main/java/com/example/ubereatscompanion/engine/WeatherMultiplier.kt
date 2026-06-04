package com.example.ubereatscompanion.engine

import com.example.ubereatscompanion.model.DecisionSettings
import com.example.ubereatscompanion.model.WeatherSnapshot

class WeatherMultiplier {
    fun minimumEuroPerKm(settings: DecisionSettings, weather: WeatherSnapshot?): Double {
        if (weather == null) return settings.minEuroPerKm
        return when {
            weather.rainMmPerHour >= 4.0 -> settings.heavyRainMinEuroPerKm
            weather.rainMmPerHour > 0.0 -> settings.rainMinEuroPerKm
            weather.windKph >= 40.0 -> settings.rainMinEuroPerKm
            else -> settings.minEuroPerKm
        }
    }
}
