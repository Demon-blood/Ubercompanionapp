package com.example.ubereatscompanion.features

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.ubereatscompanion.UberCompanionApp
import com.example.ubereatscompanion.engine.OfferDecisionEngine
import com.example.ubereatscompanion.services.AppState
import com.example.ubereatscompanion.services.OfferTextParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OfferNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = OfferTextParser
    private val engine = OfferDecisionEngine()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val platform = SupportedPlatform.fromPackage(sbn.packageName) ?: return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val raw = listOf(title, text, bigText).filter { it.isNotBlank() }.joinToString("\n")
        if (raw.isBlank()) return
        val offer = parser.parse(raw)?.copy(platform = platform.label, rawText = raw) ?: return
        val app = application as UberCompanionApp
        scope.launch {
            val ruleEntity = app.repository.currentRuleEntity()
            val settings = app.repository.currentSettings()
            val decision = engine.evaluate(offer, settings, AppState.weather.value, app.repository.findStoreRule(offer.pickupName))
            AppState.updateOffer(offer, decision)
            app.repository.saveEvaluatedOffer(offer, decision)
            if (ruleEntity.taskerEnabled && ruleEntity.taskerBroadcastEvents) {
                TaskerBridge.publishOfferEvent(
                    context = this@OfferNotificationListener,
                    source = "Notification",
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
}
