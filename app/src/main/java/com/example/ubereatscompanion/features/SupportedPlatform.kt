package com.example.ubereatscompanion.features

enum class SupportedPlatform(val label: String, val packageHints: List<String>) {
    UBER_EATS("Uber Eats", listOf("com.ubercab.driver", "uber")),
    DOORDASH("DoorDash", listOf("doordash")),
    GRUBHUB("Grubhub", listOf("grubhub")),
    SPARK("Spark", listOf("spark")),
    VROMO("Vromo", listOf("vromo"));

    companion object {
        fun fromPackage(packageName: String?): SupportedPlatform? = values().firstOrNull { platform ->
            platform.packageHints.any { packageName?.contains(it, ignoreCase = true) == true }
        }
    }
}
