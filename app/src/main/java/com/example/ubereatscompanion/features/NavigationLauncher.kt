package com.example.ubereatscompanion.features

import android.content.Context
import android.content.Intent
import android.net.Uri

object NavigationLauncher {
    fun openGoogleMaps(context: Context, destination: String) {
        val uri = Uri.parse("google.navigation:q=" + Uri.encode(destination))
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(destination)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openWaze(context: Context, destination: String) {
        val uri = Uri.parse("waze://?q=${Uri.encode(destination)}&navigate=yes")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=${Uri.encode(destination)}&navigate=yes")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
