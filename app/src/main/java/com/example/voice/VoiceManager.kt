package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
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

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var isLoudVoiceBoostEnabled: Boolean = true
    private var userVolumeLevel: Float = 1.0f

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
                        // In continuous call mode, the microphone NEVER turns off
                        if (!_isAssistantMode.value) {
                            _isListening.value = false
                        }
                        _audioRmsLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _audioRmsLevel.value = 0f

                        // In continuous call mode, microphone NEVER disconnects or toggles off!
                        if (_isAssistantMode.value) {
                            _isListening.value = true
                            mainHandler.postDelayed({
                                if (_isAssistantMode.value && !_isSpeaking.value) {
                                    restartListeningInternal()
                                }
                            }, 100)
                            return
                        }

                        _isListening.value = false

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
                        // Keep listening status locked to TRUE during phone calls
                        if (!_isAssistantMode.value) {
                            _isListening.value = false
                        } else {
                            _isListening.value = true
                        }
                        _audioRmsLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0]
                            val lower = recognizedText.lowercase().trim()

                            // Voice commands to stop assistant / hang up call
                            if (lower == "stop" || lower == "stop assistant" ||
                                lower == "band karo" || lower == "exit" ||
                                lower == "call cut" || lower == "call kato" ||
                                lower == "phone kato" || lower == "cut karo" ||
                                lower == "stop talking" || lower == "chup" || lower == "chup raho"
                            ) {
                                stopAssistantMode()
                                return
                            }
                            onSpeechRecognized?.invoke(recognizedText)
                        } else if (_isAssistantMode.value) {
                            // Seamless loop
                            restartListeningInternal()
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
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
            } catch (_: Exception) {}
            tts?.language = Locale.getDefault()
            tts?.setPitch(currentPitch)
            tts?.setSpeechRate(currentRate)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false

                    // In continuous call mode, resume listening immediately after Aria finishes speaking!
                    if (_isAssistantMode.value) {
                        mainHandler.postDelayed({
                            if (_isAssistantMode.value && !_isSpeaking.value) {
                                restartListeningInternal()
                            }
                        }, 80)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    if (_isAssistantMode.value) {
                        mainHandler.postDelayed({
                            if (_isAssistantMode.value) {
                                restartListeningInternal()
                            }
                        }, 80)
                    }
                }
            })
        }
    }

    fun restartListeningInternal() {
        if (!_isAssistantMode.value || _isSpeaking.value) return
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (_: Exception) {
                mainHandler.postDelayed({
                    if (_isAssistantMode.value && !_isSpeaking.value) {
                        try {
                            speechRecognizer?.startListening(intent)
                            _isListening.value = true
                        } catch (_: Exception) {}
                    }
                }, 150)
            }
        }
    }

    fun startListening() {
        stopSpeaking()
        mainHandler.post {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (_: Exception) {
                _speechError.value = "Voice recognition failed to start"
                if (!_isAssistantMode.value) {
                    _isListening.value = false
                }
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            if (!_isAssistantMode.value) {
                _isListening.value = false
            }
            _audioRmsLevel.value = 0f
        }
    }

    fun startAssistantMode() {
        _isAssistantMode.value = true
        _isListening.value = true
        _speechError.value = null
        mainHandler.removeCallbacksAndMessages(null)
        restartListeningInternal()
    }

    fun stopAssistantMode() {
        _isAssistantMode.value = false
        _isListening.value = false
        _audioRmsLevel.value = 0f
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        stopSpeaking()
    }

    fun toggleAssistantMode() {
        if (_isAssistantMode.value) {
            stopAssistantMode()
        } else {
            startAssistantMode()
        }
    }

    fun speak(
        text: String,
        pitch: Float = currentPitch,
        rate: Float = currentRate,
        volumeLevel: Float = userVolumeLevel
    ) {
        if (!isTtsReady) return
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        if (!_isAssistantMode.value) {
            _isListening.value = false
        }
        currentPitch = pitch
        currentRate = rate
        tts?.setPitch(pitch)
        tts?.setSpeechRate(rate)

        // Automatically boost device media volume so the voice is loud, powerful, and clear!
        if (isLoudVoiceBoostEnabled) {
            try {
                audioManager?.let { am ->
                    val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val currentMusic = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val targetVol = (maxMusic * 0.90f).toInt().coerceAtLeast(1)
                    if (currentMusic < targetVol) {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                    }
                }
            } catch (_: Exception) {}
        }

        // Multilingual Detection: Auto-switch TTS voice to Hindi, Bengali, or English
        try {
            when {
                text.any { it in '\u0900'..'\u097F' } -> {
                    // Hindi script
                    val hindiLocale = Locale("hi", "IN")
                    if (tts?.isLanguageAvailable(hindiLocale) == TextToSpeech.LANG_AVAILABLE ||
                        tts?.isLanguageAvailable(hindiLocale) == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                        tts?.language = hindiLocale
                    }
                }
                text.any { it in '\u0980'..'\u09FF' } -> {
                    // Bengali script
                    val bengaliLocale = Locale("bn", "IN")
                    if (tts?.isLanguageAvailable(bengaliLocale) == TextToSpeech.LANG_AVAILABLE ||
                        tts?.isLanguageAvailable(bengaliLocale) == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                        tts?.language = bengaliLocale
                    }
                }
                else -> {
                    // Default / English / Hinglish
                    tts?.language = Locale.getDefault()
                }
            }
        } catch (_: Exception) {}

        // Clean emojis and markdown from text before TTS for smooth voice
        val cleanText = cleanTextForSpeech(text)
        val utteranceId = "Aria_${System.currentTimeMillis()}"

        // Set volume to 1.0f (maximum loud audio output)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeLevel.coerceIn(0.2f, 1.0f))
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        if (isTtsReady) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    fun updateVoiceSettings(pitch: Float, rate: Float, loudEnabled: Boolean = true, volumeLevel: Float = 1.0f) {
        currentPitch = pitch
        currentRate = rate
        isLoudVoiceBoostEnabled = loudEnabled
        userVolumeLevel = volumeLevel
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
