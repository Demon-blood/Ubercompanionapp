package com.example.ubereatscompanion.services

import com.example.ubereatscompanion.model.Offer

object OfferTextParser {
    private val priceRegexes = listOf(
        Regex("€\\s*([0-9]+(?:[,.][0-9]{1,2})?)"),
        Regex("([0-9]+(?:[,.][0-9]{1,2})?)\\s*€"),
        Regex("\\$\\s*([0-9]+(?:[,.][0-9]{1,2})?)")
    )
    private val kmRegex = Regex("([0-9]+(?:[,.][0-9]+)?)\\s*(?:km|kilometers?)", RegexOption.IGNORE_CASE)
    private val mileRegex = Regex("([0-9]+(?:[,.][0-9]+)?)\\s*(?:mi|miles?)", RegexOption.IGNORE_CASE)
    private val minuteRegex = Regex("([0-9]+)\\s*(?:min|mins|minutes)", RegexOption.IGNORE_CASE)
    private val pickupDistanceRegex = Regex("(?:pickup|pick up|to pickup|naar pickup)[^0-9]{0,20}([0-9]+(?:[,.][0-9]+)?)\\s*(km|mi|miles?)", RegexOption.IGNORE_CASE)
    private val dropoffRegex = Regex("(?:drop.?off|destination|deliver to|naar)[\\s:.-]+([^\\n]+)", RegexOption.IGNORE_CASE)
    private val pickupRegex = Regex("(?:pickup|pick up|restaurant|store|from)[\\s:.-]+([^\\n]+)", RegexOption.IGNORE_CASE)

    fun parse(text: String, currentBatteryPercent: Double = 100.0): Offer? {
        val cleaned = text.replace('\r', '\n').lines().map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n")
        val price = priceRegexes.firstNotNullOfOrNull { it.find(cleaned)?.groupValues?.getOrNull(1)?.toDoubleCompat() }
        val kmDistances = kmRegex.findAll(cleaned).mapNotNull { it.groupValues.getOrNull(1)?.toDoubleCompat() }.toList()
        val mileDistances = mileRegex.findAll(cleaned).mapNotNull { it.groupValues.getOrNull(1)?.toDoubleCompat()?.times(1.60934) }.toList()
        val allDistances = kmDistances + mileDistances
        val minutes = minuteRegex.findAll(cleaned).mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }.toList()
        val pickupDistance = pickupDistanceRegex.find(cleaned)?.let {
            val value = it.groupValues[1].toDoubleCompat() ?: return@let null
            val unit = it.groupValues[2].lowercase()
            if (unit.startsWith("mi")) value * 1.60934 else value
        }
        val pickupName = pickupRegex.find(cleaned)?.groupValues?.getOrNull(1)?.cleanLocation()
            ?: cleaned.lines().firstOrNull { it.contains("restaurant", true) || it.contains("pickup", true) }?.cleanLocation()
        val dropoff = dropoffRegex.find(cleaned)?.groupValues?.getOrNull(1)?.cleanLocation()

        val hasOfferSignal = price != null || allDistances.isNotEmpty() || minutes.isNotEmpty()
        if (!hasOfferSignal) return null

        val flags = cleaned.lowercase()
        return Offer(
            price = price ?: 0.0,
            pickupName = pickupName,
            pickupAddress = pickupName,
            dropoffAddress = dropoff,
            estimatedDistanceKm = allDistances.maxOrNull(),
            estimatedMinutes = minutes.maxOrNull(),
            pickupDistanceKm = pickupDistance,
            pickupMinutes = minutes.firstOrNull(),
            tripTimeMinutes = minutes.lastOrNull(),
            isStacked = flags.contains("stacked") || flags.contains("multiple orders") || flags.contains("2 orders"),
            isMultiStop = flags.contains("multi-stop") || flags.contains("multiple stops") || flags.contains("2 stops"),
            isOrderAndPay = flags.contains("order & pay") || flags.contains("order and pay") || flags.contains("shop & pay") || flags.contains("shopping"),
            currentBatteryPercent = currentBatteryPercent,
            rawText = cleaned
        )
    }

    private fun String.toDoubleCompat(): Double? = replace(',', '.').toDoubleOrNull()
    private fun String.cleanLocation(): String = replace(Regex("^(pickup|pick up|restaurant|store|from|drop.?off|destination|deliver to)[:\\s.-]*", RegexOption.IGNORE_CASE), "").trim().take(120)
}
