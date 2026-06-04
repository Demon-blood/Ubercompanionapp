package com.example.ubereatscompanion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val platform: String?,
    val price: Double,
    val pickupName: String?,
    val pickupAddress: String?,
    val dropoffAddress: String?,
    val currentLat: Double?,
    val currentLng: Double?,
    val estimatedDistanceKm: Double?,
    val estimatedMinutes: Int?,
    val pickupDistanceKm: Double?,
    val tripTimeMinutes: Int?,
    val recommendation: String,
    val score: Double,
    val reason: String,
    val rawText: String?
)

@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val completedAt: Long,
    val platform: String? = "Uber Eats",
    val earnings: Double,
    val distanceKm: Double,
    val durationMinutes: Int,
    val pickupName: String?,
    val pickupZone: String?,
    val dropoffZone: String?,
    val weatherCondition: String?,
    val batteryUsedPercent: Double?
)

@Entity(tableName = "waiting_zones")
data class WaitingZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val centerLat: Double,
    val centerLng: Double,
    val radiusMeters: Int,
    val historicalScore: Double,
    val averageEuroPerHour: Double?,
    val averageOfferFrequency: Double?
)

@Entity(tableName = "store_rules")
data class StoreRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storeName: String,
    val maxPickupDistanceKm: Double?,
    val maxTotalDistanceKm: Double?,
    val minEuroPerKm: Double?,
    val minPayout: Double?,
    val penaltyScore: Double,
    val allowOrderAndPay: Boolean = false,
    val allowStackedOrders: Boolean = true,
    val allowMultiStop: Boolean = true,
    val notes: String? = null
)

@Entity(tableName = "user_offer_actions")
data class UserOfferActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val source: String,
    val action: String,
    val recommendation: String?,
    val price: Double?,
    val distanceKm: Double?,
    val pickupName: String?,
    val dropoffAddress: String?,
    val rawText: String?
)

@Entity(tableName = "shift_sessions")
data class ShiftSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long?,
    val platform: String?,
    val startLat: Double?,
    val startLng: Double?,
    val endLat: Double?,
    val endLng: Double?,
    val totalEarnings: Double = 0.0,
    val totalDistanceKm: Double = 0.0,
    val acceptedOffers: Int = 0,
    val declinedOffers: Int = 0
)

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val id: Int = 1,
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
    val whPerKm: Double = 18.0,
    val playVoiceAlerts: Boolean = true,
    val showOverlay: Boolean = true,
    val bringOfferToFront: Boolean = false,
    val recordScreenshots: Boolean = false,
    val taskerEnabled: Boolean = true,
    val taskerBroadcastEvents: Boolean = true,
    val taskerRunNamedTasks: Boolean = false,
    val taskerTaskOnAccept: String? = "UEC Confirm Accept",
    val taskerTaskOnMaybe: String? = "UEC Confirm Maybe",
    val taskerTaskOnDecline: String? = "UEC Confirm Decline",
    val requireUserConfirmation: Boolean = true,
    val openUberAfterConfirmation: Boolean = true
)
