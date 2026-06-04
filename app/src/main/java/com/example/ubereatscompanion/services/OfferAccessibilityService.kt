package com.example.ubereatscompanion.services

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.ubereatscompanion.UberCompanionApp
import com.example.ubereatscompanion.engine.OfferDecisionEngine
import com.example.ubereatscompanion.features.TaskerBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OfferAccessibilityService : AccessibilityService() {
    private val parser = OfferTextParser
    private val engine = OfferDecisionEngine()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastTextHash: Int = 0
    private var lastSavedAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val text = buildString { collectText(root, this) }
        val hash = text.hashCode()
        val now = System.currentTimeMillis()
        if (hash == lastTextHash && now - lastSavedAt < 10_000L) return

        val offer = parser.parse(text, currentBatteryPercent = 100.0) ?: return
        val location = AppState.location.value
        val enriched = offer.copy(currentLat = location?.latitude, currentLng = location?.longitude)
        lastTextHash = hash
        lastSavedAt = now

        scope.launch {
            runCatching {
                val repository = (application as UberCompanionApp).repository
                val ruleEntity = repository.currentRuleEntity()
                val settings = repository.currentSettings()
                val decision = engine.evaluate(enriched, settings, AppState.weather.value, repository.findStoreRule(enriched.pickupName))
                AppState.updateOffer(enriched, decision)
                repository.saveEvaluatedOffer(enriched, decision)
                if (ruleEntity.taskerEnabled && ruleEntity.taskerBroadcastEvents) {
                    TaskerBridge.publishOfferEvent(
                        context = this@OfferAccessibilityService,
                        source = "Accessibility",
                        offer = enriched,
                        decision = decision,
                        runNamedTask = ruleEntity.taskerRunNamedTasks,
                        acceptTaskName = ruleEntity.taskerTaskOnAccept,
                        maybeTaskName = ruleEntity.taskerTaskOnMaybe,
                        declineTaskName = ruleEntity.taskerTaskOnDecline,
                        showConfirmation = ruleEntity.requireUserConfirmation
                    )
                }
            }.onFailure { Log.e("OfferAccessibility", "Failed to save detected offer", it) }
        }
    }

    override fun onInterrupt() = Unit

    private fun collectText(node: AccessibilityNodeInfo, out: StringBuilder) {
        node.text?.takeIf { it.isNotBlank() }?.let { out.append(it).append('\n') }
        node.contentDescription?.takeIf { it.isNotBlank() }?.let { out.append(it).append('\n') }
        for (i in 0 until node.childCount) node.getChild(i)?.let { collectText(it, out) }
    }
}
