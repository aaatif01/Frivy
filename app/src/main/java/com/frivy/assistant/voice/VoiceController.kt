package com.frivy.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceController(
    private val context: Context,
    private val onText: (String) -> Unit,
) : RecognitionListener {

    private val recognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context).also { speechRecognizer ->
            speechRecognizer.setRecognitionListener(this)
        }

    private val tts: TextToSpeech = TextToSpeech(context, ::onTextToSpeechInitialized)

    fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(intent)
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        val params = Bundle()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "frivy-response")
    }

    fun destroy() {
        recognizer.stopListening()
        recognizer.cancel()
        recognizer.destroy()
        tts.stop()
        tts.shutdown()
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() = Unit

    override fun onError(error: Int) = Unit

    override fun onResults(results: Bundle?) {
        val recognizedText: String? = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()

        if (!recognizedText.isNullOrBlank()) {
            onText(recognizedText)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun onTextToSpeechInitialized(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.getDefault())
        }
    }
}