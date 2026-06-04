package com.example.ubereatscompanion.model

enum class Recommendation { ACCEPT, MAYBE, DECLINE }

data class Offer(
    val platform: String? = "Uber Eats",
    val price: Double,
    val pickupName: String? = null,
    val pickupAddress: String? = null,
    val dropoffAddress: String? = null,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val estimatedDistanceKm: Double? = null,
    val estimatedMinutes: Int? = null,
    val pickupDistanceKm: Double? = null,
    val pickupMinutes: Int? = null,
    val tripTimeMinutes: Int? = null,
    val isStacked: Boolean = false,
    val isMultiStop: Boolean = false,
    val isOrderAndPay: Boolean = false,
    val passengerOrCustomerRating: Double? = null,
    val currentBatteryPercent: Double = 100.0,
    val rawText: String? = null
)

data class WeatherSnapshot(
    val condition: String = "unknown",
    val rainMmPerHour: Double = 0.0,
    val windKph: Double = 0.0,
    val temperatureC: Double? = null
)

data class StoreRule(
    val storeName: String,
    val maxPickupDistanceKm: Double? = null,
    val maxTotalDistanceKm: Double? = null,
    val minEuroPerKm: Double? = null,
    val minPayout: Double? = null,
    val penaltyScore: Double = 0.0,
    val allowOrderAndPay: Boolean = false,
    val allowStackedOrders: Boolean = true,
    val allowMultiStop: Boolean = true
)

data class DecisionSettings(
    val minEuroPerKm: Double = 1.20,
    val rainMinEuroPerKm: Double = 1.50,
    val heavyRainMinEuroPerKm: Double = 1.70,
    val minPayout: Double = 4.50,
    val maxDistanceKm: Double = 6.0,
    val maxPickupDistanceKm: Double = 2.0,
    val maxTotalMinutes: Int = 35,
    val minTripMinutes: Int = 0,
    val minEuroPerHour: Double = 18.0,
    val minBatteryReservePercent: Double = 25.0,
    val batteryCapacityWh: Double = 500.0,
    val whPerKm: Double = 18.0
)

data class OfferDecision(
    val recommendation: Recommendation,
    val score: Double,
    val euroPerKm: Double?,
    val euroPerHour: Double?,
    val batteryAfterTrip: Double?,
    val reasons: List<String>
)

data class WaitingZone(
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int,
    val historicalScore: Double = 50.0,
    val averageEuroPerHour: Double? = null,
    val averageOfferFrequency: Double? = null
)

data class ShiftStats(
    val totalOffers: Int,
    val accepted: Int,
    val declined: Int,
    val totalEarnings: Double,
    val totalDistanceKm: Double,
    val averageEuroPerKm: Double,
    val averageEuroPerHour: Double,
    val bestHour: Int?
)
