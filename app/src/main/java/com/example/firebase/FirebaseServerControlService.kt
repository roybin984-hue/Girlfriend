package com.example.firebase

import android.content.Context
import android.content.SharedPreferences
import com.example.data.ServerControlConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class FirebaseServerControlService(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("firebase_admin_control", Context.MODE_PRIVATE)

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // User's configured Firebase Realtime Database URL
    private val firebaseUrl = "https://admin-panel-d48f2-default-rtdb.firebaseio.com/server_control.json"

    private val _config = MutableStateFlow(loadLocalConfig())
    val config: StateFlow<ServerControlConfig> = _config.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        // Initial fetch from Firebase on app launch
        fetchRemoteConfig()
    }

    fun fetchRemoteConfig(onResult: ((Boolean, String) -> Unit)? = null) {
        coroutineScope.launch {
            _isSyncing.value = true
            try {
                val url = URL(firebaseUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Accept", "application/json")
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()

                    if (response.isNotBlank() && response != "null") {
                        val json = JSONObject(response)
                        val maintenance = json.optBoolean("maintenanceMode", json.optBoolean("maintenance_mode", false))
                        val serverActive = json.optBoolean("serverActive", json.optBoolean("server_active", true))
                        val msg = json.optString(
                            "maintenanceMessage",
                            json.optString("maintenance_message", "Server maintenance chal raha hai! Admin ne temporary band kar rakha hai. Thodi der me restart ho jayega! 🛠️")
                        )
                        val announcement = json.optString("announcement", "")
                        val adminEmail = json.optString("adminEmail", "roysurjo27238@gmail.com")

                        val newConfig = ServerControlConfig(
                            maintenanceMode = maintenance,
                            serverActive = serverActive,
                            maintenanceMessage = msg,
                            announcement = announcement,
                            adminEmail = adminEmail,
                            lastUpdated = System.currentTimeMillis()
                        )

                        _config.value = newConfig
                        saveLocalConfig(newConfig)
                        withContext(Dispatchers.Main) {
                            onResult?.invoke(true, "Firebase status synced successfully! ✅")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onResult?.invoke(true, "Firebase connected! Server is active. 🟢")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false, "Server HTTP error: $responseCode")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, "Could not reach Firebase: ${e.message ?: "Network error"}")
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun updateRemoteConfig(
        maintenanceMode: Boolean,
        serverActive: Boolean,
        maintenanceMessage: String,
        announcement: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        coroutineScope.launch {
            _isSyncing.value = true
            try {
                val jsonPayload = JSONObject().apply {
                    put("maintenanceMode", maintenanceMode)
                    put("serverActive", serverActive)
                    put("maintenanceMessage", maintenanceMessage)
                    put("announcement", announcement)
                    put("adminEmail", "roysurjo27238@gmail.com")
                    put("lastUpdated", System.currentTimeMillis())
                }

                val url = URL(firebaseUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 8000
                    readTimeout = 8000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonPayload.toString())
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val updatedConfig = ServerControlConfig(
                        maintenanceMode = maintenanceMode,
                        serverActive = serverActive,
                        maintenanceMessage = maintenanceMessage,
                        announcement = announcement,
                        lastUpdated = System.currentTimeMillis()
                    )
                    _config.value = updatedConfig
                    saveLocalConfig(updatedConfig)
                    withContext(Dispatchers.Main) {
                        onComplete(true, "Firebase Server Control updated successfully! 🚀")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Failed to update Firebase: HTTP $responseCode")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Update failed: ${e.message ?: "Connection error"}")
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun loadLocalConfig(): ServerControlConfig {
        return ServerControlConfig(
            maintenanceMode = prefs.getBoolean("maintenanceMode", false),
            serverActive = prefs.getBoolean("serverActive", true),
            maintenanceMessage = prefs.getString(
                "maintenanceMessage",
                "Server maintenance chal raha hai! Admin ne temporary band kar rakha hai. Thodi der me restart ho jayega! 🛠️"
            ) ?: "",
            announcement = prefs.getString("announcement", "") ?: ""
        )
    }

    private fun saveLocalConfig(config: ServerControlConfig) {
        prefs.edit()
            .putBoolean("maintenanceMode", config.maintenanceMode)
            .putBoolean("serverActive", config.serverActive)
            .putString("maintenanceMessage", config.maintenanceMessage)
            .putString("announcement", config.announcement)
            .putLong("lastUpdated", config.lastUpdated)
            .apply()
    }
}
