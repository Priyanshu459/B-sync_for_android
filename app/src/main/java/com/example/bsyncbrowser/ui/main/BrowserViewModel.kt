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
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoSession
import com.example.bsyncbrowser.update.UpdateInfo
import com.example.bsyncbrowser.update.UpdateChecker
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

@Serializable
data class LibraryItem(val url: String, val title: String)

data class BrowserTab(
    val id: Long,
    val session: GeckoSession,
    var url: String = "resource://android/assets/newtab.html",
    var title: String = "New Tab",
    val isIncognito: Boolean = false,
    var canGoBack: Boolean = false
)

data class BrowserState(
    val tabs: List<BrowserTab> = emptyList(),
    val activeTabIndex: Int = -1,
    val bookmarks: List<LibraryItem> = emptyList(),
    val history: List<LibraryItem> = emptyList(),
    val updateInfo: UpdateInfo? = null
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    val geckoRuntime = GeckoRuntime.create(application).apply {
        settings.aboutConfigEnabled = true
        webExtensionController.ensureBuiltIn("resource://android/assets/ublock/", "uBlock0@raymondhill.net")
            .accept(
                { extension ->
                    extension?.let {
                        webExtensionController.setAllowedInPrivateBrowsing(it, true)
                    }
                },
                { e -> e?.printStackTrace() }
            )
        
        webExtensionController.ensureBuiltIn("resource://android/assets/bsync_shields/", "bsync-shields@bsync.app")
            .accept(
                { extension ->
                    extension?.let {
                        webExtensionController.setAllowedInPrivateBrowsing(it, true)
                    }
                },
                { e -> e?.printStackTrace() }
            )

        settings.contentBlocking.setAntiTracking(ContentBlocking.AntiTracking.STRICT)
        settings.contentBlocking.setCookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY)
        settings.contentBlocking.setSafeBrowsing(
            ContentBlocking.SafeBrowsing.MALWARE or 
            ContentBlocking.SafeBrowsing.PHISHING
        )
    }

    private val masterKey = MasterKey.Builder(application)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        application,
        "bsync_library_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _uiState = MutableStateFlow(BrowserState())
    val uiState: StateFlow<BrowserState> = _uiState.asStateFlow()

    private var nextTabId = 0L

    init {
        loadLibrary()
        createNewTab("resource://android/assets/newtab.html")
        checkForUpdates()
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            val appVersion = try {
                getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0).versionName
            } catch (e: Exception) {
                "1.0"
            }
            val update = UpdateChecker.checkForUpdate(appVersion ?: "1.0")
            if (update?.isUpdateAvailable == true) {
                _uiState.update { it.copy(updateInfo = update) }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(updateInfo = null) }
    }

    fun loadLibrary() {
        val bookmarksJson = prefs.getString("bookmarks", "[]") ?: "[]"
        val historyJson = prefs.getString("history", "[]") ?: "[]"
        try {
            val loadedBookmarks = Json.decodeFromString<List<LibraryItem>>(bookmarksJson)
            val loadedHistory = Json.decodeFromString<List<LibraryItem>>(historyJson)
            _uiState.update { it.copy(bookmarks = loadedBookmarks, history = loadedHistory) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveBookmarks(bookmarks: List<LibraryItem>) {
        prefs.edit().putString("bookmarks", Json.encodeToString(bookmarks)).apply()
    }

    private fun saveHistory(history: List<LibraryItem>) {
        prefs.edit().putString("history", Json.encodeToString(history)).apply()
    }

    fun toggleBookmark(url: String, title: String) {
        _uiState.update { state ->
            val isBookmarked = state.bookmarks.any { it.url == url }
            val newBookmarks = if (isBookmarked) {
                state.bookmarks.filter { it.url != url }
            } else {
                state.bookmarks + LibraryItem(url, title)
            }
            saveBookmarks(newBookmarks)
            state.copy(bookmarks = newBookmarks)
        }
    }

    fun deleteBookmark(index: Int) {
        _uiState.update { state ->
            if (index in state.bookmarks.indices) {
                val newBookmarks = state.bookmarks.filterIndexed { i, _ -> i != index }
                saveBookmarks(newBookmarks)
                state.copy(bookmarks = newBookmarks)
            } else state
        }
    }

    fun deleteHistoryItem(index: Int) {
        _uiState.update { state ->
            if (index in state.history.indices) {
                val newHistory = state.history.filterIndexed { i, _ -> i != index }
                saveHistory(newHistory)
                state.copy(history = newHistory)
            } else state
        }
    }

    fun clearHistory() {
        _uiState.update { state ->
            saveHistory(emptyList())
            state.copy(history = emptyList())
        }
    }

    private fun addToHistory(url: String, title: String, isIncognito: Boolean) {
        if (isIncognito) return // Do not save history for incognito tabs
        _uiState.update { state ->
            if (state.history.firstOrNull()?.url == url) return@update state
            val newHistory = listOf(LibraryItem(url, title)) + state.history
            saveHistory(newHistory)
            state.copy(history = newHistory)
        }
    }

    fun createNewTab(url: String, isIncognito: Boolean = false) {
        val sessionSettings = org.mozilla.geckoview.GeckoSessionSettings.Builder()
            .usePrivateMode(isIncognito)
            .useTrackingProtection(true)
            .build()
            
        val session = GeckoSession(sessionSettings).apply {
            open(geckoRuntime)
            
            navigationDelegate = object : GeckoSession.NavigationDelegate {
                override fun onLocationChange(
                    session: GeckoSession,
                    location: String?,
                    perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                    hasUserGesture: Boolean
                ) {
                    location?.let { newUrl ->
                        updateTabUrl(session, newUrl)
                        // Add to history
                        val state = _uiState.value
                        val tab = state.tabs.find { it.session == session }
                        val title = tab?.title ?: newUrl
                        addToHistory(newUrl, title, tab?.isIncognito ?: isIncognito)
                    }
                }
                
                override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                    _uiState.update { state ->
                        val updatedTabs = state.tabs.map { tab ->
                            if (tab.session == session) tab.copy(canGoBack = canGoBack) else tab
                        }
                        state.copy(tabs = updatedTabs)
                    }
                }
            }

            promptDelegate = object : GeckoSession.PromptDelegate {
                private fun isScareware(text: String?): Boolean {
                    if (text == null) return false
                    val lower = text.lowercase()
                    return lower.contains("warning") || 
                           lower.contains("clean your device") || 
                           lower.contains("virus") || 
                           lower.contains("downloads pending") || 
                           lower.contains("proceed now")
                }

                override fun onAlertPrompt(
                    session: GeckoSession,
                    prompt: GeckoSession.PromptDelegate.AlertPrompt
                ): org.mozilla.geckoview.GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                    if (isScareware(prompt.title) || isScareware(prompt.message)) {
                        return org.mozilla.geckoview.GeckoResult.fromValue(prompt.dismiss())
                    }
                    return null
                }

                override fun onButtonPrompt(
                    session: GeckoSession,
                    prompt: GeckoSession.PromptDelegate.ButtonPrompt
                ): org.mozilla.geckoview.GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                    if (isScareware(prompt.title) || isScareware(prompt.message)) {
                        return org.mozilla.geckoview.GeckoResult.fromValue(prompt.dismiss())
                    }
                    return null
                }
            }
            loadUri(url)
        }

        val newTab = BrowserTab(
            id = nextTabId++,
            session = session,
            url = url,
            isIncognito = isIncognito
        )

        _uiState.update { state ->
            val newTabs = state.tabs + newTab
            state.copy(
                tabs = newTabs,
                activeTabIndex = newTabs.size - 1
            )
        }
    }

    fun switchTab(index: Int) {
        _uiState.update { state ->
            if (index in state.tabs.indices) {
                state.copy(activeTabIndex = index)
            } else {
                state
            }
        }
    }

    fun closeTab(index: Int) {
        _uiState.update { state ->
            if (index in state.tabs.indices) {
                val tabToClose = state.tabs[index]
                tabToClose.session.close()

                val newTabs = state.tabs.filterIndexed { i, _ -> i != index }
                val newIndex = if (newTabs.isEmpty()) -1 
                               else if (state.activeTabIndex >= newTabs.size) newTabs.size - 1 
                               else state.activeTabIndex
                
                state.copy(
                    tabs = newTabs,
                    activeTabIndex = newIndex
                )
            } else {
                state
            }
        }
    }

    fun loadUrlInActiveTab(url: String, searchEngine: String = "google") {
        val state = _uiState.value
        if (state.activeTabIndex in state.tabs.indices) {
            val input = url.trim()
            var finalUrl = input
            
            val isUrl = android.util.Patterns.WEB_URL.matcher(input).matches()
            if (isUrl || input.contains("://")) {
                if (!input.startsWith("http://") && !input.startsWith("https://") && !input.startsWith("resource://") && !input.startsWith("file://") && !input.startsWith("moz-extension://")) {
                    finalUrl = "https://$input"
                }
            } else {
                val encoded = java.net.URLEncoder.encode(input, "UTF-8")
                finalUrl = when (searchEngine.lowercase()) {
                    "bing" -> "https://www.bing.com/search?q=$encoded"
                    "duckduckgo" -> "https://duckduckgo.com/?q=$encoded"
                    "brave" -> "https://search.brave.com/search?q=$encoded"
                    else -> "https://www.google.com/search?q=$encoded" // google fallback
                }
            }

            state.tabs[state.activeTabIndex].session.loadUri(finalUrl)
            updateTabUrl(state.tabs[state.activeTabIndex].session, finalUrl)
            addToHistory(finalUrl, finalUrl, state.tabs[state.activeTabIndex].isIncognito)
        }
    }

    fun goBackActiveTab() {
        val state = _uiState.value
        if (state.activeTabIndex in state.tabs.indices) {
            state.tabs[state.activeTabIndex].session.goBack()
        }
    }

    fun goForwardActiveTab() {
        val state = _uiState.value
        if (state.activeTabIndex in state.tabs.indices) {
            state.tabs[state.activeTabIndex].session.goForward()
        }
    }

    fun reloadActiveTab() {
        val state = _uiState.value
        if (state.activeTabIndex in state.tabs.indices) {
            state.tabs[state.activeTabIndex].session.reload()
        }
    }

    private fun updateTabUrl(session: GeckoSession, newUrl: String) {
        _uiState.update { state ->
            val newTabs = state.tabs.map { tab ->
                if (tab.session == session) tab.copy(url = newUrl) else tab
            }
            state.copy(tabs = newTabs)
        }
    }

    override fun onCleared() {
        _uiState.value.tabs.forEach { it.session.close() }
        super.onCleared()
    }
}
