package com.example.ubereatscompanion.features

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.ubereatscompanion.model.OfferDecision
import java.util.Locale

class VoiceAlertManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        tts?.language = Locale.US
    }

    fun speak(decision: OfferDecision) {
        if (!ready) return
        val phrase = "${decision.recommendation.name.lowercase().replaceFirstChar { it.uppercase() }}. Score ${decision.score.toInt()}. ${decision.euroPerKm?.let { "%.2f euros per kilometer.".format(it) }.orEmpty()} ${decision.reasons.firstOrNull().orEmpty()}"
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "offer-decision")
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tasker-command")
    }

    fun shutdown() { tts?.shutdown(); tts = null }
}
