package com.shazeb.hinata.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class VoiceEngine(private val context: Context) {

    // Voice states
    enum class State {
        IDLE, LISTENING, PROCESSING, SPEAKING
    }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false

    var onSpeechResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        setupTTS()
        setupSTT()
    }

    // Setup Text To Speech (Edge TTS via Android TTS for now)
    private fun setupTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(0.95f)
                textToSpeech?.setPitch(1.1f)
            }
        }
    }

    // Setup Speech To Text
    private fun setupSTT() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = State.LISTENING
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotEmpty()) {
                        _recognizedText.value = text
                        _state.value = State.PROCESSING
                        onSpeechResult?.invoke(text)
                    } else {
                        _state.value = State.IDLE
                    }
                }

                override fun onError(error: Int) {
                    _state.value = State.IDLE
                    isListening = false
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    // Start listening for voice input
    fun startListening() {
        if (isListening) return
        isListening = true
        _state.value = State.LISTENING

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    // Stop listening
    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        _state.value = State.IDLE
    }

    // Speak text as Hinata
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        _state.value = State.SPEAKING

        // Clean text for better speech
        val cleanText = text
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .trim()

        textToSpeech?.speak(
            cleanText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "hinata_speech"
        )

        // Check when done speaking
        Thread {
            while (textToSpeech?.isSpeaking == true) {
                Thread.sleep(100)
            }
            _state.value = State.IDLE
            onDone?.invoke()
        }.start()
    }

    // Stop speaking immediately
    fun stopSpeaking() {
        textToSpeech?.stop()
        _state.value = State.IDLE
    }

    // Clean up resources
    fun destroy() {
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
}
