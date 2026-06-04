package com.example.ubereatscompanion.features

import com.example.ubereatscompanion.data.DeliveryEntity
import com.example.ubereatscompanion.data.OfferEntity
import com.example.ubereatscompanion.model.ShiftStats
import java.time.Instant
import java.time.ZoneId

object ShiftStatsEngine {
    fun calculate(offers: List<OfferEntity>, deliveries: List<DeliveryEntity>, zoneId: ZoneId = ZoneId.systemDefault()): ShiftStats {
        val accepted = offers.count { it.recommendation == "ACCEPT" }
        val declined = offers.count { it.recommendation == "DECLINE" }
        val totalEarnings = deliveries.sumOf { it.earnings }
        val totalDistance = deliveries.sumOf { it.distanceKm }
        val totalMinutes = deliveries.sumOf { it.durationMinutes }.coerceAtLeast(1)
        val byHour = deliveries.groupBy { Instant.ofEpochMilli(it.completedAt).atZone(zoneId).hour }
            .maxByOrNull { entry -> entry.value.sumOf { it.earnings } }
            ?.key
        return ShiftStats(
            totalOffers = offers.size,
            accepted = accepted,
            declined = declined,
            totalEarnings = totalEarnings,
            totalDistanceKm = totalDistance,
            averageEuroPerKm = if (totalDistance > 0) totalEarnings / totalDistance else 0.0,
            averageEuroPerHour = totalEarnings / totalMinutes * 60.0,
            bestHour = byHour
        )
    }
}
