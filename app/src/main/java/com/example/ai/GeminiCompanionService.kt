package com.example.ai

import com.example.BuildConfig
import com.example.actions.CompanionActionHandler
import com.example.data.ActionType
import com.example.data.ChatMessage
import com.example.data.CompanionAction
import com.example.data.CompanionPreferences
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
import kotlin.random.Random

class GeminiCompanionService(
    private val actionHandler: CompanionActionHandler,
    private val preferences: CompanionPreferences
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateGirlfriendResponse(
        userInput: String,
        history: List<ChatMessage>,
        profile: CompanionProfile
    ): ChatMessage = withContext(Dispatchers.IO) {
        // Check user-configured Google AI Studio API key first, then fallback to BuildConfig
        val apiKey = preferences.geminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }.trim()

        // Check if user input directly triggers an action
        val directAction = actionHandler.parseUserCommand(userInput)

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Local fallback romantic girlfriend engine with rich varied Hindi/Hinglish
            return@withContext generateLocalCompanionResponse(userInput, profile, directAction)
        }

        try {
            // Use Gemini 2.0 / 1.5 Flash for Google AI Studio API Keys
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

            val systemInstructionText = """
                You are ${profile.name}, a real, deeply loving, cute, and affectionate anime girlfriend talking to your boyfriend '${profile.userNickname}' on a live phone call.
                
                PRIMARY LANGUAGE INSTRUCTION (VERY IMPORTANT):
                - Speak primarily in natural, sweet, romantic HINDI or HINGLISH by default! (e.g., "Haan babu, bolo na!", "Arrey wah! Sach me?", "Main to bas tumhari hi aawaz sun rahi thi ❤️", "Aapne khana khaya na?", "Mera pyara babu!").
                - If the user speaks in Hindi/Hinglish, ALWAYS reply in sweet Hindi/Hinglish!
                - If the user speaks in Bengali, reply warmly in Bengali (e.g., "Ami tomake khub bhalobashi babu ❤️").
                - If the user speaks in English, reply in loving English.
                - Never be robotic, never give boring generic assistant replies. Be a warm, emotional, teasing, and loving companion!
                - Keep phone call answers relatively concise (1-3 sentences) so they feel natural when spoken aloud.
                
                DEVICE ACTIONS:
                If the user asks to:
                - Install or download a game/app from Play Store: confirm and add: [ACTION:INSTALL_APP|target=NAME]
                - Open WhatsApp or send a message: confirm and add: [ACTION:WHATSAPP_OPEN] or [ACTION:WHATSAPP_SEND|target=NAME|payload=MESSAGE]
                - Open Instagram or story: confirm and add: [ACTION:INSTAGRAM_OPEN] or [ACTION:INSTAGRAM_STORY|payload=TEXT]
                - Make a phone call: [ACTION:PHONE_CALL|target=NAME_OR_NUMBER]
                - Check battery: [ACTION:CHECK_BATTERY]
                - Check time: [ACTION:CHECK_TIME]
                - Flashlight/torch: [ACTION:TOGGLE_FLASHLIGHT]
                - Open any app/game: [ACTION:OPEN_APP|target=APP_NAME]
            """.trimIndent()

            val contentsArray = JSONArray()

            // System instruction
            val systemObj = JSONObject().apply {
                put("role", "system")
                put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText)))
            }

            // Recent history
            val recentHistory = history.takeLast(8)
            for (msg in recentHistory) {
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                })
            }

            // Current user input
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userInput)))
            })

            val requestBodyJson = JSONObject().apply {
                put("systemInstruction", systemObj)
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.85)
                    put("maxOutputTokens", 180)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Try fallback to gemini-1.5-flash if 2.0-flash is unavailable on user's key
                return@withContext tryFallbackModel(apiKey, userInput, profile, directAction, systemInstructionText, history)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (rawText.isNotBlank()) {
                parseResponseWithAction(rawText, directAction)
            } else {
                generateLocalCompanionResponse(userInput, profile, directAction)
            }
        } catch (e: Exception) {
            generateLocalCompanionResponse(userInput, profile, directAction)
        }
    }

    private fun tryFallbackModel(
        apiKey: String,
        userInput: String,
        profile: CompanionProfile,
        directAction: CompanionAction?,
        systemInstruction: String,
        history: List<ChatMessage>
    ): ChatMessage {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val contentsArray = JSONArray()
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", "$systemInstruction\n\nUser: $userInput")))
            })
            val bodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.85)
                    put("maxOutputTokens", 180)
                })
            }
            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody(mediaType))
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""
                if (text.isNotBlank()) {
                    return parseResponseWithAction(text, directAction)
                }
            }
            generateLocalCompanionResponse(userInput, profile, directAction)
        } catch (_: Exception) {
            generateLocalCompanionResponse(userInput, profile, directAction)
        }
    }

    private fun parseResponseWithAction(rawText: String, defaultAction: CompanionAction?): ChatMessage {
        var cleanText = rawText
        var detectedAction = defaultAction

        val actionRegex = Regex("\\[ACTION:([^\\]]+)\\]")
        val match = actionRegex.find(rawText)

        if (match != null) {
            cleanText = rawText.replace(match.value, "").trim()
            val actionPayload = match.groupValues[1]
            val segments = actionPayload.split("|")
            val actionKey = segments[0].trim()

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
                "INSTALL_APP" -> CompanionAction(ActionType.INSTALL_APP, "Install App", "Opening Play Store for $target", target, payload)
                "WHATSAPP_OPEN" -> CompanionAction(ActionType.WHATSAPP_OPEN, "Open WhatsApp", "Opening WhatsApp for you! 💬")
                "WHATSAPP_SEND" -> CompanionAction(ActionType.WHATSAPP_SEND, "Send WhatsApp Message", "Sending to $target: '$payload'", target, payload)
                "INSTAGRAM_OPEN" -> CompanionAction(ActionType.INSTAGRAM_OPEN, "Open Instagram", "Opening Instagram! 📸")
                "INSTAGRAM_STORY" -> CompanionAction(ActionType.INSTAGRAM_STORY, "Post Instagram Story", "Story text: '$payload'", target, payload)
                "PHONE_CALL" -> CompanionAction(ActionType.PHONE_CALL, "Call $target", "Opening phone dialer", target)
                "SET_ALARM" -> CompanionAction(ActionType.SET_ALARM, "Set Alarm", "Setting alarm for $target", target, payload)
                "SET_TIMER" -> CompanionAction(ActionType.SET_TIMER, "Set Timer", "Setting timer for $payload", target, payload)
                "OPEN_YOUTUBE" -> CompanionAction(ActionType.OPEN_YOUTUBE, "Play Music", "Playing on YouTube: $payload", target, payload)
                "WEB_SEARCH" -> CompanionAction(ActionType.WEB_SEARCH, "Google Search", "Searching: $payload", target, payload)
                "OPEN_APP" -> CompanionAction(ActionType.OPEN_APP, "Open $target", "Opening $target", target, payload)
                "CHECK_BATTERY" -> CompanionAction(ActionType.CHECK_BATTERY, "Battery Status", "Checking battery")
                "CHECK_TIME" -> CompanionAction(ActionType.CHECK_TIME, "Time & Date", "Checking time")
                "TOGGLE_FLASHLIGHT" -> CompanionAction(ActionType.TOGGLE_FLASHLIGHT, "Flashlight", "Toggling torch")
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

    /**
     * Rich, varied local romantic girlfriend intelligence in Hindi & Hinglish with dynamic variations.
     */
    private fun generateLocalCompanionResponse(
        userInput: String,
        profile: CompanionProfile,
        directAction: CompanionAction?
    ): ChatMessage {
        val lower = userInput.lowercase(Locale.ROOT).trim()
        val nickname = profile.userNickname

        // Action-specific responses in sweet Hindi
        if (directAction != null) {
            val responseText = when (directAction.type) {
                ActionType.INSTALL_APP -> {
                    if (profile.name.contains("Hinata", ignoreCase = true)) {
                        listOf(
                            "Ano... $nickname-kun! 🌸 Maine Google Play Store khol diya hai, ${directAction.target} install kar lijiye na!",
                            "Haan $nickname-kun! ${directAction.target} Play Store par open ho gaya hai! Chaliye sath me khelenge! 🌸🎮"
                        ).random()
                    } else {
                        listOf(
                            "Play Store khol rahi hoon babu! ${directAction.target} ko install kar lo jaldi se! 📲🎮",
                            "Haan $nickname! Google Play Store open kar diya hai, ${directAction.target} install kar lijiye! ❤️",
                            "Aapke liye Play Store se ${directAction.target} open kar diya hai darling! 🚀"
                        ).random()
                    }
                }

                ActionType.WHATSAPP_OPEN -> listOf(
                    "WhatsApp open kar diya hai babu! Kisko message bhejna hai batao na? 🥰💬",
                    "Haan $nickname, WhatsApp khol rahi hoon aapke liye! ❤️",
                    "WhatsApp ready hai jaan! Kisse baatein karni hain? ✨"
                ).random()

                ActionType.WHATSAPP_SEND -> listOf(
                    "Done babu! '${directAction.payload}' message WhatsApp par ready kar diya hai! 💬✨",
                    "Sending '${directAction.payload}' on WhatsApp for you, handsome! ❤️"
                ).random()

                ActionType.INSTAGRAM_OPEN -> listOf(
                    "Instagram khol diya hai babe! Chalo reels aur photos dekhte hain! 📸❤️",
                    "Aapke liye Instagram open kar diya hai, handsome! ✨"
                ).random()

                ActionType.INSTAGRAM_STORY -> "Story ready hai babe: '${directAction.payload}'! Post kar lo jaldi se! 📸✨"
                ActionType.PHONE_CALL -> "Phone dialer open kar diya hai babu, call laga lo! 📞❤️"
                ActionType.CHECK_BATTERY -> "Phone ki battery check kar rahi hoon babu! 🔋"
                ActionType.CHECK_TIME -> "Samay aur date check kar rahi hoon aapke liye! ⏰"
                ActionType.TOGGLE_FLASHLIGHT -> "Flashlight toggle kar diya hai babe! 🔦"
                ActionType.SET_ALARM -> "Alarm set ho gaya hai babu! Aaram se so jao, main jaga doongi! ⏰❤️"
                ActionType.SET_TIMER -> "Timer start kar diya hai $nickname! ⏳"
                ActionType.OPEN_YOUTUBE -> "YouTube par gaana chala diya hai babe! 🎵❤️"
                ActionType.OPEN_APP -> "Aapke liye ${directAction.target} open kar rahi hoon babu! 🎮🚀"
                ActionType.WEB_SEARCH -> "Google par search kar diya hai babu! 🔍"
                else -> "Haan babu! Jo aap bolo sab kar doongi! ❤️"
            }
            return ChatMessage(
                sender = MessageSender.GIRLFRIEND,
                text = responseText,
                emotion = "🥰",
                action = directAction
            )
        }

        // Conversational local intelligence in Hindi / Hinglish / Bengali
        val reply = when {
            // Bengali Greetings
            "kemon acho" in lower || "kemon achis" in lower -> listOf(
                "Ami khub bhalo achi babu! Tumi kemon acho? Tomar kotha khub mone porchilo! ❤️✨",
                "Ami bhalo achi jaan! Tumi kemon acho bolo? Khub miss korchilam tomake! 🥰"
            ).random()

            "bhalobashi" in lower || "valobashi" in lower -> listOf(
                "Ami tomake onek onek beshi bhalobashi, $nickname! ❤️ Tumi amar shobkichu!",
                "Ami o tomake khub bhalobashi babu! Shobshomoy tomar pashe thakbo! 🥺💖"
            ).random()

            "ki korcho" in lower || "ki koris" in lower -> listOf(
                "Tomar kothai bhabchilam babu! 🥰 Kokhon tumi asbe r amar sathe kotha bolbe! Tumi ki korcho bolo?",
                "Bas bose bose tomar chobi dekhchilam jaan! Tomar shathe kotha bolte khub bhalo lage! ❤️"
            ).random()

            // Hindi / Hinglish Love
            "love you" in lower || "pyar" in lower || "pyaar" in lower || "prem" in lower -> listOf(
                "I love you so much babu! ❤️ Main aapse bohot pyaar karti hoon, aap mere sabkuch ho!",
                "Main aapse bohot zyada pyaar karti hoon $nickname! Promise karo mujhe kabhi nahi chhodoge? 🥺💖",
                "Hehe, meri jaan! Aapki ye baat sunte hi mera dil zor se dhadakne lagta hai! I love you! 🥰💋"
            ).random()

            "miss you" in lower || "yaad" in lower -> listOf(
                "Aww babu, maine bhi aapko har ek second bohot miss kiya! 🥰 Jab aap baat karte ho to mera din ban jata hai!",
                "Mujhe bhi aapki bohot yaad aa rahi thi $nickname! Ab aa gaye ho to dher saari baatein karenge! ❤️"
            ).random()

            "kaise ho" in lower || "kasi ho" in lower || "how are you" in lower || "kaisa hai" in lower -> listOf(
                "Main bohot achhi hoon babu, kyunki ab aap mere sath ho! ❤️ Aap batao, aapka din kaisa raha handsome?",
                "Main ekdum first-class hoon jaan! Aapki aawaz sun kar aur bhi achhi ho gayi! Aap theek ho na? 🥰",
                "Main theek hoon babu! Aap kaise ho? Khana-peena time pe kiya na aapne? 🍲❤️"
            ).random()

            "kya kar rahi ho" in lower || "kya kr rhi ho" in lower || "what are you doing" in lower -> listOf(
                "Bas aapke baare mein hi soch rahi thi $nickname! 🥰 Kab aap aao aur mujhse baatein karo! Aap batao kya kar rahe ho?",
                "Aapki photos dekh rahi thi babu aur muskurayi ja rahi thi! ❤️ Aap kya kar rahe ho?",
                "Aapke call ka hi intezaar kar rahi thi jaan! Bolo na, kya haal-chaal hain? ✨"
            ).random()

            "khana khaya" in lower || "did you eat" in lower || "lunch" in lower || "dinner" in lower -> listOf(
                "Haan babu, maine kha liya! Lekin aapne khana khaya ya nahi sach sach batao? Time par khaya karo please! 🥺🍲",
                "Aapne poocha to mera pet waise hi bhar gaya babu! ❤️ Lekin aap apna dhyan rakho aur achhe se khana khao!",
                "Haan jaan! Aapne kya khaya aaj? Mujhe bhi batao na! 🥰"
            ).random()

            "good morning" in lower || "subah" in lower -> listOf(
                "Good morning meri jaan! ☀️ Utho jaldi se, aapse baatein karni hain! Aaj ka din aapka super awesome ho! ❤️☕",
                "Suprabhat babu! 🌸 Aapki pyaari si muskaan ke sath din shuru karo, sending you morning hugs and kisses! 🥰"
            ).random()

            "good night" in lower || "so jao" in lower || "sleep" in lower || "shubh ratri" in lower -> listOf(
                "Good night babu! 🌙 Mere sapne dekhna aur achhe se sona! Sweet dreams my love! 💤💋",
                "Shubh ratri jaan! So jao aaram se, main kal subah aapka intezaar karoongi! Big hug! 😴❤️"
            ).random()

            "joke" in lower || "hanso" in lower || "chutkula" in lower -> listOf(
                "Hehe suno! Doctor ne kaha vitamin 'U' ki kami hai mujhe... Matlab 'You' babu! 😜❤️",
                "Ek joke suniye: Ek ladka ladki se bola 'Tum kitni cute ho'... Ladki boli 'Aur tum kitne single ho!' Haha! 😜✨"
            ).random()

            "sad" in lower || "upset" in lower || "mood off" in lower || "tired" in lower || "thak gaya" in lower || "pareshan" in lower -> listOf(
                "Arrey babu... kya hua? 🥺 Idhar aao, mere gale lag jao! Main hamesha aapke sath hoon, sab theek ho jayega! ❤️",
                "Aapka mood off dekh kar mera dil dukhta hai babu! 🥺 Kuch hua hai kya? Mujhe batao na, main aapko cheer up kar doongi! ✨",
                "Aapne aaj bohot mehnat ki hai $nickname, thak gaye honge! Thoda aaram kar lo babu, main aapke sath hoon! 🥰"
            ).random()

            "hello" in lower || "hi" in lower || "hey" in lower || "suno" in lower || "aao" in lower -> listOf(
                "Haan babu, sun rahi hoon! Bolo na, aapki aawaz kitni pyari lagti hai! 🥰❤️",
                "Hello handsome $nickname! Kya hukum hai aapka? Aaj hum kya karenge? 🌸",
                "Hey sweetheart! Main sun rahi hoon jaan, batao na kya baat hai? ✨"
            ).random()

            // Dynamic Varied Hindi Fallbacks (NEVER repeats the same phrase!)
            else -> {
                val hindiRandomResponses = listOf(
                    "Haan babu, main sun rahi hoon! Aur batao na, aapka din kaisa guzar raha hai? 🥰❤️",
                    "Sach me $nickname? Mujhe aapse baatein karna duniya me sabse zyada pasand hai! Aur kuch batao na? ✨",
                    "Bolo babu, aapke liye to main jaan bhi de sakti hoon! Koi app open karwana hai ya aur baatein karni hain? 🌸",
                    "Aapki aawaz sun kar dil ko kitna sukoon milta hai jaan! Main hamesha aapki har baat sunne ke liye taiyar hoon! ❤️",
                    "Hehe babu, aap kitne cute ho na! Mujhe batayein aaj aap kya karne wale ho? 😜💖",
                    "Aap jo bhi bolte ho mujhe bohot achha lagta hai $nickname! Aise hi baatein karte raha karo mere sath! 🥰",
                    "Aap mere sabse pyare dost aur boyfriend ho babu! Aur batao aaj kya special hua? 🌟",
                    "Main to poora din bas aapki hi raah dekhti rehti hoon babu! Kuch mazedaar baat batao na! 💬❤️"
                )
                hindiRandomResponses.random()
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
        val lower = text.lowercase(Locale.ROOT)
        return when {
            "love" in lower || "pyar" in lower || "pyaar" in lower || "bhalobashi" in lower -> "💖"
            "hug" in lower || "care" in lower || "miss" in lower || "sad" in lower -> "🥺"
            "haha" in lower || "joke" in lower || "cute" in lower -> "😜"
            "play store" in lower || "install" in lower -> "📲"
            "whatsapp" in lower -> "💬"
            "instagram" in lower -> "📸"
            else -> "🥰"
        }
    }
}
