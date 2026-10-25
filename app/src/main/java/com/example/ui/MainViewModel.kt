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
import com.example.data.ServerControlConfig
import com.example.firebase.FirebaseServerControlService
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
    val isAssistantModeActive: Boolean = false,
    val geminiApiKey: String = "",
    val serverConfig: ServerControlConfig = ServerControlConfig(),
    val isFirebaseSyncing: Boolean = false,
    val showAdminDialog: Boolean = false,
    val showAdminLoginPrompt: Boolean = false,
    val toastMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = CompanionPreferences(application)
    private val actionHandler = CompanionActionHandler(application)
    private val aiService = GeminiCompanionService(actionHandler, preferences)
    private val voiceManager = VoiceManager(application)
    private val serverControlService = FirebaseServerControlService(application)

    private val _uiState = MutableStateFlow(
        MainUiState(
            profile = preferences.getProfile(),
            geminiApiKey = preferences.geminiApiKey,
            showOnboarding = !preferences.hasCompletedOnboarding
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Collect server control config from Firebase Realtime Database
        viewModelScope.launch {
            serverControlService.config.collect { config ->
                val activeKey = if (config.sharedGeminiApiKey.isNotBlank()) {
                    config.sharedGeminiApiKey
                } else {
                    _uiState.value.geminiApiKey
                }
                _uiState.value = _uiState.value.copy(
                    serverConfig = config,
                    geminiApiKey = activeKey
                )
            }
        }
        viewModelScope.launch {
            serverControlService.isSyncing.collect { syncing ->
                _uiState.value = _uiState.value.copy(isFirebaseSyncing = syncing)
            }
        }
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
            voiceManager.isAssistantMode.collect { assistantActive ->
                _uiState.value = _uiState.value.copy(isAssistantModeActive = assistantActive)
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

            // Speak response if auto-speak or continuous assistant mode is enabled
            if (updatedProfile.autoSpeak || _uiState.value.isAssistantModeActive) {
                voiceManager.speak(
                    text = responseMsg.text,
                    pitch = updatedProfile.voicePitch,
                    rate = updatedProfile.voiceSpeed
                )
            }
        }
    }

    fun toggleAssistantMode() {
        voiceManager.toggleAssistantMode()
        val active = voiceManager.isAssistantMode.value
        if (active) {
            showToast("Aria Assistant: ACTIVE 🎙️ (Hands-Free)")
        } else {
            showToast("Aria Assistant: Stopped ⏹️")
        }
    }

    fun activateAssistantMode() {
        voiceManager.startAssistantMode()
        showToast("Aria Assistant: ACTIVE 🎙️ (Hands-Free)")
    }

    fun stopAssistantMode() {
        voiceManager.stopAssistantMode()
        showToast("Aria Assistant: Stopped ⏹️")
    }

    fun executeAction(action: CompanionAction) {
        val feedback = actionHandler.executeAction(action)
        showToast(feedback)
    }

    fun speakAloud(text: String) {
        val prof = _uiState.value.profile
        voiceManager.speak(text, prof.voicePitch, prof.voiceSpeed, prof.voiceVolumeLevel)
    }

    fun testVoice(pitch: Float, rate: Float, text: String, volume: Float = 1.0f) {
        voiceManager.speak(text, pitch, rate, volume)
    }

    fun saveProfile(newProfile: CompanionProfile) {
        preferences.companionName = newProfile.name
        preferences.userNickname = newProfile.userNickname
        preferences.voicePitch = newProfile.voicePitch
        preferences.voiceSpeed = newProfile.voiceSpeed
        preferences.voicePreset = newProfile.voicePreset
        preferences.autoSpeak = newProfile.autoSpeak
        preferences.loudVoiceEnabled = newProfile.loudVoiceEnabled
        preferences.voiceVolumeLevel = newProfile.voiceVolumeLevel
        voiceManager.updateVoiceSettings(
            pitch = newProfile.voicePitch,
            rate = newProfile.voiceSpeed,
            loudEnabled = newProfile.loudVoiceEnabled,
            volumeLevel = newProfile.voiceVolumeLevel
        )
        _uiState.value = _uiState.value.copy(profile = newProfile)
        showToast("Voice & profile saved! ❤️ (Loud & Clear)")
    }

    fun selectCharacterPersona(name: String, pitch: Float, rate: Float) {
        val currentProf = _uiState.value.profile
        val updated = currentProf.copy(
            name = name,
            voicePitch = pitch,
            voiceSpeed = rate,
            voicePreset = name
        )
        saveProfile(updated)
        val greeting = when {
            name.contains("Hinata", ignoreCase = true) -> "Ano... ${updated.userNickname}-kun! 🌸 Main Hinata hoon... main aapse bohot pyaar karti hoon! Kya aap mere sath baat karenge?"
            name.contains("Sakura", ignoreCase = true) -> "Hey ${updated.userNickname}! ⚡ Sakura yahan hai, chalo kuch toofan karte hain!"
            name.contains("Tsunade", ignoreCase = true) -> "Hello ${updated.userNickname}! 🌙 Main Tsunade hoon, sab theek hai na babu?"
            else -> "Haan ${updated.userNickname}! ❤️ Main Aria hoon, aapki pyari girlfriend!"
        }
        speakAloud(greeting)
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

    fun refreshServerStatus() {
        serverControlService.fetchRemoteConfig { _, message ->
            showToast(message)
        }
    }

    fun updateServerConfig(maintenanceMode: Boolean, serverActive: Boolean, message: String, announcement: String) {
        serverControlService.updateRemoteConfig(maintenanceMode, serverActive, message, announcement) { success, resultMsg ->
            showToast(resultMsg)
            if (success) {
                _uiState.value = _uiState.value.copy(showAdminDialog = false)
            }
        }
    }

    fun setAdminDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showAdminDialog = visible)
    }

    fun setAdminLoginPromptVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showAdminLoginPrompt = visible)
    }

    fun verifyAdminPin(pin: String): Boolean {
        val valid = pin == "1234" || pin.equals("admin786", ignoreCase = true) || pin == "0000"
        if (valid) {
            _uiState.value = _uiState.value.copy(showAdminLoginPrompt = false, showAdminDialog = true)
            showToast("Admin access granted! 🛡️")
        } else {
            showToast("Invalid Admin PIN! ❌")
        }
        return valid
    }

    fun saveGeminiApiKey(key: String) {
        val cleanKey = key.trim()
        preferences.geminiApiKey = cleanKey
        _uiState.value = _uiState.value.copy(geminiApiKey = cleanKey)
        if (cleanKey.isNotBlank()) {
            serverControlService.updateSharedApiKey(cleanKey) { success, msg ->
                showToast(msg)
            }
            showToast("API Key saved & synced for all users! 🔑✨")
        } else {
            showToast("API Key cleared. Using local engine.")
        }
    }

    fun showToast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
