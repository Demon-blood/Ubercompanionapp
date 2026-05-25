package com.deliverycompanion.aio

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object LocalStore {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun saveTrips(context: Context, trips: List<TripLog>) {
        context.getSharedPreferences("delivery_companion", Context.MODE_PRIVATE)
            .edit()
            .putString("trips", json.encodeToString(trips))
            .apply()
    }

    fun loadTrips(context: Context): List<TripLog> {
        val raw = context.getSharedPreferences("delivery_companion", Context.MODE_PRIVATE)
            .getString("trips", null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<TripLog>>(raw) }.getOrDefault(emptyList())
    }

    fun saveRules(context: Context, rules: OfferRules) {
        context.getSharedPreferences("delivery_companion", Context.MODE_PRIVATE)
            .edit()
            .putString("rules", json.encodeToString(rules))
            .apply()
    }

    fun loadRules(context: Context): OfferRules {
        val raw = context.getSharedPreferences("delivery_companion", Context.MODE_PRIVATE)
            .getString("rules", null) ?: return OfferRules()
        return runCatching { json.decodeFromString<OfferRules>(raw) }.getOrDefault(OfferRules())
    }
}