package com.example.bsyncbrowser.ui.main

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class SyncState(
    val isSyncOpen: Boolean = false,
    val token: String? = null,
    val username: String? = null,
    val statusMessage: String = "",
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("bsync_sync_meta", Context.MODE_PRIVATE)
    private val libraryPrefs = application.getSharedPreferences("bsync_library", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SyncState())
    val uiState: StateFlow<SyncState> = _uiState.asStateFlow()

    private val serverUrl = "http://13.233.208.184"

    init {
        val savedToken = prefs.getString("syncToken", null)
        val savedUsername = prefs.getString("syncUsername", null)
        if (savedToken != null && savedUsername != null) {
            _uiState.update { it.copy(token = savedToken, username = savedUsername) }
        }
    }

    fun toggleSyncModal() {
        _uiState.update { it.copy(isSyncOpen = !it.isSyncOpen, statusMessage = "", isError = false) }
    }

    fun register(user: String, pass: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Registering...", isError = false) }
            try {
                val url = URL("$serverUrl/api/auth/register")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("username", user)
                    put("password", pass)
                }.toString()

                OutputStreamWriter(conn.outputStream).use { it.write(payload) }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Registered! You can now log in.", isError = false) }
                } else {
                    val errorStream = conn.errorStream
                    val response = BufferedReader(InputStreamReader(errorStream)).readText()
                    val errorMsg = JSONObject(response).optString("error", "Registration failed")
                    _uiState.update { it.copy(isLoading = false, statusMessage = errorMsg, isError = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, statusMessage = e.message ?: "Network error", isError = true) }
            }
        }
    }

    fun login(user: String, pass: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Logging in...", isError = false) }
            try {
                val url = URL("$serverUrl/api/auth/login")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("username", user)
                    put("password", pass)
                }.toString()

                OutputStreamWriter(conn.outputStream).use { it.write(payload) }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                    val json = JSONObject(response)
                    val token = json.getString("token")
                    val username = json.getString("username")

                    prefs.edit()
                        .putString("syncToken", token)
                        .putString("syncUsername", username)
                        .apply()

                    _uiState.update { it.copy(isLoading = false, token = token, username = username, statusMessage = "Logged in successfully!", isError = false) }
                } else {
                    val errorStream = conn.errorStream
                    val response = BufferedReader(InputStreamReader(errorStream)).readText()
                    val errorMsg = JSONObject(response).optString("error", "Login failed")
                    _uiState.update { it.copy(isLoading = false, statusMessage = errorMsg, isError = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, statusMessage = e.message ?: "Network error", isError = true) }
            }
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        _uiState.update { it.copy(token = null, username = null, statusMessage = "") }
    }

    fun pushSync() {
        val token = _uiState.value.token ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Pushing to cloud...", isError = false) }
            try {
                // Read local library data directly from SharedPreferences
                val bookmarksJson = libraryPrefs.getString("bookmarks", "[]") ?: "[]"
                val historyJson = libraryPrefs.getString("history", "[]") ?: "[]"
                
                // Parse them as JsonArrays to nest in the payload
                val bookmarksArray = org.json.JSONArray(bookmarksJson)
                val historyArray = org.json.JSONArray(historyJson)

                val url = URL("$serverUrl/api/sync/data")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("bookmarks", bookmarksArray)
                    put("history", historyArray)
                    // Omit vault for cross-platform encryption safety
                }.toString()

                OutputStreamWriter(conn.outputStream).use { it.write(payload) }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Successfully pushed to cloud!", isError = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Push failed (HTTP $responseCode)", isError = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, statusMessage = e.message ?: "Network error", isError = true) }
            }
        }
    }

    fun pullSync(onSuccess: (List<LibraryItem>, List<LibraryItem>) -> Unit) {
        val token = _uiState.value.token ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Pulling from cloud...", isError = false) }
            try {
                val url = URL("$serverUrl/api/sync/data")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                    val json = JSONObject(response)
                    
                    val bookmarksJson = json.optJSONArray("bookmarks")?.toString() ?: "[]"
                    val historyJson = json.optJSONArray("history")?.toString() ?: "[]"

                    // Save directly to prefs
                    libraryPrefs.edit()
                        .putString("bookmarks", bookmarksJson)
                        .putString("history", historyJson)
                        .apply()

                    val bookmarks = Json.decodeFromString<List<LibraryItem>>(bookmarksJson)
                    val history = Json.decodeFromString<List<LibraryItem>>(historyJson)

                    _uiState.update { it.copy(isLoading = false, statusMessage = "Successfully pulled data!", isError = false) }
                    
                    // Callback to update BrowserViewModel
                    launch(Dispatchers.Main) {
                        onSuccess(bookmarks, history)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Pull failed (HTTP $responseCode)", isError = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, statusMessage = e.message ?: "Network error", isError = true) }
            }
        }
    }
}
