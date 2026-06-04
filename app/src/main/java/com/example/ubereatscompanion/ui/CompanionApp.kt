package com.example.ubereatscompanion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ubereatscompanion.data.CompanionRepository
import com.example.ubereatscompanion.data.OfferEntity
import com.example.ubereatscompanion.data.UserOfferActionEntity
import com.example.ubereatscompanion.model.Recommendation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionApp(
    repository: CompanionRepository,
    onStartLocation: () -> Unit,
    onStartOcr: () -> Unit,
    onStopOcr: () -> Unit,
    onImportCsv: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onStartOverlay: () -> Unit
) {
    val vm: CompanionViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CompanionViewModel(repository) as T
    })

    val weather by vm.weather.collectAsState()
    val location by vm.location.collectAsState()
    val liveOffer by vm.liveOffer.collectAsState()
    val decision by vm.lastDecision.collectAsState()
    val recentOffers by vm.recentOffers.collectAsState()
    val recentActions by vm.recentActions.collectAsState()
    var price by remember { mutableStateOf("6.50") }
    var distance by remember { mutableStateOf("4.2") }
    var minutes by remember { mutableStateOf("22") }
    var battery by remember { mutableStateOf("80") }

    Scaffold(topBar = { TopAppBar(title = { Text("Uber Eats Companion") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatusCard(
                    weatherText = "${weather.condition}, rain ${weather.rainMmPerHour} mm/h, wind ${weather.windKph} km/h",
                    locationText = location?.let { "%.5f, %.5f ± %.0fm".format(it.latitude, it.longitude, it.accuracyMeters ?: 0f) } ?: "Waiting for GPS",
                    onStartLocation = onStartLocation,
                    onStartOcr = onStartOcr,
                    onStopOcr = onStopOcr,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenAppSettings = onOpenAppSettings,
                    onImportCsv = onImportCsv,
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onStartOverlay = onStartOverlay
                )
            }
            liveOffer?.let {
                item { LiveOfferCard(it.source, it.rawText, it.decision?.recommendation?.name ?: "Parsed, no decision") }
            }
            item {
                ManualOfferCard(
                    price = price,
                    distance = distance,
                    minutes = minutes,
                    battery = battery,
                    onPrice = { price = it },
                    onDistance = { distance = it },
                    onMinutes = { minutes = it },
                    onBattery = { battery = it },
                    onEvaluate = {
                        vm.evaluateManualOffer(
                            price.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            distance.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            minutes.toIntOrNull() ?: 0,
                            battery.replace(',', '.').toDoubleOrNull() ?: 100.0
                        )
                    }
                )
            }
            item { WeatherModeCard(onDry = { vm.setRainMode(0.0) }, onRain = { vm.setRainMode(1.5) }, onHeavyRain = { vm.setRainMode(5.0) }) }
            decision?.let { item { DecisionCard(it.recommendation, it.score, it.euroPerKm, it.euroPerHour, it.batteryAfterTrip, it.reasons) } }
            item { MaxymoStyleFeaturesCard() }
            item { WaitingZonesCard() }
            item { Text("Recent saved offers", style = MaterialTheme.typography.titleLarge) }
            items(recentOffers.take(10)) { RecentOfferRow(it) }
            item { Text("Recent confirmed actions", style = MaterialTheme.typography.titleLarge) }
            items(recentActions.take(10)) { RecentActionRow(it) }
        }
    }
}

@Composable
private fun StatusCard(
    weatherText: String,
    locationText: String,
    onStartLocation: () -> Unit,
    onStartOcr: () -> Unit,
    onStopOcr: () -> Unit,
    onImportCsv: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onStartOverlay: () -> Unit
) {
    ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Live status", style = MaterialTheme.typography.titleLarge)
        Text("GPS: $locationText")
        Text("Weather: $weatherText")
        Text("Accessibility reads visible offer text when Android exposes it. OCR fallback needs screen-capture permission.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartLocation) { Text("Start GPS") }
            OutlinedButton(onClick = onOpenAccessibilitySettings) { Text("Accessibility") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartOcr) { Text("Start OCR") }
            OutlinedButton(onClick = onStopOcr) { Text("Stop OCR") }
            OutlinedButton(onClick = onOpenAppSettings) { Text("Permissions") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenOverlaySettings) { Text("Overlay") }
            OutlinedButton(onClick = onStartOverlay) { Text("Start overlay") }
            OutlinedButton(onClick = onOpenNotificationSettings) { Text("Notifications") }
        }
        OutlinedButton(onClick = onImportCsv, modifier = Modifier.fillMaxWidth()) { Text("Import delivery CSV") }
    } }
}

@Composable
private fun LiveOfferCard(source: String, rawText: String, recommendation: String) {
    ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Live offer detected via $source", style = MaterialTheme.typography.titleLarge)
        Text("Recommendation: $recommendation")
        Text(rawText.take(500))
    } }
}

@Composable
private fun ManualOfferCard(
    price: String, distance: String, minutes: String, battery: String,
    onPrice: (String) -> Unit, onDistance: (String) -> Unit, onMinutes: (String) -> Unit, onBattery: (String) -> Unit,
    onEvaluate: () -> Unit
) {
    ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Manual offer test", style = MaterialTheme.typography.titleLarge)
        NumberField("Offer price €", price, onPrice)
        NumberField("Distance km", distance, onDistance)
        NumberField("Estimated minutes", minutes, onMinutes)
        NumberField("Battery %", battery, onBattery)
        Button(onClick = onEvaluate, modifier = Modifier.align(Alignment.End)) { Text("Evaluate") }
    } }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun WeatherModeCard(onDry: () -> Unit, onRain: () -> Unit, onHeavyRain: () -> Unit) {
    ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Weather mode override", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDry) { Text("Dry") }
            OutlinedButton(onClick = onRain) { Text("Rain") }
            OutlinedButton(onClick = onHeavyRain) { Text("Heavy rain") }
        }
    } }
}

@Composable
private fun DecisionCard(recommendation: Recommendation, score: Double, euroPerKm: Double?, euroPerHour: Double?, batteryAfter: Double?, reasons: List<String>) {
    ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Recommendation: ${recommendation.name}", style = MaterialTheme.typography.headlineSmall)
        Text("Score: ${score.toInt()}/100")
        Text("€/km: ${euroPerKm?.let { "%.2f".format(it) } ?: "unknown"}")
        Text("€/hour estimate: ${euroPerHour?.let { "%.2f".format(it) } ?: "unknown"}")
        Text("Battery after trip: ${batteryAfter?.let { "%.0f%%".format(it) } ?: "unknown"}")
        reasons.forEach { Text("• $it") }
    } }
}

@Composable
private fun WaitingZonesCard() {
    ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AI waiting zones", style = MaterialTheme.typography.titleLarge)
        Text("Initial defaults until enough delivery history is imported:")
        Text("1. Turnhout center / Grote Markt")
        Text("2. Station area")
        Text("3. Dense restaurant clusters near your current GPS position")
    } }
}

@Composable
private fun RecentOfferRow(offer: OfferEntity) {
    ElevatedCard { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${offer.recommendation} · €${"%.2f".format(offer.price)} · ${offer.estimatedDistanceKm?.let { "%.1f km".format(it) } ?: "distance unknown"}")
        Text("Score ${offer.score.toInt()}/100")
    } }
}


@Composable
private fun MaxymoStyleFeaturesCard() {
    ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Implemented companion features", style = MaterialTheme.typography.titleLarge)
        Text("• Live Accessibility reader for Uber Driver / Uber Eats")
        Text("• Notification reader for supported delivery app offer alerts")
        Text("• OCR fallback with throttling and crash guards")
        Text("• Floating recommendation overlay")
        Text("• Voice recommendation alerts via Android Text-to-Speech")
        Text("• Store-specific filters: €/km, min payout, pickup distance, total time, stacked/multi-stop/order-and-pay rules")
        Text("• Shift tracking, history import, earnings and best-time analytics foundation")
        Text("• Tasker integration: offer-evaluated broadcasts, decision-specific broadcasts, start/stop shift commands, mode commands, voice last offer, and navigation command")
        Text("• Full-screen confirmation screen with I ACCEPTED / I DECLINED / MAYBE buttons")
        Text("• Confirmation actions are stored in the local database for later review")
        Text("Automation note: this workflow does not tap Accept/Decline inside Uber. It confirms and logs your explicit choice, then can open Uber Driver/Eats.")
    } }
}

@Composable
private fun RecentActionRow(action: UserOfferActionEntity) {
    ElevatedCard { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${action.action} · ${action.recommendation ?: "no recommendation"}")
        Text("${action.price?.let { "€%.2f".format(it) } ?: "price unknown"} · ${action.distanceKm?.let { "%.1f km".format(it) } ?: "distance unknown"}")
        if (!action.pickupName.isNullOrBlank()) Text("Pickup: ${action.pickupName}")
    } }
}
