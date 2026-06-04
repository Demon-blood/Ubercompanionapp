package com.example.ubereatscompanion.engine

import com.example.ubereatscompanion.model.WeatherSnapshot

class BatteryEstimator {
    fun estimateEnergyWh(distanceKm: Double, whPerKm: Double, weather: WeatherSnapshot?): Double {
        val weatherMultiplier = when {
            weather == null -> 1.0
            weather.windKph >= 40.0 -> 1.25
            weather.windKph >= 25.0 -> 1.15
            else -> 1.0
        } * when {
            weather?.rainMmPerHour ?: 0.0 >= 4.0 -> 1.15
            weather?.rainMmPerHour ?: 0.0 > 0.0 -> 1.08
            else -> 1.0
        }
        return distanceKm * whPerKm * weatherMultiplier
    }

    fun estimateBatteryAfterTrip(
        currentBatteryPercent: Double,
        distanceKm: Double,
        capacityWh: Double,
        whPerKm: Double,
        weather: WeatherSnapshot?
    ): Double {
        if (capacityWh <= 0.0) return currentBatteryPercent
        val usedPercent = estimateEnergyWh(distanceKm, whPerKm, weather) / capacityWh * 100.0
        return (currentBatteryPercent - usedPercent).coerceIn(0.0, 100.0)
    }
}
