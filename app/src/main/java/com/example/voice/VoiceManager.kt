package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
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

    // Natural human conversational pitch (1.02f) - eliminates robotic/chipmunk sound
    private var currentPitch: Float = 1.02f
    private var currentRate: Float = 0.98f
    private var isTtsReady = false
    private var lastSpokenText: String = ""

    var onSpeechRecognized: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
        initSpeechRecognizer()
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        try {
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
                        // In continuous live call mode, the microphone NEVER turns off
                        if (!_isAssistantMode.value) {
                            _isListening.value = false
                        }
                        _audioRmsLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _audioRmsLevel.value = 0f

                        // IN CONTINUOUS LIVE CALL MODE: The microphone NEVER disconnects!
                        if (_isAssistantMode.value) {
                            _isListening.value = true

                            // If AI is currently speaking, do not restart listening yet (wait for onDone)
                            if (_isSpeaking.value) return

                            // If recognizer got busy, client error, or audio error, reinitialize with safe backoff
                            if (error == SpeechRecognizer.ERROR_CLIENT ||
                                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                                error == SpeechRecognizer.ERROR_AUDIO
                            ) {
                                mainHandler.postDelayed({
                                    if (_isAssistantMode.value && !_isSpeaking.value) {
                                        recreateSpeechRecognizer()
                                        restartListeningInternal()
                                    }
                                }, 250)
                            } else {
                                // Natural pause, silence timeout (ERROR_SPEECH_TIMEOUT), or no-match: seamless loop
                                mainHandler.postDelayed({
                                    if (_isAssistantMode.value && !_isSpeaking.value) {
                                        restartListeningInternal()
                                    }
                                }, 150)
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
                        // Keep call status active
                        if (_isAssistantMode.value) {
                            _isListening.value = true
                        } else {
                            _isListening.value = false
                        }
                        _audioRmsLevel.value = 0f

                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0].trim()
                            if (recognizedText.isNotBlank()) {
                                val lower = recognizedText.lowercase()

                                // Voice commands to hang up call
                                if (lower == "stop" || lower == "stop assistant" ||
                                    lower == "band karo" || lower == "exit" ||
                                    lower == "call cut" || lower == "call kato" ||
                                    lower == "phone kato" || lower == "cut karo" ||
                                    lower == "cut" || lower == "chup" || lower == "chup raho"
                                ) {
                                    stopAssistantMode()
                                    return
                                }

                                // Echo suppression: Ignore if the recognizer heard the AI's own last words
                                if (_isSpeaking.value && isAudioEcho(recognizedText, lastSpokenText)) {
                                    return
                                }

                                // If user interrupts while AI is speaking, stop speaking immediately (Full-Duplex Barge-in)
                                if (_isSpeaking.value) {
                                    stopSpeaking()
                                }

                                // Forward recognized text to girlfriend AI
                                onSpeechRecognized?.invoke(recognizedText)
                                // Do not start listening right away while AI prepares response;
                                // listening will resume seamlessly as soon as AI finishes speaking (onDone).
                                return
                            }
                        }

                        // If empty results in continuous call mode, keep listening without interruption
                        if (_isAssistantMode.value && !_isSpeaking.value) {
                            mainHandler.postDelayed({
                                if (_isAssistantMode.value && !_isSpeaking.value) {
                                    restartListeningInternal()
                                }
                            }, 150)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
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
        } catch (_: Exception) {}
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

            val defaultLocale = Locale.getDefault()
            tts?.language = defaultLocale
            applyBestHumanVoice(defaultLocale)
            tts?.setPitch(currentPitch)
            tts?.setSpeechRate(currentRate)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    // Real phone call behavior: As soon as girlfriend finishes talking, mic immediately listens
                    if (_isAssistantMode.value) {
                        mainHandler.postDelayed({
                            if (_isAssistantMode.value) {
                                restartListeningInternal()
                            }
                        }, 100)
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
                        }, 100)
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
                    val isNeural = name.contains("network") || name.contains("neural") ||
                            name.contains("wavenet") || name.contains("natural")
                    val isFemale = voice.features?.contains("gender=female") == true ||
                            name.contains("female") || name.contains("-f-") ||
                            name.contains("woman") || name.contains("-f_") ||
                            name.contains("-d-") || name.contains("-c-")
                    val isHigh = voice.quality >= Voice.QUALITY_HIGH
                    (isNeural || isHigh) && isFemale
                } ?: matching.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    val isFemale = voice.features?.contains("gender=female") == true ||
                            name.contains("female") || name.contains("-f-") ||
                            name.contains("woman") || name.contains("-f_")
                    isFemale
                } ?: matching.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    name.contains("network") || name.contains("neural") || name.contains("natural")
                } ?: matching.maxByOrNull { it.quality }

                best?.let {
                    tts?.voice = it
                }
            }
        } catch (_: Exception) {}
    }

    fun restartListeningInternal() {
        if (!_isAssistantMode.value) return
        if (!hasAudioPermission()) return
        if (_isSpeaking.value) return

        mainHandler.post {
            val intent = createSpeechRecognizerIntent()
            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (_: Exception) {
                mainHandler.postDelayed({
                    if (_isAssistantMode.value && !_isSpeaking.value) {
                        recreateSpeechRecognizer()
                        try {
                            speechRecognizer?.startListening(intent)
                            _isListening.value = true
                        } catch (_: Exception) {}
                    }
                }, 150)
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
            // Universal multilingual recognition support for all Indian & global languages
            putExtra(
                "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                arrayOf("hi-IN", "en-IN", "bn-IN", "mr-IN", "ta-IN", "te-IN", "gu-IN", "pa-IN", "ur-PK", "en-US")
            )
        }
    }

    fun startListening() {
        if (!hasAudioPermission()) {
            _speechError.value = "Microphone permission required"
            return
        }
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

    // Speaks text with natural human voice, loud volume boost, and multilingual pronunciation
    fun speak(
        text: String,
        pitch: Float = currentPitch,
        rate: Float = currentRate,
        volumeLevel: Float = userVolumeLevel
    ) {
        if (!isTtsReady) return

        // In continuous call mode, UI shows call is active
        _isListening.value = true

        // Apply natural human pitch & rate (prevent artificial chipmunk/robotic voice)
        currentPitch = pitch.coerceIn(0.92f, 1.10f)
        currentRate = rate.coerceIn(0.85f, 1.15f)
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

        // Thoroughly clean text to eliminate stuttering ("atakna") and emoji lag using CodePoint sanitization
        val cleanText = sanitizeTextForNaturalVoice(text)
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
            // Hindi / Marathi / Nepali (Devanagari script)
            text.any { it in '\u0900'..'\u097F' } -> Locale.forLanguageTag("hi-IN")
            // Bengali script
            text.any { it in '\u0980'..'\u09FF' } -> Locale.forLanguageTag("bn-IN")
            // Tamil script
            text.any { it in '\u0B80'..'\u0BFF' } -> Locale.forLanguageTag("ta-IN")
            // Telugu script
            text.any { it in '\u0C00'..'\u0C7F' } -> Locale.forLanguageTag("te-IN")
            // Gujarati script
            text.any { it in '\u0A80'..'\u0AFF' } -> Locale.forLanguageTag("gu-IN")
            // Punjabi script
            text.any { it in '\u0A00'..'\u0A7F' } -> Locale.forLanguageTag("pa-IN")
            // Kannada script
            text.any { it in '\u0C80'..'\u0CFF' } -> Locale.forLanguageTag("kn-IN")
            // Malayalam script
            text.any { it in '\u0D00'..'\u0D7F' } -> Locale.forLanguageTag("ml-IN")
            // Urdu / Arabic script
            text.any { it in '\u0600'..'\u06FF' } -> Locale.forLanguageTag("ur-PK")
            // Japanese script
            text.any { it in '\u3040'..'\u30FF' || it in '\u4E00'..'\u9FFF' } -> Locale.JAPANESE
            // Korean script
            text.any { it in '\uAC00'..'\uD7AF' } -> Locale.KOREAN
            // Default to Indian English for Hinglish (Latin-script Hindi / English)
            else -> Locale.forLanguageTag("en-IN")
        }
    }

    fun stopSpeaking() {
        if (isTtsReady) {
            try {
                tts?.stop()
            } catch (_: Exception) {}
        }
        _isSpeaking.value = false
        if (_isAssistantMode.value) {
            mainHandler.postDelayed({
                if (_isAssistantMode.value) {
                    restartListeningInternal()
                }
            }, 100)
        }
    }

    fun updateVoiceSettings(pitch: Float, rate: Float, loudEnabled: Boolean = true, volumeLevel: Float = 1.0f) {
        currentPitch = pitch.coerceIn(0.92f, 1.10f)
        currentRate = rate.coerceIn(0.85f, 1.15f)
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

    // CodePoint-based Unicode text sanitizer: Removes symbols, emojis, markdown, and converts pauses cleanly
    // Eliminates TTS crashes, freezes, and speech stutter ("atakna bilkul band")
    private fun sanitizeTextForNaturalVoice(input: String): String {
        // 1. Remove action tags like [ACTION:WHATSAPP_OPEN]
        var text = input.replace(Regex("\\[ACTION:[^\\]]+\\]"), "")
        // 2. Remove stage directions like (smiles), (blushes), *whispers*
        text = text.replace(Regex("\\([^)]*\\)"), "")
        text = text.replace(Regex("\\[[^\\]]*\\]"), "")
        text = text.replace(Regex("\\*+[^*]+\\*+"), "")
        // 3. Remove all markdown characters
        text = text.replace(Regex("[*#_~`>|\"']"), "")

        // 4. Safe Unicode emoji and symbol removal using CodePoints without breaking surrogate pairs
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val charCount = Character.charCount(cp)
            val type = Character.getType(cp)

            val isEmojiOrSymbol = type == Character.SURROGATE.toInt() ||
                    type == Character.OTHER_SYMBOL.toInt() ||
                    type == Character.MODIFIER_SYMBOL.toInt() ||
                    (cp in 0x1F000..0x1FAFF) ||
                    (cp in 0x2600..0x27BF) ||
                    (cp in 0xFE00..0xFE0F) ||
                    (cp in 0x200D..0x200F)

            if (!isEmojiOrSymbol) {
                sb.appendCodePoint(cp)
            }
            i += charCount
        }
        text = sb.toString()

        // 5. Replace multiple punctuation with natural breathing pauses (comma)
        text = text.replace(Regex("\\.{2,}"), ", ")
        text = text.replace(Regex("-{2,}"), ", ")
        text = text.replace(Regex("!{2,}"), "! ")
        text = text.replace(Regex("\\?{2,}"), "? ")
        text = text.replace(Regex("[,;:]+"), ",")
        // 6. Clean extra whitespaces
        return text.replace(Regex("\\s+"), " ").trim()
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
