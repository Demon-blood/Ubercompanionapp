package com.example.ubereatscompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ubereatscompanion.data.CompanionRepository
import com.example.ubereatscompanion.data.OfferEntity
import com.example.ubereatscompanion.data.UserOfferActionEntity
import com.example.ubereatscompanion.engine.OfferDecisionEngine
import com.example.ubereatscompanion.model.DecisionSettings
import com.example.ubereatscompanion.model.Offer
import com.example.ubereatscompanion.model.OfferDecision
import com.example.ubereatscompanion.model.WeatherSnapshot
import com.example.ubereatscompanion.services.AppState
import com.example.ubereatscompanion.features.TaskerBridge
import com.example.ubereatscompanion.services.LiveLocation
import com.example.ubereatscompanion.services.LiveOfferState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompanionViewModel(
    private val repository: CompanionRepository,
    private val decisionEngine: OfferDecisionEngine = OfferDecisionEngine()
) : ViewModel() {
    private val _settings = MutableStateFlow(DecisionSettings())
    val settings: StateFlow<DecisionSettings> = _settings

    val weather: StateFlow<WeatherSnapshot> = AppState.weather
    val location: StateFlow<LiveLocation?> = AppState.location
    val liveOffer: StateFlow<LiveOfferState?> = AppState.lastOffer
    val recentOffers: StateFlow<List<OfferEntity>> = repository.recentOffers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentActions: StateFlow<List<UserOfferActionEntity>> = repository.recentUserActions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lastDecision = MutableStateFlow<OfferDecision?>(null)
    val lastDecision: StateFlow<OfferDecision?> = _lastDecision

    fun evaluateManualOffer(price: Double, distanceKm: Double, minutes: Int, battery: Double) {
        val loc = location.value
        val offer = Offer(
            price = price,
            estimatedDistanceKm = distanceKm,
            estimatedMinutes = minutes,
            currentBatteryPercent = battery,
            currentLat = loc?.latitude,
            currentLng = loc?.longitude
        )
        val decision = decisionEngine.evaluate(offer, _settings.value, weather.value)
        _lastDecision.value = decision
        viewModelScope.launch {
            repository.saveEvaluatedOffer(offer, decision)
            val ruleEntity = repository.currentRuleEntity()
            if (ruleEntity.taskerEnabled && ruleEntity.taskerBroadcastEvents) {
                TaskerBridge.publishOfferEvent(
                    context = com.example.ubereatscompanion.UberCompanionApp.instance,
                    source = "Manual",
                    offer = offer,
                    decision = decision,
                    runNamedTask = ruleEntity.taskerRunNamedTasks,
                    acceptTaskName = ruleEntity.taskerTaskOnAccept,
                    maybeTaskName = ruleEntity.taskerTaskOnMaybe,
                    declineTaskName = ruleEntity.taskerTaskOnDecline,
                    showConfirmation = ruleEntity.requireUserConfirmation
                )
            }
        }
    }

    fun setRainMode(rainMmPerHour: Double) {
        AppState.updateWeather(
            weather.value.copy(
                condition = if (rainMmPerHour >= 4.0) "heavy rain" else if (rainMmPerHour > 0.0) "rain" else "dry",
                rainMmPerHour = rainMmPerHour
            )
        )
    }
}
