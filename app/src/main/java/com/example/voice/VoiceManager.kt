package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isAssistantMode = MutableStateFlow(false)
    val isAssistantMode: StateFlow<Boolean> = _isAssistantMode.asStateFlow()

    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError: StateFlow<String?> = _speechError.asStateFlow()

    private var currentPitch: Float = 1.25f
    private var currentRate: Float = 1.0f
    private var isTtsReady = false

    var onSpeechRecognized: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _speechError.value = null
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _audioRmsLevel.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _audioRmsLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _audioRmsLevel.value = 0f

                        // In continuous hands-free assistant mode, automatically resume listening on timeouts
                        if (_isAssistantMode.value) {
                            if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                                error == SpeechRecognizer.ERROR_CLIENT
                            ) {
                                mainHandler.postDelayed({
                                    if (_isAssistantMode.value && !_isSpeaking.value) {
                                        startListening()
                                    }
                                }, 600)
                                return
                            }
                        }

                        val msg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
                            SpeechRecognizer.ERROR_NETWORK -> "Network issue with voice"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that, say again babe?"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
                            else -> "Voice recognition paused"
                        }
                        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            _speechError.value = msg
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _audioRmsLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0]
                            val lower = recognizedText.lowercase().trim()

                            // Voice commands to stop assistant
                            if (lower == "stop" || lower == "stop assistant" ||
                                lower == "band karo" || lower == "exit" ||
                                lower == "stop talking" || lower == "chup" || lower == "chup raho"
                            ) {
                                stopAssistantMode()
                            }
                            onSpeechRecognized?.invoke(recognizedText)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.language = Locale.getDefault()
            tts?.setPitch(currentPitch)
            tts?.setSpeechRate(currentRate)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false

                    // In continuous assistant mode, resume listening automatically after speaking!
                    if (_isAssistantMode.value) {
                        mainHandler.postDelayed({
                            if (_isAssistantMode.value && !_isSpeaking.value) {
                                startListening()
                            }
                        }, 500)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    if (_isAssistantMode.value) {
                        mainHandler.postDelayed({
                            if (_isAssistantMode.value) {
                                startListening()
                            }
                        }, 600)
                    }
                }
            })
        }
    }

    fun startListening() {
        stopSpeaking()
        mainHandler.post {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (_: Exception) {
                _speechError.value = "Voice recognition failed to start"
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
            _audioRmsLevel.value = 0f
        }
    }

    fun startAssistantMode() {
        _isAssistantMode.value = true
        mainHandler.removeCallbacksAndMessages(null)
        startListening()
    }

    fun stopAssistantMode() {
        _isAssistantMode.value = false
        mainHandler.removeCallbacksAndMessages(null)
        stopListening()
        stopSpeaking()
    }

    fun toggleAssistantMode() {
        if (_isAssistantMode.value) {
            stopAssistantMode()
        } else {
            startAssistantMode()
        }
    }

    fun speak(text: String, pitch: Float = currentPitch, rate: Float = currentRate) {
        if (!isTtsReady) return
        stopListening()
        currentPitch = pitch
        currentRate = rate
        tts?.setPitch(pitch)
        tts?.setSpeechRate(rate)

        // Clean emojis and markdown from text before TTS for smooth voice
        val cleanText = cleanTextForSpeech(text)
        val utteranceId = "Aria_${System.currentTimeMillis()}"
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        if (isTtsReady) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    fun updateVoiceSettings(pitch: Float, rate: Float) {
        currentPitch = pitch
        currentRate = rate
        if (isTtsReady) {
            tts?.setPitch(pitch)
            tts?.setSpeechRate(rate)
        }
    }

    fun destroy() {
        _isAssistantMode.value = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }

    private fun cleanTextForSpeech(raw: String): String {
        return raw
            .replace(Regex("[*#_~`]"), "")
            .replace(Regex("\\[ACTION:[^\\]]+\\]"), "")
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")
            .trim()
    }
}
