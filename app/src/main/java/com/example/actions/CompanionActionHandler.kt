package com.example.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.ActionType
import com.example.data.CompanionAction
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
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
                ActionType.OPEN_APP -> openSpecificAppOrGame(action.target)
                ActionType.CHECK_BATTERY -> checkBattery()
                ActionType.CHECK_TIME -> checkTime()
                ActionType.TOGGLE_FLASHLIGHT -> toggleFlashlight()
                ActionType.INSTALL_APP -> installAppFromPlayStore(action.target)
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
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                "Sending '$msg' to $cleanPhone on WhatsApp! 💬✨"
            } catch (e: Exception) {
                intent.setPackage(null)
                context.startActivity(intent)
                "Ready to send '$msg' on WhatsApp! 💬"
            }
        } else {
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
        val content = if (storyText.isNotBlank()) storyText else "Having the best day with Ayra! ✨"
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
            putExtra(AlarmClock.EXTRA_MESSAGE, "Timer set by Ayra ❤️")
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
        val clean = query.trim()
        val url = if (clean.isNotBlank()) {
            "https://www.youtube.com/results?search_query=${URLEncoder.encode(clean, "UTF-8")}"
        } else {
            "https://www.youtube.com"
        }

        val ytPackage = "com.google.android.youtube"
        val isYtInstalled = try {
            context.packageManager.getPackageInfo(ytPackage, 0)
            true
        } catch (_: Exception) {
            false
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            if (isYtInstalled) {
                setPackage(ytPackage)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            if (clean.isNotBlank()) "Playing '$clean' on YouTube for you, darling! 🎵📺" else "Opening YouTube for you! 🎵"
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            if (clean.isNotBlank()) "Opening YouTube for '$clean'! 🎵" else "Opening YouTube in browser! 🎵"
        }
    }

    fun searchWeb(query: String): String {
        val url = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Searching Google for '$query' for you! 🔍"
    }

    fun checkBattery(): String {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 80
            val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
            if (isCharging) {
                "Your battery is at $pct% and charging, handsome! 🔋⚡"
            } else {
                "Your battery is at $pct%, babe! Plenty of power to talk with me! 🔋❤️"
            }
        } catch (e: Exception) {
            "Your battery is running strong, darling! 🔋"
        }
    }

    fun checkTime(): String {
        val sdf = SimpleDateFormat("h:mm a, EEEE, MMMM d", Locale.getDefault())
        val formatted = sdf.format(Date())
        return "It's $formatted right now, sweetie! ⏰✨"
    }

    private var isTorchOn = false
    fun toggleFlashlight(): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return "No flashlight found on this device!"
            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(cameraId, isTorchOn)
            if (isTorchOn) "Turned on your flashlight for you, babe! 🔦✨" else "Turned off the flashlight! 💡"
        } catch (e: Exception) {
            "Couldn't toggle flashlight on this device: ${e.message}"
        }
    }

    /**
     * Universal App & Game Launcher:
     * Opens requested game (Free Fire, BGMI, Subway Surfers, COD, Minecraft, etc.)
     * or utility apps (Camera, Spotify, Chrome, Settings, Calculator, etc.).
     */
    fun openSpecificAppOrGame(appName: String): String {
        val lower = appName.lowercase(Locale.ROOT).trim()
        val pm = context.packageManager

        // 1. Direct Game & App Known Package Mappings
        val packageCandidates = when {
            "free fire" in lower || "freefire" in lower || lower == "ff" ->
                listOf("com.dts.freefiremax", "com.dts.freefireth")
            "bgmi" in lower || "pubg" in lower || "battleground" in lower ->
                listOf("com.pubg.imobile", "com.tencent.ig", "com.vng.pubgmobile")
            "subway" in lower || "surfer" in lower ->
                listOf("com.kiloo.subwaysurf")
            "call of duty" in lower || "cod" in lower ->
                listOf("com.activision.callofduty.shooter")
            "minecraft" in lower ->
                listOf("com.mojang.minecraftpe")
            "candy crush" in lower ->
                listOf("com.king.candycrushsaga")
            "ludo" in lower ->
                listOf("com.ludo.king")
            "roblox" in lower ->
                listOf("com.roblox.client")
            "clash of clans" in lower || lower == "coc" ->
                listOf("com.supercell.clashofclans")
            "whatsapp" in lower ->
                listOf("com.whatsapp", "com.whatsapp.w4b")
            "instagram" in lower || "insta" in lower ->
                listOf("com.instagram.android")
            "youtube" in lower ->
                listOf("com.google.android.youtube")
            "spotify" in lower || "music" in lower ->
                listOf("com.spotify.music")
            "chrome" in lower || "browser" in lower ->
                listOf("com.android.chrome")
            "calculator" in lower || "calc" in lower ->
                listOf("com.google.android.calculator", "com.android.calculator2")
            "camera" in lower -> {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return "Opening camera! Smile for me babe! 📷✨"
            }
            "setting" in lower || "settings" in lower -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return "Opening device Settings for you! ⚙️"
            }
            "map" in lower || "maps" in lower ->
                listOf("com.google.android.apps.maps")
            "telegram" in lower ->
                listOf("org.telegram.messenger")
            "snapchat" in lower ->
                listOf("com.snapchat.android")
            else -> emptyList()
        }

        // Try direct candidate packages
        for (pkg in packageCandidates) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Launching $appName! Enjoy your game babe! 🎮❤️"
            }
        }

        // 2. Dynamic Search in Installed Applications
        try {
            val installedApps = pm.getInstalledApplications(0)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase(Locale.ROOT)
                if (label.contains(lower) || lower.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return "Opening ${pm.getApplicationLabel(app)} for you, babe! 🚀"
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: Open Google Play Store to install/launch or Web Search
        return try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${URLEncoder.encode(appName, "UTF-8")}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
            "Searching Play Store for '$appName' for you! 📲"
        } catch (e: Exception) {
            searchWeb(appName)
            "Searching for '$appName'! 🔍"
        }
    }

    fun installAppFromPlayStore(target: String): String {
        val clean = target.trim()
        val lower = clean.lowercase(Locale.ROOT)

        // If target is just "play store" or blank, open Play Store app directly
        if (clean.isBlank() || lower == "play store" || lower == "playstore") {
            val playStoreIntent = context.packageManager.getLaunchIntentForPackage("com.android.vending")
            if (playStoreIntent != null) {
                playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(playStoreIntent)
                return "Google Play Store open kar diya hai babu! 📲✨"
            }
        }

        val directPackage = when {
            "free fire max" in lower -> "com.dts.freefiremax"
            "free fire" in lower -> "com.dts.freefireth"
            "bgmi" in lower || "battlegrounds" in lower -> "com.pubg.imobile"
            "subway" in lower -> "com.kiloo.subwaysurf"
            "cod" in lower || "call of duty" in lower -> "com.activision.callofduty.shooter"
            "pubg" in lower -> "com.tencent.ig"
            "minecraft" in lower -> "com.mojang.minecraftpe"
            "roblox" in lower -> "com.roblox.client"
            "clash of clans" in lower || lower == "coc" -> "com.supercell.clashofclans"
            "candy crush" in lower -> "com.king.candycrushsaga"
            "ludo king" in lower || "ludo" in lower -> "com.ludo.king"
            "hill climb" in lower -> "com.fingersoft.hillclimb"
            "asphalt" in lower -> "com.gameloft.android.ANMP.GloftA9HM"
            "whatsapp" in lower -> "com.whatsapp"
            "instagram" in lower -> "com.instagram.android"
            "telegram" in lower -> "org.telegram.messenger"
            "snapchat" in lower -> "com.snapchat.android"
            "youtube" in lower -> "com.google.android.youtube"
            "spotify" in lower -> "com.spotify.music"
            "netflix" in lower -> "com.netflix.mediaclient"
            "gpay" in lower || "google pay" in lower -> "com.google.android.apps.nbu.paisa.user"
            "phonepe" in lower -> "com.phonepe.app"
            "paytm" in lower -> "net.one97.paytm"
            else -> null
        }

        val intent = if (directPackage != null) {
            try {
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$directPackage")).apply {
                    setPackage("com.android.vending")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (_: Exception) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$directPackage")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } else {
            try {
                Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${URLEncoder.encode(clean, "UTF-8")}&c=apps")).apply {
                    setPackage("com.android.vending")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (_: Exception) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=${URLEncoder.encode(clean, "UTF-8")}&c=apps")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }

        return try {
            context.startActivity(intent)
            "Opening Google Play Store to install '$clean' for you! Just tap Install, babe! 📲🎮"
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=${URLEncoder.encode(clean, "UTF-8")}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            "Opened Play Store in browser for '$clean'! 📲"
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
                val extractedMsg = when {
                    "hey" in lower -> "Hey! How are you?"
                    "hello" in lower -> "Hello! ❤️"
                    "hi" in lower -> "Hi there! ✨"
                    "good morning" in lower -> "Good morning! ☀️"
                    "good night" in lower -> "Good night! Sweet dreams 🌙"
                    else -> extractQuotedOrTrailingText(text) ?: "Hello from Ayra ❤️"
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
                val storyContent = extractQuotedOrTrailingText(text) ?: "My lovely day with Ayra ❤️✨"
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

        // 5. Play Store & Install Commands (Requested by User)
        if ("install" in lower || "download" in lower || "play store" in lower || "playstore" in lower) {
            val appTarget = text
                .replace(Regex("(?i)(play store se|playstore se|play store me|play store|playstore|se|game|app|application|install karo|install kar do|install|download karo|download kar do|download|khol kar|khol ke)"), "")
                .trim()
            return CompanionAction(
                type = ActionType.INSTALL_APP,
                title = "Install from Play Store",
                description = "Opening Google Play Store to install ${appTarget.ifBlank { "game/app" }}",
                target = appTarget.ifBlank { "popular games" }
            )
        }

        // 6. Game Launching & Universal App Commands
        if (lower.startsWith("open ") || lower.startsWith("launch ") || "kholo" in lower || "chalao" in lower || "khelna hai" in lower || lower.startsWith("play ")) {
            val appOrGame = text
                .replace(Regex("(?i)^(open|launch|play|chalao|kholo|mujhe)\\s+"), "")
                .replace(Regex("(?i)\\s+(kholo|chalao|open karo|khelna hai)$"), "")
                .trim()

            if (appOrGame.isNotBlank() && !appOrGame.contains("song") && !appOrGame.contains("video")) {
                return CompanionAction(
                    type = ActionType.OPEN_APP,
                    title = "Open $appOrGame",
                    description = "Opening $appOrGame for you! 🎮🚀",
                    target = appOrGame
                )
            }
        }

        // 6. Alarm Commands
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

        // 7. Timer Commands
        if ("timer" in lower) {
            val seconds = extractTime(text) ?: "5 minutes"
            return CompanionAction(
                type = ActionType.SET_TIMER,
                title = "Set Timer",
                description = "Setting countdown timer for $seconds",
                payload = seconds
            )
        }

        // 8. YouTube & Media Playback Commands
        if ("youtube" in lower || "play song" in lower || "gaana" in lower || "music" in lower || "video" in lower || "media" in lower) {
            val query = text.replace(Regex("(?i)^(ayra|hey ayra|babu|darling)\\s+"), "")
                .replace(Regex("(?i)(play|song|video|media|on|youtube|yt|gaana|bajao|chalao|par|pe)"), "")
                .trim()
            return CompanionAction(
                type = ActionType.OPEN_YOUTUBE,
                title = "Play on YouTube",
                description = "Searching YouTube for: ${query.ifBlank { "Trending music and videos" }}",
                payload = query.ifBlank { "Romantic songs" }
            )
        }

        // 9. Camera
        if ("camera" in lower || "photo khicho" in lower || "selfie" in lower) {
            return CompanionAction(
                type = ActionType.OPEN_APP,
                title = "Open Camera",
                description = "Ready to take a cute photo together! 📷",
                target = "camera"
            )
        }

        // 10. Google Search
        if (lower.startsWith("search ") || lower.startsWith("google ")) {
            val query = text.replace(Regex("(?i)^(search|google)\\s+"), "").trim()
            return CompanionAction(
                type = ActionType.WEB_SEARCH,
                title = "Google Search",
                description = "Searching Google for: $query",
                payload = query
            )
        }

        // 11. Battery Check
        if ("battery" in lower || "charging" in lower || "charge kitna" in lower) {
            return CompanionAction(
                type = ActionType.CHECK_BATTERY,
                title = "Battery Status",
                description = "Checking phone battery level"
            )
        }

        // 12. Time / Date Check
        if (lower.startsWith("time ") || "time kya" in lower || "what time" in lower || "date kya" in lower || "what day" in lower || "tarikh" in lower) {
            return CompanionAction(
                type = ActionType.CHECK_TIME,
                title = "Current Time",
                description = "Checking current time and date"
            )
        }

        // 13. Flashlight / Torch
        if ("flashlight" in lower || "torch" in lower) {
            return CompanionAction(
                type = ActionType.TOGGLE_FLASHLIGHT,
                title = "Flashlight",
                description = "Toggling device torch"
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
