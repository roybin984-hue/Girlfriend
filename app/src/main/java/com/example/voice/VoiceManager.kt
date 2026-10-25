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
import android.speech.tts.Voice
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

    // Natural human pitch (1.02f) - eliminates artificial robotic sound
    private var currentPitch: Float = 1.02f
    private var currentRate: Float = 1.0f
    private var isTtsReady = false
    private var lastSpokenText: String = ""

    var onSpeechRecognized: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

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
                    // In continuous phone call mode, the microphone NEVER turns off!
                    if (!_isAssistantMode.value) {
                        _isListening.value = false
                    }
                    _audioRmsLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _audioRmsLevel.value = 0f

                    // IN CONTINUOUS CALL MODE: The microphone NEVER disconnects!
                    if (_isAssistantMode.value) {
                        _isListening.value = true

                        // If recognizer got busy or client error, recreate it to stay alive forever
                        if (error == SpeechRecognizer.ERROR_CLIENT ||
                            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                            error == SpeechRecognizer.ERROR_AUDIO
                        ) {
                            mainHandler.postDelayed({
                                if (_isAssistantMode.value) {
                                    recreateSpeechRecognizer()
                                    restartListeningInternal()
                                }
                            }, 120)
                        } else {
                            // Instant loop restart on timeout or no-match
                            mainHandler.postDelayed({
                                if (_isAssistantMode.value) {
                                    restartListeningInternal()
                                }
                            }, 50)
                        }
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
                    // Lock mic status to TRUE during call mode so user sees call is continuous
                    if (_isAssistantMode.value) {
                        _isListening.value = true
                    } else {
                        _isListening.value = false
                    }
                    _audioRmsLevel.value = 0f

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0].trim()
                        val lower = recognizedText.lowercase()

                        // Voice commands to hang up call
                        if (lower == "stop" || lower == "stop assistant" ||
                            lower == "band karo" || lower == "exit" ||
                            lower == "call cut" || lower == "call kato" ||
                            lower == "phone kato" || lower == "cut karo" ||
                            lower == "stop talking" || lower == "chup" || lower == "chup raho"
                        ) {
                            stopAssistantMode()
                            return
                        }

                        // Echo suppression: Ignore if the recognizer picked up the AI's own voice
                        if (_isSpeaking.value && isAudioEcho(recognizedText, lastSpokenText)) {
                            if (_isAssistantMode.value) {
                                restartListeningInternal()
                            }
                            return
                        }

                        // If user interrupts while AI is speaking, stop speaking immediately (Full-Duplex Barge-in)
                        if (_isSpeaking.value) {
                            stopSpeaking()
                        }

                        onSpeechRecognized?.invoke(recognizedText)
                    }

                    // In continuous call mode, seamlessly keep the mic listening non-stop!
                    if (_isAssistantMode.value) {
                        restartListeningInternal()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // If user starts speaking while AI is speaking, barge-in immediately
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty() && _isSpeaking.value) {
                        val partial = matches[0].trim()
                        if (partial.length > 3 && !isAudioEcho(partial, lastSpokenText)) {
                            stopSpeaking()
                        }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun recreateSpeechRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        initSpeechRecognizer()
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
            applyBestHumanVoice(Locale.getDefault())
            tts?.setPitch(currentPitch)
            tts?.setSpeechRate(currentRate)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    // Ensure continuous microphone stays seamlessly active
                    if (_isAssistantMode.value) {
                        mainHandler.postDelayed({
                            if (_isAssistantMode.value) {
                                restartListeningInternal()
                            }
                        }, 50)
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
                        }, 50)
                    }
                }
            })
        }
    }

    // Finds and applies the most natural, high-quality human-like female/neural voice
    private fun applyBestHumanVoice(locale: Locale) {
        try {
            val voices = tts?.voices ?: return
            val matching = voices.filter { it.locale.language.equals(locale.language, ignoreCase = true) }
            if (matching.isNotEmpty()) {
                val best = matching.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    val isFemale = voice.features?.contains("gender=female") == true ||
                            name.contains("female") || name.contains("-f-") || name.contains("woman") || name.contains("-f_")
                    val isHigh = voice.quality >= Voice.QUALITY_HIGH
                    isFemale && isHigh
                } ?: matching.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    name.contains("female") || name.contains("-f-") || name.contains("woman")
                } ?: matching.maxByOrNull { it.quality }

                best?.let {
                    tts?.voice = it
                }
            }
        } catch (_: Exception) {}
    }

    fun restartListeningInternal() {
        if (!_isAssistantMode.value) return
        mainHandler.post {
            val intent = createSpeechRecognizerIntent()
            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (_: Exception) {
                mainHandler.postDelayed({
                    if (_isAssistantMode.value) {
                        try {
                            speechRecognizer?.startListening(intent)
                            _isListening.value = true
                        } catch (_: Exception) {
                            recreateSpeechRecognizer()
                            try {
                                speechRecognizer?.startListening(intent)
                                _isListening.value = true
                            } catch (_: Exception) {}
                        }
                    }
                }, 80)
            }
        }
    }

    private fun createSpeechRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Universal multilingual recognition support
            putExtra(
                "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                arrayOf("hi-IN", "en-IN", "bn-IN", "mr-IN", "ta-IN", "te-IN", "gu-IN", "pa-IN", "ur-PK", "en-US")
            )
        }
    }

    fun startListening() {
        stopSpeaking()
        mainHandler.post {
            val intent = createSpeechRecognizerIntent()
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

        // In continuous call mode, do NOT cancel speech recognizer so the mic stays constantly active!
        if (!_isAssistantMode.value) {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            _isListening.value = false
        } else {
            _isListening.value = true
        }

        // Apply natural human pitch & rate (prevent artificial chipmunk/robotic voice)
        currentPitch = pitch.coerceIn(0.85f, 1.20f)
        currentRate = rate.coerceIn(0.80f, 1.25f)
        tts?.setPitch(currentPitch)
        tts?.setSpeechRate(currentRate)

        // Automatically boost device media volume so speech is loud and clear
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

        // Universal Multilingual Language & Voice Engine Detection
        val targetLocale = detectLanguageFromText(text)
        try {
            if (tts?.isLanguageAvailable(targetLocale) in listOf(
                    TextToSpeech.LANG_AVAILABLE,
                    TextToSpeech.LANG_COUNTRY_AVAILABLE,
                    TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
                )
            ) {
                tts?.language = targetLocale
                applyBestHumanVoice(targetLocale)
            } else {
                tts?.language = Locale.getDefault()
                applyBestHumanVoice(Locale.getDefault())
            }
        } catch (_: Exception) {}

        // Thoroughly clean text to eliminate stuttering ("atakna") and emoji lag
        val cleanText = cleanTextForSpeech(text)
        if (cleanText.isBlank()) return
        lastSpokenText = cleanText

        val utteranceId = "Aria_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeLevel.coerceIn(0.2f, 1.0f))
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun detectLanguageFromText(text: String): Locale {
        return when {
            // Hindi / Marathi / Nepali (Devanagari)
            text.any { it in '\u0900'..'\u097F' } -> Locale("hi", "IN")
            // Bengali
            text.any { it in '\u0980'..'\u09FF' } -> Locale("bn", "IN")
            // Tamil
            text.any { it in '\u0B80'..'\u0BFF' } -> Locale("ta", "IN")
            // Telugu
            text.any { it in '\u0C00'..'\u0C7F' } -> Locale("te", "IN")
            // Gujarati
            text.any { it in '\u0A80'..'\u0AFF' } -> Locale("gu", "IN")
            // Punjabi
            text.any { it in '\u0A00'..'\u0A7F' } -> Locale("pa", "IN")
            // Kannada
            text.any { it in '\u0C80'..'\u0CFF' } -> Locale("kn", "IN")
            // Malayalam
            text.any { it in '\u0D00'..'\u0D7F' } -> Locale("ml", "IN")
            // Urdu / Arabic
            text.any { it in '\u0600'..'\u06FF' } -> Locale("ur", "PK")
            // Japanese
            text.any { it in '\u3040'..'\u30FF' || it in '\u4E00'..'\u9FFF' } -> Locale.JAPANESE
            // Korean
            text.any { it in '\uAC00'..'\uD7AF' } -> Locale.KOREAN
            // Default to India English/Hindi for Hinglish or default locale
            else -> Locale("en", "IN")
        }
    }

    fun stopSpeaking() {
        if (isTtsReady) {
            try {
                tts?.stop()
            } catch (_: Exception) {}
        }
        _isSpeaking.value = false
    }

    fun updateVoiceSettings(pitch: Float, rate: Float, loudEnabled: Boolean = true, volumeLevel: Float = 1.0f) {
        currentPitch = pitch.coerceIn(0.85f, 1.20f)
        currentRate = rate.coerceIn(0.80f, 1.25f)
        isLoudVoiceBoostEnabled = loudEnabled
        userVolumeLevel = volumeLevel
        if (isTtsReady) {
            tts?.setPitch(currentPitch)
            tts?.setSpeechRate(currentRate)
            applyBestHumanVoice(tts?.voice?.locale ?: Locale.getDefault())
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

    // Removes symbols, stage directions, and ellipsis to give smooth human speech without stutter
    private fun cleanTextForSpeech(raw: String): String {
        return raw
            .replace(Regex("[*#_~`>]"), "")
            .replace(Regex("\\[ACTION:[^\\]]+\\]"), "")
            .replace(Regex("\\([^)]*\\)"), "") // Remove (smiles), (blushes), etc.
            .replace(Regex("\\[[^\\]]*\\]"), "")
            .replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}\\p{Co}]"), "") // Emojis and unassigned symbols
            .replace(Regex("\\.{2,}"), ", ") // Ellipsis -> natural breathing pause
            .replace(Regex("!{2,}"), "!")
            .replace(Regex("\\?{2,}"), "?")
            .replace(Regex("-{2,}"), " - ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isAudioEcho(recognized: String, spoken: String): Boolean {
        if (spoken.isBlank() || recognized.isBlank()) return false
        val recWords = recognized.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
        val spkWords = spoken.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
        if (recWords.isEmpty() || spkWords.isEmpty()) return false
        val common = recWords.count { spkWords.contains(it) }
        return common >= 3 || (common.toFloat() / recWords.size.toFloat() > 0.65f)
    }
}
