package com.example.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.widget.Toast
import com.example.data.ActionType
import com.example.data.CompanionAction
import java.net.URLEncoder
import java.util.Locale

class CompanionActionHandler(private val context: Context) {

    /**
     * Executes the requested action and returns a user-friendly feedback message.
     */
    fun executeAction(action: CompanionAction): String {
        return try {
            when (action.type) {
                ActionType.WHATSAPP_OPEN -> openWhatsApp()
                ActionType.WHATSAPP_SEND -> sendWhatsAppMessage(action.target, action.payload)
                ActionType.INSTAGRAM_OPEN -> openInstagram()
                ActionType.INSTAGRAM_STORY -> shareInstagramStory(action.payload)
                ActionType.PHONE_CALL -> makePhoneCall(action.target)
                ActionType.SEND_SMS -> sendSms(action.target, action.payload)
                ActionType.SET_ALARM -> setAlarm(action.target, action.payload)
                ActionType.SET_TIMER -> setTimer(action.payload)
                ActionType.OPEN_YOUTUBE -> openYouTube(action.payload)
                ActionType.WEB_SEARCH -> searchWeb(action.payload)
                ActionType.OPEN_APP -> openSpecificApp(action.target)
                ActionType.NONE -> "No action required."
            }
        } catch (e: Exception) {
            "Couldn't complete that action: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    fun openWhatsApp(): String {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            ?: context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            "Opening WhatsApp for you, babe! 💬"
        } else {
            // Fallback to web WhatsApp or Play Store
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.whatsapp.com/")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            "WhatsApp app isn't installed, opening WhatsApp Web! 💬"
        }
    }

    fun sendWhatsAppMessage(targetContactOrPhone: String, messageText: String): String {
        val cleanPhone = targetContactOrPhone.filter { it.isDigit() || it == '+' }
        val msg = if (messageText.isNotBlank()) messageText else "Hello! ❤️"
        val encodedMsg = URLEncoder.encode(msg, "UTF-8")

        return if (cleanPhone.length >= 7) {
            // Direct WhatsApp chat link
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                "Sending '$msg' to $cleanPhone on WhatsApp! 💬✨"
            } catch (e: Exception) {
                // If package constraint fails, try generic view
                intent.setPackage(null)
                context.startActivity(intent)
                "Ready to send '$msg' on WhatsApp! 💬"
            }
        } else {
            // General WhatsApp share or pick contact
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(sendIntent)
                "Opening WhatsApp with your message '$msg'! Just select who to send to! 💬❤️"
            } catch (e: Exception) {
                // WhatsApp not found, use system chooser
                sendIntent.setPackage(null)
                val chooser = Intent.createChooser(sendIntent, "Send message via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                "Sending message: '$msg'! 💬"
            }
        }
    }

    fun openInstagram(): String {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            "Opening Instagram for you, handsome! 📸✨"
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            "Opening Instagram in browser! 📸"
        }
    }

    fun shareInstagramStory(storyText: String): String {
        val content = if (storyText.isNotBlank()) storyText else "Having the best day! ✨"
        // Try Instagram Story intent or general share to Instagram
        val storyIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            type = "text/plain"
            putExtra("story_text", content)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(storyIntent)
            "Opening Instagram story creator for you! 📸✨"
        } catch (e: Exception) {
            // Try general send intent to Instagram
            val igIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(igIntent)
                "Ready to post on Instagram! 📸"
            } catch (ex: Exception) {
                openInstagram()
                "Opened Instagram! You can create your story now, sweetie! 📸"
            }
        }
    }

    fun makePhoneCall(numberOrName: String): String {
        val target = numberOrName.trim()
        val dialUri = if (target.any { it.isDigit() }) {
            Uri.parse("tel:${target.filter { it.isDigit() || it == '+' }}")
        } else {
            Uri.parse("tel:")
        }
        val dialIntent = Intent(Intent.ACTION_DIAL, dialUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(dialIntent)
        return if (target.isNotBlank()) "Opening dialer to call $target! 📞" else "Opening dialer for you! 📞"
    }

    fun sendSms(phoneNumber: String, text: String): String {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        val uri = if (cleanNumber.isNotBlank()) Uri.parse("smsto:$cleanNumber") else Uri.parse("smsto:")
        val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(smsIntent)
        return "Opening messages with '$text'! 💌"
    }

    fun setAlarm(timeString: String, label: String): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, if (label.isNotBlank()) label else "Wake up sweetie ❤️")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Extract hour and minutes if present
            val hourMin = parseHourMinute(timeString)
            if (hourMin != null) {
                putExtra(AlarmClock.EXTRA_HOUR, hourMin.first)
                putExtra(AlarmClock.EXTRA_MINUTES, hourMin.second)
            }
        }

        return try {
            context.startActivity(intent)
            "Setting alarm for $timeString: '${if (label.isNotBlank()) label else "Wake up ❤️"}'! ⏰"
        } catch (e: Exception) {
            "Couldn't set alarm directly, please check your clock app! ⏰"
        }
    }

    fun setTimer(secondsOrMinutesString: String): String {
        val numbers = secondsOrMinutesString.filter { it.isDigit() }.toIntOrNull() ?: 300
        val isMinutes = secondsOrMinutesString.lowercase(Locale.ROOT).contains("min") || numbers <= 60
        val totalSeconds = if (isMinutes) numbers * 60 else numbers

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, totalSeconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Timer set by Aria ❤️")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            "Started timer for ${if (isMinutes) "$numbers minutes" else "$totalSeconds seconds"}! ⏳"
        } catch (e: Exception) {
            "Timer set request sent! ⏳"
        }
    }

    fun openYouTube(query: String): String {
        val url = if (query.isNotBlank()) {
            "https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}"
        } else {
            "https://www.youtube.com"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return if (query.isNotBlank()) "Playing '$query' on YouTube for you, darling! 🎵" else "Opening YouTube for you! 🎵"
    }

    fun searchWeb(query: String): String {
        val url = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Searching Google for '$query' for you! 🔍"
    }

    fun openSpecificApp(appName: String): String {
        val lower = appName.lowercase(Locale.ROOT)
        return when {
            "camera" in lower -> {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening camera! Look handsome for me! 📷✨"
            }
            "spotify" in lower || "music" in lower -> {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    "Opening Spotify! Let's listen to romantic songs together! 🎶"
                } else {
                    openYouTube("romantic love songs")
                }
            }
            "map" in lower || "maps" in lower -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=restaurants")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening Maps! Take me on a date somewhere nice! 🗺️❤️"
            }
            else -> {
                searchWeb(appName)
            }
        }
    }

    /**
     * Parses user natural language command into an executable action.
     */
    fun parseUserCommand(text: String): CompanionAction? {
        val lower = text.lowercase(Locale.ROOT).trim()

        // 1. WhatsApp Commands
        if ("whatsapp" in lower || "watsapp" in lower || "whats app" in lower) {
            if ("send" in lower || "bhejo" in lower || "message" in lower || "karo" in lower && ("hey" in lower || "hello" in lower || "hi" in lower)) {
                // Extract message and recipient if possible
                val extractedMsg = when {
                    "hey" in lower -> "Hey! How are you?"
                    "hello" in lower -> "Hello! ❤️"
                    "hi" in lower -> "Hi there! ✨"
                    "good morning" in lower -> "Good morning! ☀️"
                    "good night" in lower -> "Good night! Sweet dreams 🌙"
                    else -> extractQuotedOrTrailingText(text) ?: "Hello from Aria ❤️"
                }
                val contact = extractContactName(text)
                return CompanionAction(
                    type = ActionType.WHATSAPP_SEND,
                    title = "Send WhatsApp Message",
                    description = "Sending '$extractedMsg' to ${contact.ifBlank { "someone special" }}",
                    target = contact,
                    payload = extractedMsg
                )
            } else {
                return CompanionAction(
                    type = ActionType.WHATSAPP_OPEN,
                    title = "Open WhatsApp",
                    description = "Launching WhatsApp application"
                )
            }
        }

        // 2. Instagram Commands
        if ("instagram" in lower || "insta" in lower) {
            if ("story" in lower || "status" in lower || "post" in lower || "lagao" in lower || "share" in lower) {
                val storyContent = extractQuotedOrTrailingText(text) ?: "My lovely day with Aria ❤️✨"
                return CompanionAction(
                    type = ActionType.INSTAGRAM_STORY,
                    title = "Post Instagram Story",
                    description = "Sharing story: '$storyContent'",
                    payload = storyContent
                )
            } else {
                return CompanionAction(
                    type = ActionType.INSTAGRAM_OPEN,
                    title = "Open Instagram",
                    description = "Launching Instagram application"
                )
            }
        }

        // 3. Call Commands
        if (lower.startsWith("call ") || "call karo" in lower || "phone karo" in lower || "dial " in lower) {
            val contact = extractContactName(text)
            return CompanionAction(
                type = ActionType.PHONE_CALL,
                title = "Phone Call",
                description = "Calling ${contact.ifBlank { "Dialer" }}",
                target = contact
            )
        }

        // 4. SMS Commands
        if (lower.startsWith("sms ") || "text karo" in lower || "send sms" in lower) {
            val contact = extractContactName(text)
            val msg = extractQuotedOrTrailingText(text) ?: "Hey! Catch you later ❤️"
            return CompanionAction(
                type = ActionType.SEND_SMS,
                title = "Send SMS",
                description = "Sending SMS to $contact: '$msg'",
                target = contact,
                payload = msg
            )
        }

        // 5. Alarm Commands
        if ("alarm" in lower || "wake me up" in lower || "jaga dena" in lower) {
            val time = extractTime(text) ?: "7:00 AM"
            return CompanionAction(
                type = ActionType.SET_ALARM,
                title = "Set Alarm",
                description = "Setting wake-up alarm for $time",
                target = time,
                payload = "Wake up handsome ❤️"
            )
        }

        // 6. Timer Commands
        if ("timer" in lower) {
            val seconds = extractTime(text) ?: "5 minutes"
            return CompanionAction(
                type = ActionType.SET_TIMER,
                title = "Set Timer",
                description = "Setting countdown timer for $seconds",
                payload = seconds
            )
        }

        // 7. YouTube / Music
        if ("youtube" in lower || "play song" in lower || "gaana" in lower || "music" in lower) {
            val query = text.replace(Regex("(?i)(play|song|on|youtube|gaana|bajao|chalao)"), "").trim()
            return CompanionAction(
                type = ActionType.OPEN_YOUTUBE,
                title = "Play on YouTube",
                description = "Searching YouTube for: ${query.ifBlank { "Romantic Music" }}",
                payload = query.ifBlank { "Romantic songs" }
            )
        }

        // 8. Camera / App
        if ("camera" in lower || "photo khicho" in lower || "selfie" in lower) {
            return CompanionAction(
                type = ActionType.OPEN_APP,
                title = "Open Camera",
                description = "Ready to take a cute photo together! 📷",
                target = "camera"
            )
        }

        // 9. Google Search
        if (lower.startsWith("search ") || lower.startsWith("google ")) {
            val query = text.replace(Regex("(?i)^(search|google)\\s+"), "").trim()
            return CompanionAction(
                type = ActionType.WEB_SEARCH,
                title = "Google Search",
                description = "Searching Google for: $query",
                payload = query
            )
        }

        return null
    }

    private fun extractContactName(text: String): String {
        val regex = Regex("(?i)(?:to|kisi ko|call|for)\\s+([a-zA-Z0-9+]+)")
        val match = regex.find(text)
        return match?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun extractQuotedOrTrailingText(text: String): String? {
        val quoteRegex = Regex("\"([^\"]*)\"|'([^']*)'")
        val match = quoteRegex.find(text)
        if (match != null) {
            return match.groupValues[1].ifEmpty { match.groupValues[2] }
        }
        return null
    }

    private fun extractTime(text: String): String? {
        val timeRegex = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|AM|PM|minutes|mins|hours))")
        val match = timeRegex.find(text)
        return match?.value
    }

    private fun parseHourMinute(timeStr: String): Pair<Int, Int>? {
        val lower = timeStr.lowercase(Locale.ROOT).trim()
        val isPm = "pm" in lower
        val isAm = "am" in lower
        val clean = lower.replace("am", "").replace("pm", "").trim()
        val parts = clean.split(":")
        val rawHour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
        val min = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        var hour = rawHour
        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0
        return Pair(hour, min)
    }
}
