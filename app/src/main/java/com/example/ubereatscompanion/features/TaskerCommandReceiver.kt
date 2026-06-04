package com.example.ubereatscompanion.features

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.ubereatscompanion.UberCompanionApp
import com.example.ubereatscompanion.data.AppRuleEntity
import com.example.ubereatscompanion.services.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Commands that Tasker can send to this companion app.
 *
 * Examples in Tasker: Action = Send Intent, Action field equals one of
 * TaskerBridge.ACTION_* constants. Use extras like uec_mode:rain.
 */
class TaskerCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? UberCompanionApp ?: return
        val repository = app.repository
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        when (intent.action) {
            TaskerBridge.ACTION_START_SHIFT -> scope.launch {
                val loc = AppState.location.value
                repository.startShift("Uber Eats", loc?.latitude, loc?.longitude)
            }
            TaskerBridge.ACTION_STOP_SHIFT -> scope.launch {
                val loc = AppState.location.value
                repository.stopShift(loc?.latitude, loc?.longitude)
            }
            TaskerBridge.ACTION_SET_MODE -> scope.launch {
                setMode(repository, intent.getStringExtra(TaskerBridge.EXTRA_MODE).orEmpty())
            }
            TaskerBridge.ACTION_SPEAK_LAST_OFFER -> {
                val last = AppState.lastOffer.value
                val text = last?.decision?.let { decision ->
                    val price = last.offer?.price?.let { "€%.2f".format(it) }.orEmpty()
                    "$price ${decision.recommendation.name}. ${decision.reasons.firstOrNull().orEmpty()}"
                } ?: "No offer has been detected yet."
                VoiceAlertManager(context).speak(text)
            }
            TaskerBridge.ACTION_OPEN_NAVIGATION -> {
                val query = intent.getStringExtra(TaskerBridge.EXTRA_NAV_QUERY)
                    ?: AppState.lastOffer.value?.offer?.pickupAddress
                    ?: AppState.lastOffer.value?.offer?.pickupName
                if (!query.isNullOrBlank()) NavigationLauncher.openGoogleMaps(context, query)
            }
            TaskerBridge.ACTION_SHOW_CONFIRMATION -> {
                ConfirmationLauncher.showConfirmation(context)
            }
            TaskerBridge.ACTION_MARK_ACCEPTED -> scope.launch {
                repository.saveUserOfferAction("USER_ACCEPT_CONFIRMED", "tasker")
            }
            TaskerBridge.ACTION_MARK_DECLINED -> scope.launch {
                repository.saveUserOfferAction("USER_DECLINE_CONFIRMED", "tasker")
            }
            TaskerBridge.ACTION_MARK_MAYBE -> scope.launch {
                repository.saveUserOfferAction("USER_MAYBE_CONFIRMED", "tasker")
            }
            TaskerBridge.ACTION_CONFIGURE_TASKER -> scope.launch {
                val current = repository.currentRuleEntity()
                repository.saveSettings(
                    current.copy(
                        taskerEnabled = intent.getBooleanExtra(TaskerBridge.EXTRA_TASKER_ENABLED, current.taskerEnabled),
                        taskerBroadcastEvents = intent.getBooleanExtra(TaskerBridge.EXTRA_TASKER_BROADCAST_EVENTS, current.taskerBroadcastEvents),
                        taskerRunNamedTasks = false,
                        taskerTaskOnAccept = intent.getStringExtra(TaskerBridge.EXTRA_TASKER_TASK_ACCEPT) ?: current.taskerTaskOnAccept,
                        taskerTaskOnMaybe = intent.getStringExtra(TaskerBridge.EXTRA_TASKER_TASK_MAYBE) ?: current.taskerTaskOnMaybe,
                        taskerTaskOnDecline = intent.getStringExtra(TaskerBridge.EXTRA_TASKER_TASK_DECLINE) ?: current.taskerTaskOnDecline
                    )
                )
            }
            else -> Log.d("TaskerCommandReceiver", "Ignored unsupported action ${intent.action}")
        }
    }

    private suspend fun setMode(repository: com.example.ubereatscompanion.data.CompanionRepository, mode: String) {
        val current = repository.currentRuleEntity()
        val updated: AppRuleEntity = when (mode.lowercase()) {
            "rain" -> current.copy(minEuroPerKm = current.rainMinEuroPerKm, minPayout = maxOf(current.minPayout, 5.0))
            "heavy_rain", "heavyrain", "storm" -> current.copy(minEuroPerKm = current.heavyRainMinEuroPerKm, minPayout = maxOf(current.minPayout, 6.0), maxDistanceKm = minOf(current.maxDistanceKm, 5.0))
            "strict" -> current.copy(minEuroPerKm = maxOf(current.minEuroPerKm, 1.60), minEuroPerHour = maxOf(current.minEuroPerHour, 22.0), maxPickupDistanceKm = minOf(current.maxPickupDistanceKm, 1.5))
            "normal", "dry" -> current.copy(minEuroPerKm = 1.20, minPayout = 4.50, maxDistanceKm = 6.0, maxPickupDistanceKm = 2.0)
            else -> current
        }
        repository.saveSettings(updated)
    }
}
