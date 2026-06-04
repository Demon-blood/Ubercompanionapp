package com.example.ubereatscompanion.data

import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

object DeliveryCsvImporter {
    fun parse(input: InputStream): List<DeliveryEntity> {
        val lines = input.bufferedReader().readLines().filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()
        val headers = splitCsvLine(lines.first()).map { it.trim().lowercase(Locale.US) }
        return lines.drop(1).mapNotNull { row ->
            val cells = splitCsvLine(row)
            fun value(vararg names: String): String? {
                val idx = headers.indexOfFirst { header -> names.any { name -> header.contains(name) } }
                return if (idx in cells.indices) cells[idx].trim() else null
            }
            val earnings = value("earning", "pay", "fare", "amount", "price")?.moneyToDouble() ?: return@mapNotNull null
            val distance = value("distance", "km", "kilometer", "kilometre")?.numberToDouble() ?: 0.0
            val minutes = value("duration", "minute", "time")?.numberToDouble()?.toInt() ?: 0
            val dateText = value("date", "completed", "time", "timestamp")
            DeliveryEntity(
                completedAt = parseDateMillis(dateText) ?: System.currentTimeMillis(),
                earnings = earnings,
                distanceKm = distance,
                durationMinutes = minutes,
                pickupName = value("restaurant", "merchant", "store", "pickup"),
                pickupZone = value("pickup zone", "pickup area"),
                dropoffZone = value("drop", "delivery zone", "dropoff"),
                weatherCondition = null,
                batteryUsedPercent = null
            )
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { current.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { result += current.toString(); current.clear() }
                else -> current.append(c)
            }
            i++
        }
        result += current.toString()
        return result
    }

    private fun String.moneyToDouble(): Double? = replace("€", "").replace("$", "").replace(',', '.').filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()
    private fun String.numberToDouble(): Double? = replace(',', '.').filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()

    private fun parseDateMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val formats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "dd/MM/yyyy HH:mm", "dd/MM/yyyy", "MM/dd/yyyy")
        return formats.firstNotNullOfOrNull { pattern ->
            runCatching { SimpleDateFormat(pattern, Locale.US).parse(value)?.time }.getOrNull()
        }
    }
}
