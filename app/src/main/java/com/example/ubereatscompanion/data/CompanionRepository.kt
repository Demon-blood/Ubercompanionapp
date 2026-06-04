package com.example.ubereatscompanion.data

import com.example.ubereatscompanion.model.DecisionSettings
import com.example.ubereatscompanion.model.Offer
import com.example.ubereatscompanion.model.OfferDecision
import com.example.ubereatscompanion.model.StoreRule
import kotlinx.coroutines.flow.Flow

class CompanionRepository(private val db: AppDatabase) {
    val recentOffers: Flow<List<OfferEntity>> = db.offerDao().recentOffers()
    val deliveries: Flow<List<DeliveryEntity>> = db.deliveryDao().deliveries()
    val zones: Flow<List<WaitingZoneEntity>> = db.waitingZoneDao().zones()
    val storeRules: Flow<List<StoreRuleEntity>> = db.storeRuleDao().rules()
    val appRules: Flow<AppRuleEntity?> = db.appRuleDao().rules()
    val recentShifts: Flow<List<ShiftSessionEntity>> = db.shiftSessionDao().recentShifts()
    val recentUserActions: Flow<List<UserOfferActionEntity>> = db.userOfferActionDao().recentActions()

    suspend fun currentRuleEntity(): AppRuleEntity = db.appRuleDao().current() ?: AppRuleEntity().also { db.appRuleDao().save(it) }

    suspend fun currentSettings(): DecisionSettings {
        val r = currentRuleEntity()
        return DecisionSettings(
            minEuroPerKm = r.minEuroPerKm,
            rainMinEuroPerKm = r.rainMinEuroPerKm,
            heavyRainMinEuroPerKm = r.heavyRainMinEuroPerKm,
            minPayout = r.minPayout,
            maxDistanceKm = r.maxDistanceKm,
            maxPickupDistanceKm = r.maxPickupDistanceKm,
            maxTotalMinutes = r.maxTotalMinutes,
            minTripMinutes = r.minTripMinutes,
            minEuroPerHour = r.minEuroPerHour,
            minBatteryReservePercent = r.minBatteryReservePercent,
            batteryCapacityWh = r.batteryCapacityWh,
            whPerKm = r.whPerKm
        )
    }

    suspend fun findStoreRule(storeName: String?): StoreRule? {
        if (storeName.isNullOrBlank()) return null
        return db.storeRuleDao().findForStore(storeName)?.let {
            StoreRule(
                storeName = it.storeName,
                maxPickupDistanceKm = it.maxPickupDistanceKm,
                maxTotalDistanceKm = it.maxTotalDistanceKm,
                minEuroPerKm = it.minEuroPerKm,
                minPayout = it.minPayout,
                penaltyScore = it.penaltyScore,
                allowOrderAndPay = it.allowOrderAndPay,
                allowStackedOrders = it.allowStackedOrders,
                allowMultiStop = it.allowMultiStop
            )
        }
    }

    suspend fun importDeliveries(deliveries: List<DeliveryEntity>) {
        db.deliveryDao().insertAll(deliveries)
    }

    suspend fun saveSettings(rule: AppRuleEntity) {
        db.appRuleDao().save(rule)
    }

    suspend fun saveEvaluatedOffer(offer: Offer, decision: OfferDecision) {
        db.offerDao().insert(
            OfferEntity(
                timestamp = System.currentTimeMillis(),
                platform = offer.platform,
                price = offer.price,
                pickupName = offer.pickupName,
                pickupAddress = offer.pickupAddress,
                dropoffAddress = offer.dropoffAddress,
                currentLat = offer.currentLat,
                currentLng = offer.currentLng,
                estimatedDistanceKm = offer.estimatedDistanceKm,
                estimatedMinutes = offer.estimatedMinutes,
                pickupDistanceKm = offer.pickupDistanceKm,
                tripTimeMinutes = offer.tripTimeMinutes,
                recommendation = decision.recommendation.name,
                score = decision.score,
                reason = decision.reasons.joinToString("\n"),
                rawText = offer.rawText
            )
        )
    }

    suspend fun startShift(platform: String?, lat: Double?, lng: Double?) {
        if (db.shiftSessionDao().activeShift() == null) {
            db.shiftSessionDao().upsert(ShiftSessionEntity(startedAt = System.currentTimeMillis(), endedAt = null, platform = platform, startLat = lat, startLng = lng, endLat = null, endLng = null))
        }
    }

    suspend fun stopShift(lat: Double?, lng: Double?) {
        db.shiftSessionDao().activeShift()?.let { db.shiftSessionDao().endShift(it.id, System.currentTimeMillis(), lat, lng) }
    }

    suspend fun saveUserOfferAction(action: String, source: String = "confirmation") {
        val live = com.example.ubereatscompanion.services.AppState.lastOffer.value
        db.userOfferActionDao().insert(
            UserOfferActionEntity(
                timestamp = System.currentTimeMillis(),
                source = source,
                action = action,
                recommendation = live?.decision?.recommendation?.name,
                price = live?.offer?.price,
                distanceKm = live?.offer?.estimatedDistanceKm,
                pickupName = live?.offer?.pickupName ?: live?.offer?.pickupAddress,
                dropoffAddress = live?.offer?.dropoffAddress,
                rawText = live?.rawText
            )
        )
    }
}
