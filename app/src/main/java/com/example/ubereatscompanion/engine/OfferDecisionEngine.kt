package com.example.ubereatscompanion.engine

import com.example.ubereatscompanion.model.*
import java.util.Locale

class OfferDecisionEngine(
    private val batteryEstimator: BatteryEstimator = BatteryEstimator(),
    private val weatherMultiplier: WeatherMultiplier = WeatherMultiplier()
) {
    fun evaluate(
        offer: Offer,
        settings: DecisionSettings,
        weather: WeatherSnapshot? = null,
        storeRule: StoreRule? = null
    ): OfferDecision {
        val reasons = mutableListOf<String>()
        val distanceKm = offer.estimatedDistanceKm
        if (distanceKm == null || distanceKm <= 0.0) {
            return OfferDecision(
                recommendation = Recommendation.MAYBE,
                score = 50.0,
                euroPerKm = null,
                euroPerHour = null,
                batteryAfterTrip = null,
                reasons = listOf("Distance is missing, so this offer cannot be fully evaluated.")
            )
        }

        val totalMinutes = offer.estimatedMinutes ?: ((offer.pickupMinutes ?: 0) + (offer.tripTimeMinutes ?: 0)).takeIf { it > 0 }
        val euroPerKm = offer.price / distanceKm
        val euroPerHour = totalMinutes?.takeIf { it > 0 }?.let { offer.price / it * 60.0 }
        val minEuroPerKm = storeRule?.minEuroPerKm ?: weatherMultiplier.minimumEuroPerKm(settings, weather)
        val maxDistance = storeRule?.maxTotalDistanceKm ?: settings.maxDistanceKm
        val maxPickupDistance = storeRule?.maxPickupDistanceKm ?: settings.maxPickupDistanceKm
        val minPayout = storeRule?.minPayout ?: settings.minPayout

        var score = 100.0 - (storeRule?.penaltyScore ?: 0.0)

        if (euroPerKm < minEuroPerKm) {
            score -= 35.0
            reasons += "€${euroPerKm.format(2)}/km is below your minimum of €${minEuroPerKm.format(2)}/km."
        }
        if ((euroPerHour ?: Double.MAX_VALUE) < settings.minEuroPerHour) {
            score -= 20.0
            reasons += "€${euroPerHour?.format(2) ?: "?"}/hour is below your minimum of €${settings.minEuroPerHour.format(2)}/hour."
        }
        if (distanceKm > maxDistance) {
            score -= 25.0
            reasons += "Distance ${distanceKm.format(1)} km is above your max of ${maxDistance.format(1)} km."
        }
        if ((offer.pickupDistanceKm ?: 0.0) > maxPickupDistance) {
            score -= 18.0
            reasons += "Pickup distance ${offer.pickupDistanceKm!!.format(1)} km is above your max pickup distance of ${maxPickupDistance.format(1)} km."
        }
        if (offer.price < minPayout) {
            score -= 20.0
            reasons += "Payout €${offer.price.format(2)} is below your minimum payout of €${minPayout.format(2)}."
        }
        if ((totalMinutes ?: 0) > settings.maxTotalMinutes) {
            score -= 20.0
            reasons += "Total estimated time ${totalMinutes} min is above your max of ${settings.maxTotalMinutes} min."
        }
        if ((offer.tripTimeMinutes ?: Int.MAX_VALUE) < settings.minTripMinutes) {
            score -= 10.0
            reasons += "Trip time ${offer.tripTimeMinutes} min is shorter than your minimum trip time."
        }
        if (offer.isOrderAndPay && storeRule?.allowOrderAndPay != true) {
            score -= 25.0
            reasons += "Order & Pay offer detected and not allowed by your current rules."
        }
        if (offer.isStacked && storeRule?.allowStackedOrders == false) {
            score -= 20.0
            reasons += "Stacked order detected and this store rule does not allow stacked orders."
        }
        if (offer.isMultiStop && storeRule?.allowMultiStop == false) {
            score -= 20.0
            reasons += "Multi-stop order detected and this store rule does not allow multi-stop orders."
        }
        if ((weather?.rainMmPerHour ?: 0.0) >= 4.0 && distanceKm > 5.0) {
            score -= 10.0
            reasons += "Heavy rain detected; long scooter trips are penalized."
        }
        if ((weather?.windKph ?: 0.0) >= 35.0) {
            score -= 8.0
            reasons += "Strong wind detected; scooter battery and route risk are penalized."
        }

        val batteryAfterTrip = batteryEstimator.estimateBatteryAfterTrip(
            currentBatteryPercent = offer.currentBatteryPercent,
            distanceKm = distanceKm,
            capacityWh = settings.batteryCapacityWh,
            whPerKm = settings.whPerKm,
            weather = weather
        )
        if (batteryAfterTrip < settings.minBatteryReservePercent) {
            score -= 30.0
            reasons += "Estimated battery after trip is ${batteryAfterTrip.format(0)}%, below your reserve of ${settings.minBatteryReservePercent.format(0)}%."
        }

        val finalScore = score.coerceIn(0.0, 100.0)
        val recommendation = when {
            finalScore >= 75.0 -> Recommendation.ACCEPT
            finalScore >= 55.0 -> Recommendation.MAYBE
            else -> Recommendation.DECLINE
        }

        return OfferDecision(
            recommendation = recommendation,
            score = finalScore,
            euroPerKm = euroPerKm,
            euroPerHour = euroPerHour,
            batteryAfterTrip = batteryAfterTrip,
            reasons = reasons.ifEmpty { listOf("Offer matches your current rules.") }
        )
    }
}

private fun Double.format(decimals: Int): String = String.format(Locale.US, "%.${decimals}f", this)
