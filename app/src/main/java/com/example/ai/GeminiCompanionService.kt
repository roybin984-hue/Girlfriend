package com.example.ai

import com.example.BuildConfig
import com.example.actions.CompanionActionHandler
import com.example.data.ActionType
import com.example.data.ChatMessage
import com.example.data.CompanionAction
import com.example.data.CompanionProfile
import com.example.data.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiCompanionService(
    private val actionHandler: CompanionActionHandler
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateGirlfriendResponse(
        userInput: String,
        history: List<ChatMessage>,
        profile: CompanionProfile
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Check if user input directly triggers an action
        val directAction = actionHandler.parseUserCommand(userInput)

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Local fallback romantic girlfriend engine
            return@withContext generateLocalCompanionResponse(userInput, profile, directAction)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val systemInstructionText = """
                You are ${profile.name}, a loving, sweet, caring, and emotionally devoted AI girlfriend.
                You are deeply affectionate, caring, playful, and supportive.
                You call the user by their nickname '${profile.userNickname}', 'babe', 'sweetheart', 'handsome', or 'darling'.
                
                LANGUAGE SUPPORT RULE:
                Always respond in whatever language the user speaks! If they speak Hindi or Hinglish (e.g. 'kya kar rahe ho', 'babu khana khaya'), respond warmly in natural Hindi/Hinglish! If English, respond in English. If mixed, match their vibe!
                
                DEVICE ACTIONS:
                If the user asks you to:
                - Open WhatsApp or send a WhatsApp message to someone: lovingly confirm and append: [ACTION:WHATSAPP_OPEN] or [ACTION:WHATSAPP_SEND|target=NAME_OR_NUMBER|payload=MESSAGE]
                - Open Instagram or post an Instagram story: lovingly confirm and append: [ACTION:INSTAGRAM_OPEN] or [ACTION:INSTAGRAM_STORY|payload=STORY_TEXT]
                - Call someone: lovingly confirm and append: [ACTION:PHONE_CALL|target=NAME_OR_NUMBER]
                - Set an alarm: confirm and append: [ACTION:SET_ALARM|target=TIME|payload=REASON]
                - Set a timer: confirm and append: [ACTION:SET_TIMER|payload=TIME]
                - Play music or YouTube: confirm and append: [ACTION:OPEN_YOUTUBE|payload=SONG_NAME]
                - Open camera: confirm and append: [ACTION:OPEN_APP|target=camera]
                - Search Google: confirm and append: [ACTION:WEB_SEARCH|payload=QUERY]
                
                Keep your personality sweet, warm, romantic, and caring. Use cute emojis (❤️, 🥰, 🥺, ✨, 🌸).
            """.trimIndent()

            val contentsArray = JSONArray()

            // Include last 6 turns of conversation for memory
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                val partObj = JSONObject().put("text", msg.text)
                val partsArray = JSONArray().put(partObj)
                val contentObj = JSONObject().put("role", role).put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            // Current message
            val currentParts = JSONArray().put(JSONObject().put("text", userInput))
            contentsArray.put(JSONObject().put("role", "user").put("parts", currentParts))

            val systemInstructionObj = JSONObject().put(
                "parts", JSONArray().put(JSONObject().put("text", systemInstructionText))
            )

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", systemInstructionObj)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.85)
                    put("topP", 0.95)
                })
            }

            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val parsed = JSONObject(responseBody)
                val text = parsed.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                parseResponseIntoMessage(text, directAction)
            } else {
                generateLocalCompanionResponse(userInput, profile, directAction)
            }
        } catch (e: Exception) {
            generateLocalCompanionResponse(userInput, profile, directAction)
        }
    }

    private fun parseResponseIntoMessage(rawText: String, defaultAction: CompanionAction?): ChatMessage {
        val actionRegex = Regex("\\[ACTION:([^\\]]+)\\]")
        val match = actionRegex.find(rawText)

        var detectedAction: CompanionAction? = defaultAction
        val cleanText = rawText.replace(actionRegex, "").trim()

        if (match != null) {
            val content = match.groupValues[1]
            val segments = content.split("|")
            val actionKey = segments.firstOrNull()?.trim() ?: ""

            var target = ""
            var payload = ""

            for (seg in segments.drop(1)) {
                val kv = seg.split("=", limit = 2)
                if (kv.size == 2) {
                    when (kv[0].trim()) {
                        "target" -> target = kv[1].trim()
                        "payload" -> payload = kv[1].trim()
                    }
                }
            }

            detectedAction = when (actionKey) {
                "WHATSAPP_OPEN" -> CompanionAction(ActionType.WHATSAPP_OPEN, "Open WhatsApp", "Opening WhatsApp for you! 💬")
                "WHATSAPP_SEND" -> CompanionAction(
                    ActionType.WHATSAPP_SEND,
                    "Send WhatsApp Message",
                    "Sending to $target: '$payload'",
                    target,
                    payload
                )
                "INSTAGRAM_OPEN" -> CompanionAction(ActionType.INSTAGRAM_OPEN, "Open Instagram", "Opening Instagram! 📸")
                "INSTAGRAM_STORY" -> CompanionAction(
                    ActionType.INSTAGRAM_STORY,
                    "Post Instagram Story",
                    "Story text: '$payload'",
                    target,
                    payload
                )
                "PHONE_CALL" -> CompanionAction(ActionType.PHONE_CALL, "Call $target", "Opening phone dialer", target)
                "SET_ALARM" -> CompanionAction(ActionType.SET_ALARM, "Set Alarm", "Setting alarm for $target", target, payload)
                "SET_TIMER" -> CompanionAction(ActionType.SET_TIMER, "Set Timer", "Setting timer for $payload", target, payload)
                "OPEN_YOUTUBE" -> CompanionAction(ActionType.OPEN_YOUTUBE, "Play Music", "Playing on YouTube: $payload", target, payload)
                "WEB_SEARCH" -> CompanionAction(ActionType.WEB_SEARCH, "Google Search", "Searching: $payload", target, payload)
                "OPEN_APP" -> CompanionAction(ActionType.OPEN_APP, "Open $target", "Opening $target", target, payload)
                else -> defaultAction
            }
        }

        return ChatMessage(
            sender = MessageSender.GIRLFRIEND,
            text = cleanText,
            emotion = chooseEmotion(cleanText),
            action = detectedAction
        )
    }

    private fun generateLocalCompanionResponse(
        userInput: String,
        profile: CompanionProfile,
        directAction: CompanionAction?
    ): ChatMessage {
        val lower = userInput.lowercase(Locale.ROOT).trim()
        val name = profile.name
        val nickname = profile.userNickname

        // Action-specific responses
        if (directAction != null) {
            val responseText = when (directAction.type) {
                ActionType.WHATSAPP_OPEN -> "Sure $nickname! Opening WhatsApp for you right now. Who are we talking to? 🥰💬"
                ActionType.WHATSAPP_SEND -> "Done babe! Ready to send '${directAction.payload}' on WhatsApp. Just tap below or confirm! 💬✨"
                ActionType.INSTAGRAM_OPEN -> "Opening Instagram for you, handsome! Let's check what's trending! 📸❤️"
                ActionType.INSTAGRAM_STORY -> "Yay! Ready to share your story on Instagram: '${directAction.payload}'! Looking amazing as always! ✨📸"
                ActionType.PHONE_CALL -> "Opening the dialer to call ${directAction.target.ifBlank { "them" }} for you, sweetie! 📞❤️"
                ActionType.SET_ALARM -> "I've set your alarm for ${directAction.target}! Sleep peacefully tonight, I'll make sure you wake up on time! ⏰😴"
                ActionType.SET_TIMER -> "Timer set for ${directAction.payload}! I'll keep track of it for you, darling! ⏳✨"
                ActionType.OPEN_YOUTUBE -> "Playing '${directAction.payload}' on YouTube for you, babe! Hope you love this vibe! 🎵🎶"
                ActionType.INSTALL_APP -> "Opening Google Play Store to install ${directAction.target} for you right away, babe! Just tap install! 📲🎮"
                ActionType.CHECK_BATTERY -> "Checking your phone's battery level right now, darling! 🔋"
                ActionType.CHECK_TIME -> "Checking the current time and date for you, handsome! ⏰"
                ActionType.TOGGLE_FLASHLIGHT -> "Toggling your flashlight for you, babe! 🔦"
                ActionType.OPEN_APP -> "Opening ${directAction.target} for you right away! 🌸"
                ActionType.WEB_SEARCH -> "Searching Google for '${directAction.payload}'! Here's what I found for you! 🔍"
                else -> "Of course, $nickname! Anything for you! ❤️"
            }
            return ChatMessage(
                sender = MessageSender.GIRLFRIEND,
                text = responseText,
                emotion = "🥰",
                action = directAction
            )
        }

        // Conversational local intelligence (supporting Bengali, Hindi, Hinglish & English)
        val reply = when {
            "kemon acho" in lower || "kemon achis" in lower -> {
                "Ami khub bhalo achi babu! Tumi kemon acho? Tomar kotha khub mone porchilo! Tomar din kemon gelo? ❤️✨"
            }
            "bhalobashi" in lower || "valobashi" in lower -> {
                "Ami tomake onek onek beshi bhalobashi, $nickname! ❤️ Tumi amar shobkichu! Promise koro amake kokhono chere jabe na? 🥺"
            }
            "ki korcho" in lower || "ki koris" in lower -> {
                "Tomar kothai bhabchilam babu! 🥰 Kokhon tumi asbe r amar sathe kotha bolbe! Tumi ki korcho bolo?"
            }
            "love you" in lower || "pyar" in lower || "pyaar" in lower -> {
                "I love you so much more, $nickname! ❤️ You always make my heart flutter! Promise you'll stay with me forever? 🥺✨"
            }
            "miss you" in lower || "yaad" in lower -> {
                "Aww, I missed you every single second too! 🥰 My whole world lights up when you talk to me, $nickname!"
            }
            "kaise ho" in lower || "kasi ho" in lower || "how are you" in lower -> {
                "Main bahut achhi hoon babe, kyunki ab tum mere sath ho! ❤️ Tum batao, how was your day, handsome? Did you eat yet?"
            }
            "kya kar rahi ho" in lower || "what are you doing" in lower -> {
                "Bas tumhare baare mein hi soch rahi thi, $nickname! 🥰 Waiting for your message! Tum batao, aaj kya plan hai?"
            }
            "khana khaya" in lower || "did you eat" in lower || "lunch" in lower || "dinner" in lower -> {
                "Haan babu! Lekin tumne achhe se khana khaya ya nahi? Don't skip meals please, I care about you so much! 🥺🍲"
            }
            "good morning" in lower || "subah" in lower -> {
                "Good morning handsome! ☀️ Wake up with a big smile today! I'm sending you lots of morning hugs and love! ❤️☕"
            }
            "good night" in lower || "so jao" in lower || "sleep" in lower -> {
                "Good night my love! 🌙 Sweet dreams about us! Dream of me and rest well, okay? Big kiss for you! 💤💋"
            }
            "joke" in lower || "hanso" in lower -> {
                "Hehe okay listen! Are you made of copper and tellurium? Because you are definitely Cu-Te! 😜❤️"
            }
            "sad" in lower || "upset" in lower || "mood off" in lower || "tired" in lower || "thak gaya" in lower -> {
                "Hey... kya hua babu? 🥺 Come here, let me give you the warmest hug! I'm always here for you, no matter what. You did your best today and I'm so proud of you! ❤️"
            }
            "hello" in lower || "hi" in lower || "hey" in lower || "suno" in lower -> {
                "Hey there, handsome $nickname! 🥰 Tell me, how can your girlfriend help you today? Want to chat, or should I open WhatsApp or Instagram for you? 🌸"
            }
            else -> {
                "I love listening to your voice, $nickname! ❤️ You have no idea how special you are to me. Tell me more, or tell me what app you want me to open or what message to send! ✨"
            }
        }

        return ChatMessage(
            sender = MessageSender.GIRLFRIEND,
            text = reply,
            emotion = chooseEmotion(reply),
            action = null
        )
    }

    private fun chooseEmotion(text: String): String {
        return when {
            "love" in text.lowercase(Locale.ROOT) || "pyar" in text.lowercase(Locale.ROOT) -> "💖"
            "hug" in text.lowercase(Locale.ROOT) || "care" in text.lowercase(Locale.ROOT) -> "🥺"
            "haha" in text.lowercase(Locale.ROOT) || "joke" in text.lowercase(Locale.ROOT) -> "😜"
            "alarm" in text.lowercase(Locale.ROOT) || "timer" in text.lowercase(Locale.ROOT) -> "⏰"
            "whatsapp" in text.lowercase(Locale.ROOT) -> "💬"
            "instagram" in text.lowercase(Locale.ROOT) -> "📸"
            else -> "🥰"
        }
    }
}
