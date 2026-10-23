package com.example.data

enum class MessageSender {
    USER,
    GIRLFRIEND,
    SYSTEM
}

enum class ActionType {
    NONE,
    WHATSAPP_OPEN,
    WHATSAPP_SEND,
    INSTAGRAM_OPEN,
    INSTAGRAM_STORY,
    PHONE_CALL,
    SEND_SMS,
    SET_ALARM,
    SET_TIMER,
    OPEN_YOUTUBE,
    OPEN_APP,
    WEB_SEARCH,
    CHECK_BATTERY,
    CHECK_TIME,
    TOGGLE_FLASHLIGHT,
    INSTALL_APP
}

data class ServerControlConfig(
    val maintenanceMode: Boolean = false,
    val serverActive: Boolean = true,
    val maintenanceMessage: String = "Server maintenance chal raha hai! Admin ne temporary band kar rakha hai. Thodi der me restart ho jayega! 🛠️",
    val announcement: String = "",
    val adminEmail: String = "roysurjo27238@gmail.com",
    val lastUpdated: Long = System.currentTimeMillis()
)

data class CompanionAction(
    val type: ActionType,
    val title: String,
    val description: String,
    val target: String = "",
    val payload: String = "",
    val executed: Boolean = false
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val emotion: String = "❤️",
    val action: CompanionAction? = null
)

data class VoiceProfile(
    val pitch: Float = 1.25f, // 1.0 = normal, 1.25 = cute/sweet girlfriend
    val speed: Float = 1.0f,
    val personaName: String = "Sweet & Caring",
    val description: String = "Loving, warm, and cute voice tone",
    val languageCode: String = "en-US"
)

enum class GirlfriendMood(val label: String, val emoji: String, val tonePrompt: String) {
    LOVING("Loving", "🥰", "Deeply affectionate, warm, caring, sweet, says loving things"),
    PLAYFUL("Playful", "😜", "Teasing playfully, funny, energetic, smiling, cute"),
    CARING("Caring", "🥺", "Empathetic, asking about health, food, work, reassuring"),
    ROMANTIC("Romantic", "💖", "Poetic, passionate, heart-melting, expressing love")
}

data class CompanionProfile(
    val name: String = "Aria",
    val userNickname: String = "Babe",
    val mood: GirlfriendMood = GirlfriendMood.LOVING,
    val relationshipLevel: Int = 1,
    val affectionXp: Int = 120,
    val autoSpeak: Boolean = true,
    val voicePitch: Float = 1.25f,
    val voiceSpeed: Float = 1.0f,
    val voicePreset: String = "Sweet & Caring"
)
