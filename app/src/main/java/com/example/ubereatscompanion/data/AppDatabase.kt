package com.example.ubereatscompanion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [OfferEntity::class, DeliveryEntity::class, WaitingZoneEntity::class, StoreRuleEntity::class, UserOfferActionEntity::class, ShiftSessionEntity::class, AppRuleEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun offerDao(): OfferDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun waitingZoneDao(): WaitingZoneDao
    abstract fun storeRuleDao(): StoreRuleDao
    abstract fun userOfferActionDao(): UserOfferActionDao
    abstract fun shiftSessionDao(): ShiftSessionDao
    abstract fun appRuleDao(): AppRuleDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "uber_eats_companion.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
