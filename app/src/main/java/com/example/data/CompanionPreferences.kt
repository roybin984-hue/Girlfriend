package com.example.data

import android.content.Context
import android.content.SharedPreferences

class CompanionPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aria_companion_prefs", Context.MODE_PRIVATE)

    var companionName: String
        get() = prefs.getString("companion_name", "Ayra") ?: "Ayra"
        set(value) = prefs.edit().putString("companion_name", value).apply()

    var userNickname: String
        get() = prefs.getString("user_nickname", "Babe") ?: "Babe"
        set(value) = prefs.edit().putString("user_nickname", value).apply()

    var voicePitch: Float
        get() {
            val stored = prefs.getFloat("voice_pitch", 1.22f)
            return if (stored <= 1.10f) 1.22f else stored
        }
        set(value) = prefs.edit().putFloat("voice_pitch", value).apply()

    var voiceSpeed: Float
        get() = prefs.getFloat("voice_speed", 0.98f)
        set(value) = prefs.edit().putFloat("voice_speed", value).apply()

    var voicePreset: String
        get() = prefs.getString("voice_preset", "Natural Sweet ❤️") ?: "Natural Sweet ❤️"
        set(value) = prefs.edit().putString("voice_preset", value).apply()

    var autoSpeak: Boolean
        get() = prefs.getBoolean("auto_speak", true)
        set(value) = prefs.edit().putBoolean("auto_speak", value).apply()

    var relationshipLevel: Int
        get() = prefs.getInt("rel_level", 1)
        set(value) = prefs.edit().putInt("rel_level", value).apply()

    var affectionXp: Int
        get() = prefs.getInt("affection_xp", 150)
        set(value) = prefs.edit().putInt("affection_xp", value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean("has_completed_onboarding", false)
        set(value) = prefs.edit().putBoolean("has_completed_onboarding", value).apply()

    fun addAffectionXp(amount: Int): Pair<Int, Boolean> {
        val newXp = affectionXp + amount
        val currentLvl = relationshipLevel
        val nextLevelThreshold = currentLvl * 100
        var leveledUp = false
        var newLvl = currentLvl

        if (newXp >= nextLevelThreshold) {
            newLvl = currentLvl + 1
            leveledUp = true
        }

        relationshipLevel = newLvl
        affectionXp = newXp
        return Pair(newLvl, leveledUp)
    }

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value.trim()).apply()

    var loudVoiceEnabled: Boolean
        get() = prefs.getBoolean("loud_voice_enabled", true)
        set(value) = prefs.edit().putBoolean("loud_voice_enabled", value).apply()

    var voiceVolumeLevel: Float
        get() = prefs.getFloat("voice_volume_level", 1.0f)
        set(value) = prefs.edit().putFloat("voice_volume_level", value).apply()

    fun getProfile(): CompanionProfile {
        return CompanionProfile(
            name = companionName,
            userNickname = userNickname,
            voicePitch = voicePitch,
            voiceSpeed = voiceSpeed,
            voicePreset = voicePreset,
            autoSpeak = autoSpeak,
            relationshipLevel = relationshipLevel,
            affectionXp = affectionXp,
            loudVoiceEnabled = loudVoiceEnabled,
            voiceVolumeLevel = voiceVolumeLevel
        )
    }
}
