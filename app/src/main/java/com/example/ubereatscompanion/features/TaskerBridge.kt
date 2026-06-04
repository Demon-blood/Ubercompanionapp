package com.example.ubereatscompanion.features

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.ubereatscompanion.model.Offer
import com.example.ubereatscompanion.model.OfferDecision
import com.example.ubereatscompanion.model.Recommendation

/**
 * Tasker integration that exposes offer decisions through normal Android broadcasts.
 *
 * This deliberately does not tap or control any delivery app UI. Tasker can listen to
 * the public actions below and decide what to do with the data: speak it, log it,
 * start navigation, set phone modes, vibrate, notify, etc.
 */
object TaskerBridge {
    const val ACTION_OFFER_EVALUATED = "com.example.ubereatscompanion.TASKER.OFFER_EVALUATED"
    const val ACTION_RECOMMEND_ACCEPT = "com.example.ubereatscompanion.TASKER.RECOMMEND_ACCEPT"
    const val ACTION_RECOMMEND_MAYBE = "com.example.ubereatscompanion.TASKER.RECOMMEND_MAYBE"
    const val ACTION_RECOMMEND_DECLINE = "com.example.ubereatscompanion.TASKER.RECOMMEND_DECLINE"

    const val ACTION_START_SHIFT = "com.example.ubereatscompanion.TASKER.START_SHIFT"
    const val ACTION_STOP_SHIFT = "com.example.ubereatscompanion.TASKER.STOP_SHIFT"
    const val ACTION_SET_MODE = "com.example.ubereatscompanion.TASKER.SET_MODE"
    const val ACTION_SPEAK_LAST_OFFER = "com.example.ubereatscompanion.TASKER.SPEAK_LAST_OFFER"
    const val ACTION_OPEN_NAVIGATION = "com.example.ubereatscompanion.TASKER.OPEN_NAVIGATION"
    const val ACTION_CONFIGURE_TASKER = "com.example.ubereatscompanion.TASKER.CONFIGURE_TASKER"
    const val ACTION_SHOW_CONFIRMATION = "com.example.ubereatscompanion.TASKER.SHOW_CONFIRMATION"
    const val ACTION_MARK_ACCEPTED = "com.example.ubereatscompanion.TASKER.MARK_ACCEPTED"
    const val ACTION_MARK_DECLINED = "com.example.ubereatscompanion.TASKER.MARK_DECLINED"
    const val ACTION_MARK_MAYBE = "com.example.ubereatscompanion.TASKER.MARK_MAYBE"

    const val EXTRA_SOURCE = "uec_source"
    const val EXTRA_RECOMMENDATION = "uec_recommendation"
    const val EXTRA_SCORE = "uec_score"
    const val EXTRA_PRICE = "uec_price"
    const val EXTRA_DISTANCE_KM = "uec_distance_km"
    const val EXTRA_PICKUP_DISTANCE_KM = "uec_pickup_distance_km"
    const val EXTRA_MINUTES = "uec_minutes"
    const val EXTRA_EURO_PER_KM = "uec_euro_per_km"
    const val EXTRA_EURO_PER_HOUR = "uec_euro_per_hour"
    const val EXTRA_BATTERY_AFTER = "uec_battery_after"
    const val EXTRA_PICKUP = "uec_pickup"
    const val EXTRA_DROPOFF = "uec_dropoff"
    const val EXTRA_REASONS = "uec_reasons"
    const val EXTRA_RAW_TEXT = "uec_raw_text"
    const val EXTRA_MODE = "uec_mode"
    const val EXTRA_NAV_QUERY = "uec_nav_query"
    const val EXTRA_TASKER_ENABLED = "uec_tasker_enabled"
    const val EXTRA_TASKER_BROADCAST_EVENTS = "uec_tasker_broadcast_events"
    const val EXTRA_TASKER_RUN_NAMED_TASKS = "uec_tasker_run_named_tasks"
    const val EXTRA_TASKER_TASK_ACCEPT = "uec_tasker_task_accept"
    const val EXTRA_TASKER_TASK_MAYBE = "uec_tasker_task_maybe"
    const val EXTRA_TASKER_TASK_DECLINE = "uec_tasker_task_decline"

    private const val TASKER_ACTION_TASK = "net.dinglisch.android.tasker.ACTION_TASK"
    private const val TASKER_EXTRA_TASK_NAME = "task_name"

    fun publishOfferEvent(
        context: Context,
        source: String,
        offer: Offer,
        decision: OfferDecision,
        runNamedTask: Boolean = false,
        acceptTaskName: String? = null,
        maybeTaskName: String? = null,
        declineTaskName: String? = null,
        showConfirmation: Boolean = false
    ) {
        val base = createOfferIntent(ACTION_OFFER_EVALUATED, source, offer, decision)
        context.sendBroadcast(base)

        val recommendationAction = when (decision.recommendation) {
            Recommendation.ACCEPT -> ACTION_RECOMMEND_ACCEPT
            Recommendation.MAYBE -> ACTION_RECOMMEND_MAYBE
            Recommendation.DECLINE -> ACTION_RECOMMEND_DECLINE
        }
        context.sendBroadcast(createOfferIntent(recommendationAction, source, offer, decision))

        if (showConfirmation) {
            ConfirmationLauncher.showConfirmation(context)
        }

        // Kept only for safe Tasker workflows such as showing Tasker's own confirmation scene,
        // speaking the offer, changing phone state, or logging. Do not use it to inject taps
        // into Uber Eats or any third-party app UI.
        if (runNamedTask) {
            val taskName = when (decision.recommendation) {
                Recommendation.ACCEPT -> acceptTaskName
                Recommendation.MAYBE -> maybeTaskName
                Recommendation.DECLINE -> declineTaskName
            }
            if (!taskName.isNullOrBlank()) runTaskerTask(context, taskName, source, offer, decision)
        }
    }

    fun createOfferIntent(action: String, source: String, offer: Offer, decision: OfferDecision): Intent {
        return Intent(action).apply {
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            putExtra(EXTRA_SOURCE, source)
            putExtra(EXTRA_RECOMMENDATION, decision.recommendation.name)
            putExtra(EXTRA_SCORE, decision.score)
            putExtra(EXTRA_PRICE, offer.price)
            putExtra(EXTRA_DISTANCE_KM, offer.estimatedDistanceKm ?: -1.0)
            putExtra(EXTRA_PICKUP_DISTANCE_KM, offer.pickupDistanceKm ?: -1.0)
            putExtra(EXTRA_MINUTES, offer.estimatedMinutes ?: -1)
            putExtra(EXTRA_EURO_PER_KM, decision.euroPerKm ?: -1.0)
            putExtra(EXTRA_EURO_PER_HOUR, decision.euroPerHour ?: -1.0)
            putExtra(EXTRA_BATTERY_AFTER, decision.batteryAfterTrip ?: -1.0)
            putExtra(EXTRA_PICKUP, offer.pickupName ?: offer.pickupAddress.orEmpty())
            putExtra(EXTRA_DROPOFF, offer.dropoffAddress.orEmpty())
            putExtra(EXTRA_REASONS, decision.reasons.joinToString(" | "))
            putExtra(EXTRA_RAW_TEXT, offer.rawText.orEmpty().take(4_000))
        }
    }

    fun runTaskerTask(context: Context, taskName: String, source: String, offer: Offer, decision: OfferDecision): Boolean {
        return try {
            val intent = createOfferIntent(TASKER_ACTION_TASK, source, offer, decision).apply {
                putExtra(TASKER_EXTRA_TASK_NAME, taskName)
            }
            context.sendBroadcast(intent)
            true
        } catch (security: SecurityException) {
            Log.w("TaskerBridge", "Tasker task could not be run. Enable Tasker external access and grant run-task permission.", security)
            false
        } catch (throwable: Throwable) {
            Log.e("TaskerBridge", "Failed to run Tasker task", throwable)
            false
        }
    }
}
