package com.example.ubereatscompanion.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {
    @Query("SELECT * FROM offers ORDER BY timestamp DESC LIMIT 100")
    fun recentOffers(): Flow<List<OfferEntity>>

    @Query("SELECT * FROM offers WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun offersSince(since: Long): Flow<List<OfferEntity>>

    @Insert
    suspend fun insert(offer: OfferEntity)

    @Query("SELECT COUNT(*) FROM offers WHERE recommendation = :recommendation AND timestamp >= :since")
    suspend fun countByRecommendation(recommendation: String, since: Long): Int
}

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries ORDER BY completedAt DESC")
    fun deliveries(): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE completedAt >= :since ORDER BY completedAt DESC")
    fun deliveriesSince(since: Long): Flow<List<DeliveryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deliveries: List<DeliveryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(delivery: DeliveryEntity)
}

@Dao
interface StoreRuleDao {
    @Query("SELECT * FROM store_rules ORDER BY storeName")
    fun rules(): Flow<List<StoreRuleEntity>>

    @Query("SELECT * FROM store_rules WHERE lower(:name) LIKE '%' || lower(storeName) || '%' OR lower(storeName) LIKE '%' || lower(:name) || '%' ORDER BY length(storeName) DESC LIMIT 1")
    suspend fun findForStore(name: String): StoreRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: StoreRuleEntity)

    @Delete
    suspend fun delete(rule: StoreRuleEntity)
}

@Dao
interface WaitingZoneDao {
    @Query("SELECT * FROM waiting_zones ORDER BY historicalScore DESC")
    fun zones(): Flow<List<WaitingZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(zone: WaitingZoneEntity)
}

@Dao
interface UserOfferActionDao {
    @Query("SELECT * FROM user_offer_actions ORDER BY timestamp DESC LIMIT 100")
    fun recentActions(): Flow<List<UserOfferActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: UserOfferActionEntity)
}

@Dao
interface ShiftSessionDao {
    @Query("SELECT * FROM shift_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeShift(): ShiftSessionEntity?

    @Query("SELECT * FROM shift_sessions ORDER BY startedAt DESC LIMIT 30")
    fun recentShifts(): Flow<List<ShiftSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shift: ShiftSessionEntity): Long

    @Query("UPDATE shift_sessions SET endedAt=:endedAt, endLat=:endLat, endLng=:endLng WHERE id=:id")
    suspend fun endShift(id: Long, endedAt: Long, endLat: Double?, endLng: Double?)
}

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules WHERE id=1")
    fun rules(): Flow<AppRuleEntity?>

    @Query("SELECT * FROM app_rules WHERE id=1")
    suspend fun current(): AppRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(rule: AppRuleEntity)
}
