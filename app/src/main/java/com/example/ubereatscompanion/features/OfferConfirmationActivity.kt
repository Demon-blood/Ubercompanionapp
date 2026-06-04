package com.example.ubereatscompanion.features

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ubereatscompanion.UberCompanionApp
import com.example.ubereatscompanion.services.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Full-screen human-confirmation screen for Tasker or app-driven workflows.
 *
 * This screen intentionally does not inject taps into Uber Eats or any other app.
 * It logs the user's explicit choice and can bring Uber Driver/Eats to the front.
 */
class OfferConfirmationActivity : ComponentActivity() {
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val live by AppState.lastOffer.collectAsState()
                    ConfirmationScreen(
                        source = live?.source ?: "Tasker",
                        recommendation = live?.decision?.recommendation?.name ?: "UNKNOWN",
                        score = live?.decision?.score,
                        price = live?.offer?.price,
                        distanceKm = live?.offer?.estimatedDistanceKm,
                        euroPerKm = live?.decision?.euroPerKm,
                        euroPerHour = live?.decision?.euroPerHour,
                        pickup = live?.offer?.pickupName ?: live?.offer?.pickupAddress,
                        dropoff = live?.offer?.dropoffAddress,
                        reasons = live?.decision?.reasons ?: emptyList(),
                        rawText = live?.rawText.orEmpty(),
                        onAccepted = { logAndOpen("USER_ACCEPT_CONFIRMED") },
                        onDeclined = { logAndOpen("USER_DECLINE_CONFIRMED") },
                        onMaybe = { logOnly("USER_MAYBE_CONFIRMED") },
                        onClose = { finish() }
                    )
                }
            }
        }
    }

    private fun logOnly(action: String) {
        ioScope.launch { (application as UberCompanionApp).repository.saveUserOfferAction(action) }
        finish()
    }

    private fun logAndOpen(action: String) {
        ioScope.launch { (application as UberCompanionApp).repository.saveUserOfferAction(action) }
        runCatching {
            packageManager.getLaunchIntentForPackage("com.ubercab.driver")?.let { launch ->
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
            }
        }
        finish()
    }
}

@Composable
private fun ConfirmationScreen(
    source: String,
    recommendation: String,
    score: Double?,
    price: Double?,
    distanceKm: Double?,
    euroPerKm: Double?,
    euroPerHour: Double?,
    pickup: String?,
    dropoff: String?,
    reasons: List<String>,
    rawText: String,
    onAccepted: () -> Unit,
    onDeclined: () -> Unit,
    onMaybe: () -> Unit,
    onClose: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Confirm offer decision", style = MaterialTheme.typography.headlineMedium)
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Source: $source")
                Text("Recommended: $recommendation", style = MaterialTheme.typography.headlineSmall)
                Text("Score: ${score?.let { it.toInt().toString() } ?: "unknown"}/100")
                Text("Offer: ${price?.let { "€%.2f".format(it) } ?: "unknown"}")
                Text("Distance: ${distanceKm?.let { "%.1f km".format(it) } ?: "unknown"}")
                Text("€/km: ${euroPerKm?.let { "%.2f".format(it) } ?: "unknown"}")
                Text("€/hour: ${euroPerHour?.let { "%.2f".format(it) } ?: "unknown"}")
                if (!pickup.isNullOrBlank()) Text("Pickup: $pickup")
                if (!dropoff.isNullOrBlank()) Text("Drop-off: $dropoff")
                reasons.take(4).forEach { Text("• $it") }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onAccepted, modifier = Modifier.weight(1f)) { Text("I ACCEPTED") }
            Button(onClick = onDeclined, modifier = Modifier.weight(1f)) { Text("I DECLINED") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onMaybe, modifier = Modifier.weight(1f)) { Text("MAYBE / SKIPPED") }
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("CLOSE") }
        }
        if (rawText.isNotBlank()) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Text(rawText.take(700), modifier = Modifier.padding(16.dp))
            }
        }
    }
}
