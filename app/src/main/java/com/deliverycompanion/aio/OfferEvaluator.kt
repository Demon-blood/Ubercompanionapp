package com.deliverycompanion.aio

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object OfferEvaluator {
    fun evaluate(offer: OfferInput, rules: OfferRules): OfferDecision {
        val reasons = mutableListOf<String>()
        val positives = mutableListOf<String>()
        val pay = offer.estimatedPay
        val km = offer.estimatedDistanceKm
        val minutes = offer.estimatedMinutes
        val wait = offer.pickupWaitMinutes
        val payPerKm = if (km > 0.0) pay / km else 0.0
        val hourly = if (minutes > 0.0) pay / (minutes / 60.0) else 0.0
        val blocked = csvToList(rules.blockedAreasCsv)
        val preferred = csvToList(rules.preferredAreasCsv)
        val area = offer.dropoffArea.lowercase(Locale.getDefault())

        if (pay < rules.minPay) reasons += "Pay below ${money(rules.minPay)}"
        else positives += "Minimum pay passed"

        if (km > 0.0 && payPerKm < rules.minPayPerKm) reasons += "Below ${money(rules.minPayPerKm)}/km"
        else if (km > 0.0) positives += "${money(payPerKm)}/km"

        if (minutes > 0.0 && hourly < rules.minHourlyRate) reasons += "Below ${money(rules.minHourlyRate)}/hour"
        else if (minutes > 0.0) positives += "${money(hourly)}/hour"

        if (km > rules.maxDistanceKm) reasons += "Distance above ${rules.maxDistanceKm} km"
        if (minutes > rules.maxMinutes) reasons += "Time above ${rules.maxMinutes.toInt()} min"
        if (wait > rules.maxPickupWait) reasons += "Pickup wait above ${rules.maxPickupWait.toInt()} min"
        if (offer.stackedOrder && pay < rules.rejectStackedBelow) reasons += "Stacked order payout too low"
        if (blocked.any { area.contains(it) }) reasons += "Drop-off area is blocked"
        if (preferred.any { area.contains(it) }) positives += "Preferred area"

        val rawScore = 50.0 +
            (payPerKm - rules.minPayPerKm) * 18.0 +
            (hourly - rules.minHourlyRate) * 1.5 -
            max(0.0, km - rules.maxDistanceKm) * 6.0 -
            max(0.0, wait - rules.maxPickupWait) * 3.0

        return OfferDecision(
            recommendation = if (reasons.isEmpty()) "ACCEPT" else "REJECT",
            score = min(100.0, max(0.0, rawScore)).toInt(),
            payPerKm = payPerKm,
            hourlyRate = hourly,
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