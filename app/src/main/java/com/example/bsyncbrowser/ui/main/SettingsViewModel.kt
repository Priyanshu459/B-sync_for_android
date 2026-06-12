package com.example.bsyncbrowser.ui.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BrowserSettings(
    val searchEngine: String = "google",
    val themeBgMain: Long = 0xFF1E1E1E,
    val themeBgDark: Long = 0xFF0F0F0F,
    val themeAccent: Long = 0xFF3B82F6,
    val sidebarPos: String = "left",
    val compactMode: Boolean = false
)

data class SettingsState(
    val isSettingsOpen: Boolean = false,
    val settings: BrowserSettings = BrowserSettings()
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("bsync_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val json = prefs.getString("settings_data", null)
        if (json != null) {
            try {
                val settings = Json.decodeFromString<BrowserSettings>(json)
                _uiState.update { it.copy(settings = settings) }
            } catch (e: Exception) {
                // fallback to default
            }
        }
    }

    fun toggleSettingsModal() {
        _uiState.update { it.copy(isSettingsOpen = !it.isSettingsOpen) }
    }

    fun updateSettings(newSettings: BrowserSettings) {
        _uiState.update { it.copy(settings = newSettings) }
        val json = Json.encodeToString(newSettings)
        prefs.edit().putString("settings_data", json).apply()
    }

    fun resetToDefaults() {
        updateSettings(BrowserSettings())
    }
}
