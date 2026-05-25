package com.deliverycompanion.aio

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object OfferEvaluator {
    fun evaluate(offer: OfferInput, rules: OfferRules): OfferDecision {
        val reasons = mutableListOf<String>()
        val positives = mutableListOf<String>()
        val grossPay = offer.estimatedPay
        val km = offer.estimatedDistanceKm
        val minutes = offer.estimatedMinutes
        val wait = offer.pickupWaitMinutes

        val estimatedKwh = if (km > 0.0) (km / 100.0) * rules.scooterKwhPer100Km else 0.0
        val electricityCost = estimatedKwh * rules.electricityPricePerKwh
        val netPay = grossPay - electricityCost
        val batteryPercentUsed = if (rules.batteryCapacityKwh > 0.0) {
            (estimatedKwh / rules.batteryCapacityKwh) * 100.0
        } else {
            0.0
        }

        val payPerKm = if (km > 0.0) netPay / km else 0.0
        val hourly = if (minutes > 0.0) netPay / (minutes / 60.0) else 0.0
        val blocked = csvToList(rules.blockedAreasCsv)
        val preferred = csvToList(rules.preferredAreasCsv)
        val area = offer.dropoffArea.lowercase(Locale.getDefault())

        if (netPay < rules.minPay) reasons += "Net pay below ${money(rules.minPay)} after electricity"
        else positives += "Minimum net pay passed"

        if (km > 0.0 && payPerKm < rules.minPayPerKm) reasons += "Below ${money(rules.minPayPerKm)}/km after electricity"
        else if (km > 0.0) positives += "${money(payPerKm)}/km net"

        if (minutes > 0.0 && hourly < rules.minHourlyRate) reasons += "Below ${money(rules.minHourlyRate)}/hour after electricity"
        else if (minutes > 0.0) positives += "${money(hourly)}/hour net"

        if (km > rules.maxDistanceKm) reasons += "Distance above ${rules.maxDistanceKm} km"
        if (minutes > rules.maxMinutes) reasons += "Time above ${rules.maxMinutes.toInt()} min"
        if (wait > rules.maxPickupWait) reasons += "Pickup wait above ${rules.maxPickupWait.toInt()} min"
        if (offer.stackedOrder && grossPay < rules.rejectStackedBelow) reasons += "Stacked order payout too low"
        if (blocked.any { area.contains(it) }) reasons += "Drop-off area is blocked"
        if (preferred.any { area.contains(it) }) positives += "Preferred area"
        if (batteryPercentUsed > 25.0) reasons += "High battery use estimate: ${round1(batteryPercentUsed)}%"

        val rawScore = 50.0 +
            (payPerKm - rules.minPayPerKm) * 18.0 +
            (hourly - rules.minHourlyRate) * 1.5 -
            max(0.0, km - rules.maxDistanceKm) * 6.0 -
            max(0.0, wait - rules.maxPickupWait) * 3.0 -
            max(0.0, batteryPercentUsed - 15.0) * 0.5

        return OfferDecision(
            recommendation = if (reasons.isEmpty()) "ACCEPT" else "REJECT",
            score = min(100.0, max(0.0, rawScore)).toInt(),
            payPerKm = payPerKm,
            hourlyRate = hourly,
            estimatedElectricityCost = electricityCost,
            estimatedNetPay = netPay,
            estimatedBatteryPercentUsed = batteryPercentUsed,
            reasons = reasons,
            positives = positives
        )
    }

    private fun csvToList(value: String): List<String> = value
        .split(",")
        .map { it.trim().lowercase(Locale.getDefault()) }
        .filter { it.isNotBlank() }
}

fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "BE")).format(value)
fun round1(value: Double): String = String.format(Locale.US, "%.1f", value)