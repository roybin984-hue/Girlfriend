package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.CompanionActionHandler
import com.example.ai.GeminiCompanionService
import com.example.data.ChatMessage
import com.example.data.CompanionAction
import com.example.data.CompanionPreferences
import com.example.data.CompanionProfile
import com.example.data.MessageSender
import com.example.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val messages: List<ChatMessage> = emptyList(),
    val profile: CompanionProfile = CompanionProfile(),
    val isThinking: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val audioRms: Float = 0f,
    val inputText: String = "",
    val showVoiceSettings: Boolean = false,
    val showActionHub: Boolean = false,
    val showOnboarding: Boolean = false,
    val isScreenShareActive: Boolean = false,
    val toastMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = CompanionPreferences(application)
    private val actionHandler = CompanionActionHandler(application)
    private val aiService = GeminiCompanionService(actionHandler)
    private val voiceManager = VoiceManager(application)

    private val _uiState = MutableStateFlow(
        MainUiState(
            profile = preferences.getProfile(),
            showOnboarding = !preferences.hasCompletedOnboarding
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Collect voice states
        viewModelScope.launch {
            voiceManager.isListening.collect { listening ->
                _uiState.value = _uiState.value.copy(isListening = listening)
            }
        }
        viewModelScope.launch {
            voiceManager.isSpeaking.collect { speaking ->
                _uiState.value = _uiState.value.copy(isSpeaking = speaking)
            }
        }
        viewModelScope.launch {
            voiceManager.audioRmsLevel.collect { rms ->
                _uiState.value = _uiState.value.copy(audioRms = rms)
            }
        }

        // Voice recognition result handler
        voiceManager.onSpeechRecognized = { recognizedText ->
            if (recognizedText.isNotBlank()) {
                sendMessage(recognizedText)
            }
        }

        // Initial sweet message from girlfriend
        val welcomeMsg = ChatMessage(
            sender = MessageSender.GIRLFRIEND,
            text = "Hey ${preferences.userNickname}! ❤️ I was just waiting for you! What would you like to do today? You can talk to me with voice, tell me to open WhatsApp or Instagram, send messages, set an alarm, or just chat! How are you doing? 🌸",
            emotion = "🥰"
        )
        _uiState.value = _uiState.value.copy(messages = listOf(welcomeMsg))

        // Initial voice greetings if auto-speak is enabled
        if (preferences.autoSpeak) {
            voiceManager.speak(
                text = "Hey ${preferences.userNickname}! I was waiting for you! How are you doing today?",
                pitch = preferences.voicePitch,
                rate = preferences.voiceSpeed
            )
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun toggleVoiceListening() {
        if (_uiState.value.isListening) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening()
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = trimmed
        )

        val updatedMessages = _uiState.value.messages + userMsg
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            inputText = "",
            isThinking = true
        )

        // Award Affection XP
        val (newLvl, leveledUp) = preferences.addAffectionXp(25)
        val updatedProfile = preferences.getProfile()
        _uiState.value = _uiState.value.copy(profile = updatedProfile)

        if (leveledUp) {
            showToast("Relationship Level Up! Now Level $newLvl ❤️")
        }

        viewModelScope.launch {
            val responseMsg = aiService.generateGirlfriendResponse(
                userInput = trimmed,
                history = updatedMessages,
                profile = updatedProfile
            )

            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + responseMsg,
                isThinking = false
            )

            // Auto execute action if detected
            if (responseMsg.action != null) {
                executeAction(responseMsg.action)
            }

            // Speak response if auto-speak enabled
            if (updatedProfile.autoSpeak) {
                voiceManager.speak(
                    text = responseMsg.text,
                    pitch = updatedProfile.voicePitch,
                    rate = updatedProfile.voiceSpeed
                )
            }
        }
    }

    fun executeAction(action: CompanionAction) {
        val feedback = actionHandler.executeAction(action)
        showToast(feedback)
    }

    fun speakAloud(text: String) {
        val prof = _uiState.value.profile
        voiceManager.speak(text, prof.voicePitch, prof.voiceSpeed)
    }

    fun testVoice(pitch: Float, rate: Float, text: String) {
        voiceManager.speak(text, pitch, rate)
    }

    fun saveProfile(newProfile: CompanionProfile) {
        preferences.companionName = newProfile.name
        preferences.userNickname = newProfile.userNickname
        preferences.voicePitch = newProfile.voicePitch
        preferences.voiceSpeed = newProfile.voiceSpeed
        preferences.voicePreset = newProfile.voicePreset
        preferences.autoSpeak = newProfile.autoSpeak
        voiceManager.updateVoiceSettings(newProfile.voicePitch, newProfile.voiceSpeed)
        _uiState.value = _uiState.value.copy(profile = newProfile)
        showToast("Voice & profile saved! ❤️")
    }

    fun toggleAutoSpeak() {
        val current = _uiState.value.profile.autoSpeak
        val updated = !current
        preferences.autoSpeak = updated
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile.copy(autoSpeak = updated)
        )
        if (!updated) {
            voiceManager.stopSpeaking()
        }
        showToast(if (updated) "Voice speech enabled 🔊" else "Voice muted 🔇")
    }

    fun showVoiceSettingsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showVoiceSettings = show)
    }

    fun showActionHubDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showActionHub = show)
    }

    fun toggleScreenShare(active: Boolean) {
        _uiState.value = _uiState.value.copy(isScreenShareActive = active)
        val msg = if (active) {
            "Screen share is active, ${preferences.userNickname}! I can see your screen now. Open any game or app! 🎮🔴"
        } else {
            "Screen share paused, ${preferences.userNickname}."
        }
        showToast(if (active) "Screen Share: ACTIVE 🔴" else "Screen Share: OFF ⚪")
        speakAloud(msg)
    }

    fun completeOnboarding() {
        preferences.hasCompletedOnboarding = true
        _uiState.value = _uiState.value.copy(showOnboarding = false)
    }

    fun showToast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
