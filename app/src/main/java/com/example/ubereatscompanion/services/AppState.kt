package com.example.ubereatscompanion.services

import com.example.ubereatscompanion.model.Offer
import com.example.ubereatscompanion.model.OfferDecision
import com.example.ubereatscompanion.model.WeatherSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LiveLocation(val latitude: Double, val longitude: Double, val accuracyMeters: Float?, val timestamp: Long)
data class LiveOfferState(val source: String, val rawText: String, val offer: Offer?, val decision: OfferDecision?, val timestamp: Long)

object AppState {
    private val _location = MutableStateFlow<LiveLocation?>(null)
    val location: StateFlow<LiveLocation?> = _location

    private val _weather = MutableStateFlow(WeatherSnapshot(condition = "unknown", rainMmPerHour = 0.0, windKph = 0.0))
    val weather: StateFlow<WeatherSnapshot> = _weather

    private val _lastOffer = MutableStateFlow<LiveOfferState?>(null)
    val lastOffer: StateFlow<LiveOfferState?> = _lastOffer

    fun updateLocation(location: LiveLocation) { _location.value = location }
    fun updateWeather(weather: WeatherSnapshot) { _weather.value = weather }
    fun updateOffer(source: String, rawText: String, decision: OfferDecision?) {
        _lastOffer.value = LiveOfferState(source, rawText.take(2_000), null, decision, System.currentTimeMillis())
    }
    fun updateOffer(offer: Offer, decision: OfferDecision?) {
        _lastOffer.value = LiveOfferState(offer.platform ?: "Offer", offer.rawText.orEmpty().take(2_000), offer, decision, System.currentTimeMillis())
    }
}
