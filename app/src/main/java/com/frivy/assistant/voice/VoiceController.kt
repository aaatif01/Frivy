package com.frivy.assistant.voice
import android.content.*
import android.speech.*
import android.speech.tts.TextToSpeech
import java.util.*
class VoiceController(private val context: Context, private val onText: (String) -> Unit) : RecognitionListener {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(this) }
    private val tts = TextToSpeech(context) { if (it == TextToSpeech.SUCCESS) tts.language = Locale.getDefault() }
    fun listen() { recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true) }) }
    fun speak(text: String) { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "frivy-response") }
    fun destroy() { recognizer.destroy(); tts.shutdown() }
    override fun onResults(results: Bundle?) { results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onText) }
    override fun onPartialResults(results: Bundle?) {}
    override fun onError(error: Int) {}
    override fun onReadyForSpeech(p: Bundle?) {}; override fun onBeginningOfSpeech() {}; override fun onRmsChanged(v: Float) {}; override fun onBufferReceived(b: ByteArray?) {}; override fun onEndOfSpeech() {}; override fun onEvent(t: Int, p: Bundle?) {}
}