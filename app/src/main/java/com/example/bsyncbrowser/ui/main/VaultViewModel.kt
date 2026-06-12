package com.example.bsyncbrowser.ui.main

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class VaultItem(val site: String, val username: String, val password: String)

data class VaultState(
    val isLocked: Boolean = true,
    val items: List<VaultItem> = emptyList(),
    val error: String? = null,
    val isVaultOpen: Boolean = false // controls if the modal is shown
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val masterKey = MasterKey.Builder(application)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        application,
        "bsync_secure_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val regularPrefs = application.getSharedPreferences("bsync_vault_meta", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(VaultState())
    val uiState: StateFlow<VaultState> = _uiState.asStateFlow()

    init {
        // If master password hash is not set, we assume it's a new vault
    }

    fun toggleVaultModal() {
        _uiState.update { it.copy(isVaultOpen = !it.isVaultOpen, isLocked = true, items = emptyList(), error = null) }
    }

    fun unlockVault(password: String) {
        val storedHash = regularPrefs.getString("master_password_hash", null)
        
        if (storedHash == null) {
            // First time setup
            regularPrefs.edit().putString("master_password_hash", password.hashCode().toString()).apply()
            _uiState.update { it.copy(isLocked = false, error = null) }
            loadVaultItems()
        } else {
            // Verify
            if (password.hashCode().toString() == storedHash) {
                _uiState.update { it.copy(isLocked = false, error = null) }
                loadVaultItems()
            } else {
                _uiState.update { it.copy(error = "Incorrect Master Password") }
            }
        }
    }

    fun lockVault() {
        _uiState.update { it.copy(isLocked = true, items = emptyList(), error = null) }
    }

    private fun loadVaultItems() {
        try {
            val json = encryptedPrefs.getString("vault_data", "[]") ?: "[]"
            val items = Json.decodeFromString<List<VaultItem>>(json)
            _uiState.update { it.copy(items = items) }
        } catch (e: Exception) {
            Log.e("VaultViewModel", "Error loading vault items", e)
            _uiState.update { it.copy(items = emptyList()) }
        }
    }

    fun addVaultItem(item: VaultItem) {
        if (_uiState.value.isLocked) return
        
        _uiState.update { state ->
            val newItems = state.items + item
            saveVaultItems(newItems)
            state.copy(items = newItems)
        }
    }

    fun deleteVaultItem(index: Int) {
        if (_uiState.value.isLocked) return
        
        _uiState.update { state ->
            if (index in state.items.indices) {
                val newItems = state.items.filterIndexed { i, _ -> i != index }
                saveVaultItems(newItems)
                state.copy(items = newItems)
            } else state
        }
    }

    private fun saveVaultItems(items: List<VaultItem>) {
        try {
            val json = Json.encodeToString(items)
            encryptedPrefs.edit().putString("vault_data", json).apply()
        } catch (e: Exception) {
            Log.e("VaultViewModel", "Error saving vault items", e)
        }
    }
}
