package com.deliverycompanion.aio

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent { CompanionDashboard() }
    }
}

@Composable
fun CompanionDashboard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var trips by remember { mutableStateOf(LocalStore.loadTrips(context)) }
    var rules by remember { mutableStateOf(LocalStore.loadRules(context)) }
    var offer by remember { mutableStateOf(OfferInput()) }
    val decision = OfferEvaluator.evaluate(offer, rules)
    var marketSignals by remember { mutableStateOf(MarketSignals()) }

    LaunchedEffect(Unit) {
        marketSignals = MarketSignalService.loadTurnhoutSignals()
    }

    val totalExpenses = trips.sumOf { it.expenses }
    val totalProfit = trips.sumOf { it.profit }
    val totalKm = trips.sumOf { it.distanceKm }
    val totalOrders = trips.sumOf { it.orders }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF8FAFC)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HeaderCard(
                        onStartOverlay = {
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                context.startService(Intent(context, OverlayService::class.java))
                            }
                        }
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        StatBox("Profit", money(totalProfit), Modifier.weight(1f))
                        StatBox("Orders", totalOrders.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        StatBox("Distance", "${round1(totalKm)} km", Modifier.weight(1f))
                        StatBox("Expenses", money(totalExpenses), Modifier.weight(1f))
                    }
                }

                item {
                    OfferPanel(
                        offer = offer,
                        onOfferChange = { offer = it },
                        decision = decision,
                        onCaptureLocation = {
                            scope.launch {
                                val captured = LocationHelper.getCurrentLocation(context)
                                if (captured != null) {
                                    offer = offer.copy(
                                        offerLatitude = captured.latitude,
                                        offerLongitude = captured.longitude,
                                        offerCapturedAt = java.time.ZonedDateTime.now().toString(),
                                        sourceMethod = "manual_with_gps"
                                    )
                                }
                            }
                        },
                        onLogAccepted = {
                            val now = LocalTime.now()
                            val end = now.plusMinutes(offer.estimatedMinutes.toLong())
                            val trip = TripLog(
                                id = System.currentTimeMillis().toString(),
                                date = LocalDate.now().toString(),
                                startTime = now.toString().take(5),
                                endTime = end.toString().take(5),
                                city = offer.dropoffArea,
                                pickupDestination = offer.pickupDestination,
                                deliveryDestination = offer.deliveryDestination,
                                offerLatitude = offer.offerLatitude,
                                offerLongitude = offer.offerLongitude,
                                offerCapturedAt = offer.offerCapturedAt,
                                orders = if (offer.stackedOrder) 2 else 1,
                                distanceKm = offer.estimatedDistanceKm,
                                basePay = offer.estimatedPay,
                                notes = listOf(offer.restaurant, offer.pickupDestination, offer.deliveryDestination, offer.notes).filter { it.isNotBlank() }.joinToString(" · ")
                            )
                            trips = listOf(trip) + trips
                            LocalStore.saveTrips(context, trips)
                        }
                    )
                }

                item {
                    MarketSignalsPanel(signals = marketSignals)
                }

                item {
                    WaitingZonesPanel(trips = trips, signals = marketSignals)
                }

                item {
                    RulesPanel(
                        rules = rules,
                        onRulesChange = {
                            rules = it
                            LocalStore.saveRules(context, it)
                        }
                    )
                }

                item {
                    Text("Trip History", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                items(trips) { trip ->
                    TripCard(
                        trip = trip,
                        onDelete = {
                            trips = trips.filterNot { it.id == trip.id }
                            LocalStore.saveTrips(context, trips)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderCard(onStartOverlay: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Delivery Companion AIO", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "E-scooter delivery assistant: offer filters, net profit after electricity, battery impact, AI-style waiting zones, expenses, and trip logs.",
                color = Color(0xFF64748B)
            )
            Button(onClick = onStartOverlay, shape = RoundedCornerShape(16.dp)) {
                Text("Start Floating Overlay")
            }
        }
    }
}

@Composable
fun StatBox(title: String, value: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(20.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Color(0xFF64748B), fontSize = 13.sp)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OfferPanel(
    offer: OfferInput,
    onOfferChange: (OfferInput) -> Unit,
    decision: OfferDecision,
    onCaptureLocation: () -> Unit,
    onLogAccepted: () -> Unit
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Offer Filter", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DecimalInput("Pay €", offer.estimatedPay, { onOfferChange(offer.copy(estimatedPay = it)) }, Modifier.weight(1f))
                DecimalInput("Km", offer.estimatedDistanceKm, { onOfferChange(offer.copy(estimatedDistanceKm = it)) }, Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DecimalInput("Minutes", offer.estimatedMinutes, { onOfferChange(offer.copy(estimatedMinutes = it)) }, Modifier.weight(1f))
                DecimalInput("Wait", offer.pickupWaitMinutes, { onOfferChange(offer.copy(pickupWaitMinutes = it)) }, Modifier.weight(1f))
            }

            TextInput("Restaurant", offer.restaurant) { onOfferChange(offer.copy(restaurant = it)) }
            TextInput("Pickup destination", offer.pickupDestination) { onOfferChange(offer.copy(pickupDestination = it)) }
            TextInput("Delivery destination", offer.deliveryDestination) { onOfferChange(offer.copy(deliveryDestination = it, dropoffArea = it)) }
            TextInput("Drop-off area / waiting zone", offer.dropoffArea) { onOfferChange(offer.copy(dropoffArea = it)) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = offer.stackedOrder, onCheckedChange = { onOfferChange(offer.copy(stackedOrder = it)) })
                Spacer(Modifier.width(8.dp))
                Text("Stacked order")
            }

            LocationCaptureCard(offer = offer, onCaptureLocation = onCaptureLocation)

            DecisionCard(decision)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onOfferChange(OfferInput()) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Clear")
                }

                Button(
                    onClick = onLogAccepted,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Log Accepted")
                }
            }
        }
    }
}


@Composable
fun LocationCaptureCard(offer: OfferInput, onCaptureLocation: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Offer location snapshot", fontWeight = FontWeight.Bold)
            if (offer.offerLatitude != null && offer.offerLongitude != null) {
                Text(
                    "Saved: ${round1(offer.offerLatitude)} , ${round1(offer.offerLongitude)}",
                    color = Color(0xFF1E3A8A),
                    fontSize = 13.sp
                )
                if (offer.offerCapturedAt.isNotBlank()) {
                    Text("Captured: ${offer.offerCapturedAt.take(19)}", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            } else {
                Text("No GPS snapshot saved for this offer yet.", color = Color(0xFF64748B), fontSize = 13.sp)
            }
            OutlinedButton(onClick = onCaptureLocation, shape = RoundedCornerShape(14.dp)) {
                Text("Save Current Location")
            }
        }
    }
}


@Composable
fun DecisionCard(decision: OfferDecision) {
    val accept = decision.recommendation == "ACCEPT"
    val bg = if (accept) Color(0xFFDCFCE7) else Color(0xFFFFE4E6)
    val fg = if (accept) Color(0xFF166534) else Color(0xFF9F1239)

    Column(
        Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(decision.recommendation, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = fg)
        Text("Score ${decision.score}/100 · ${money(decision.payPerKm)}/km net · ${money(decision.hourlyRate)}/hour")
        Text("Net ${money(decision.estimatedNetPay)} · Electricity ${money(decision.estimatedElectricityCost)} · Battery ${round1(decision.estimatedBatteryPercentUsed)}%")
        val lines = if (decision.reasons.isNotEmpty()) decision.reasons else decision.positives
        lines.take(4).forEach { Text("• $it", fontSize = 13.sp) }
    }
}

@Composable
fun RulesPanel(rules: OfferRules, onRulesChange: (OfferRules) -> Unit) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Rules", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DecimalInput("Min pay", rules.minPay, { onRulesChange(rules.copy(minPay = it)) }, Modifier.weight(1f))
                DecimalInput("Min €/km", rules.minPayPerKm, { onRulesChange(rules.copy(minPayPerKm = it)) }, Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DecimalInput("Min €/hour", rules.minHourlyRate, { onRulesChange(rules.copy(minHourlyRate = it)) }, Modifier.weight(1f))
                DecimalInput("Max km", rules.maxDistanceKm, { onRulesChange(rules.copy(maxDistanceKm = it)) }, Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DecimalInput("Max min", rules.maxMinutes, { onRulesChange(rules.copy(maxMinutes = it)) }, Modifier.weight(1f))
                DecimalInput("Max wait", rules.maxPickupWait, { onRulesChange(rules.copy(maxPickupWait = it)) }, Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DecimalInput("€/kWh", rules.electricityPricePerKwh, { onRulesChange(rules.copy(electricityPricePerKwh = it)) }, Modifier.weight(1f))
                DecimalInput("kWh/100km", rules.scooterKwhPer100Km, { onRulesChange(rules.copy(scooterKwhPer100Km = it)) }, Modifier.weight(1f))
            }

            DecimalInput("Battery capacity kWh", rules.batteryCapacityKwh, { onRulesChange(rules.copy(batteryCapacityKwh = it)) }, Modifier.fillMaxWidth())

            TextInput("Blocked areas, comma separated", rules.blockedAreasCsv) {
                onRulesChange(rules.copy(blockedAreasCsv = it))
            }

            TextInput("Preferred areas, comma separated", rules.preferredAreasCsv) {
                onRulesChange(rules.copy(preferredAreasCsv = it))
            }
        }
    }
}



@Composable
fun MarketSignalsPanel(signals: MarketSignals) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Weather & Event Signals", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            if (signals.error != null) {
                Text("Could not load live signals: ${signals.error}", color = Color(0xFFB91C1C), fontSize = 13.sp)
                Text("The app still works offline using your local trip history.", color = Color(0xFF64748B), fontSize = 13.sp)
            } else {
                Text("Turnhout weather: ${signals.weather.summary}", fontWeight = FontWeight.Bold)
                Text("Scooter status: ${signals.weather.scooterRisk}", color = Color(0xFF64748B), fontSize = 13.sp)
                Text("Weather demand modifier: ${signals.weather.demandBoost}", color = Color(0xFF64748B), fontSize = 13.sp)

                if (signals.events.isEmpty()) {
                    Text("No upcoming Turnhout events loaded yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                } else {
                    Text("Upcoming local events", fontWeight = FontWeight.Bold)
                    signals.events.take(3).forEach {
                        Text("• ${it.title} · ${it.date}", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}


@Composable
fun WaitingZonesPanel(trips: List<TripLog>, signals: MarketSignals) {
    val zones = trips
        .filter { it.city.isNotBlank() }
        .groupBy { it.city.trim() }
        .map { entry ->
            val zoneTrips = entry.value
            val profit = zoneTrips.sumOf { it.profit }
            val orders = zoneTrips.sumOf { it.orders }.coerceAtLeast(1)
            val km = zoneTrips.sumOf { it.distanceKm }.coerceAtLeast(1.0)
            val eventBoost = signals.events.sumOf { it.demandBoost }.toDouble()
            val weatherBoost = signals.weather.demandBoost.toDouble()
            val score = (profit / orders) + (profit / km) + eventBoost + weatherBoost
            Triple(entry.key, score, zoneTrips.size)
        }
        .sortedByDescending { it.second }
        .take(5)

    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AI Waiting Zones", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Uses your logged delivery history to rank waiting zones by net profit per order and net profit per km. The more trips you log, the better the recommendations become.",
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )

            if (zones.isEmpty()) {
                Text("No zone data yet. Log deliveries with drop-off areas to generate recommendations.", color = Color(0xFF64748B))
            } else {
                zones.forEachIndexed { index, zone ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${index + 1}. ${zone.first}", fontWeight = FontWeight.Bold)
                            Text("${zone.third} logged trips", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                        Text("Score ${round1(zone.second)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
fun TripCard(trip: TripLog, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("${trip.date} · ${trip.platform}", fontWeight = FontWeight.Bold)
                Text("${trip.orders} orders · ${round1(trip.distanceKm)} km · ${trip.city}", color = Color(0xFF64748B), fontSize = 13.sp)
                if (trip.pickupDestination.isNotBlank()) Text("Pickup: ${trip.pickupDestination}", color = Color(0xFF64748B), fontSize = 13.sp)
                if (trip.deliveryDestination.isNotBlank()) Text("Delivery: ${trip.deliveryDestination}", color = Color(0xFF64748B), fontSize = 13.sp)
                if (trip.offerLatitude != null && trip.offerLongitude != null) Text("Offer GPS: ${round1(trip.offerLatitude)} , ${round1(trip.offerLongitude)}", color = Color(0xFF64748B), fontSize = 13.sp)
                if (trip.notes.isNotBlank()) Text(trip.notes, color = Color(0xFF64748B), fontSize = 13.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(money(trip.profit), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
fun DecimalInput(label: String, value: Double, onChange: (Double) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = if (value == 0.0) "" else value.toString(),
        onValueChange = { onChange(it.replace(",", ".").toDoubleOrNull() ?: 0.0) },
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    )
}

@Composable
fun TextInput(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}