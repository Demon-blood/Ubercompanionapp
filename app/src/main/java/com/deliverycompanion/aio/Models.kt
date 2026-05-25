package com.deliverycompanion.aio

import kotlinx.serialization.Serializable

@Serializable
data class OfferInput(
    val estimatedPay: Double = 0.0,
    val estimatedDistanceKm: Double = 0.0,
    val estimatedMinutes: Double = 0.0,
    val pickupWaitMinutes: Double = 0.0,
    val restaurant: String = "",
    val dropoffArea: String = "",
    val stackedOrder: Boolean = false,
    val notes: String = ""
)

@Serializable
data class OfferRules(
    val minPay: Double = 5.0,
    val minPayPerKm: Double = 1.2,
    val minHourlyRate: Double = 18.0,
    val maxDistanceKm: Double = 8.0,
    val maxMinutes: Double = 35.0,
    val maxPickupWait: Double = 10.0,
    val rejectStackedBelow: Double = 9.0,
    val blockedAreasCsv: String = "",
    val preferredAreasCsv: String = ""
)

@Serializable
data class OfferDecision(
    val recommendation: String,
    val score: Int,
    val payPerKm: Double,
    val hourlyRate: Double,
    val reasons: List<String>,
    val positives: List<String>
)

@Serializable
data class TripLog(
    val id: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val platform: String = "Uber Eats",
    val city: String = "",
    val vehicle: String = "car",
    val orders: Int = 1,
    val distanceKm: Double = 0.0,
    val basePay: Double = 0.0,
    val tips: Double = 0.0,
    val promotions: Double = 0.0,
    val parking: Double = 0.0,
    val fuelCost: Double = 0.0,
    val notes: String = ""
) {
    val income: Double get() = basePay + tips + promotions
    val expenses: Double get() = parking + fuelCost
    val profit: Double get() = income - expenses
}