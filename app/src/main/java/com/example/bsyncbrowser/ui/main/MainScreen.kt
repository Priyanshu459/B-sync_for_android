package com.example.bsyncbrowser.ui.main

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.bsyncbrowser.R
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.bsyncbrowser.update.UpdateDownloader
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.compositionLocalOf

// Theme Colors via CompositionLocal
val LocalBgMain = compositionLocalOf { Color(0xFF1E1E1E) }
val LocalBgDark = compositionLocalOf { Color(0xFF0F0F0F) }
val LocalAccentColor = compositionLocalOf { Color(0xFF3B82F6) }
val LocalBorderColor = compositionLocalOf { Color(0x1AFFFFFF) }
val LocalTextColor = compositionLocalOf { Color(0xE6FFFFFF) }

val BgMain: Color @Composable get() = LocalBgMain.current
val BgDark: Color @Composable get() = LocalBgDark.current
val AccentColor: Color @Composable get() = LocalAccentColor.current
val BorderColor: Color @Composable get() = LocalBorderColor.current
val TextColor: Color @Composable get() = LocalTextColor.current

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    browserViewModel: BrowserViewModel = viewModel(),
    vaultViewModel: VaultViewModel = viewModel(),
    syncViewModel: SyncViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    val uiState by browserViewModel.uiState.collectAsState()
    val vaultState by vaultViewModel.uiState.collectAsState()
    val syncState by syncViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val isFullscreen by browserViewModel.isFullscreen.collectAsState()
    
    val activeTab = if (uiState.activeTabIndex in uiState.tabs.indices) {
        uiState.tabs[uiState.activeTabIndex]
    } else null

    // Local state for the address bar text while typing
    val defaultDisplayUrl = if (activeTab?.url?.startsWith("resource://android/assets/newtab.html") == true) "" else activeTab?.url ?: ""
    var typedUrl by remember(activeTab?.url) { mutableStateOf(defaultDisplayUrl) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val settings = settingsState.settings
    val bgMainColor = Color(settings.themeBgMain)
    val bgDarkColor = Color(settings.themeBgDark)
    val accentColor = Color(settings.themeAccent)
    // If it's a light theme (bg > 0.8f), use black text
    val textColor = if (bgMainColor.red > 0.8f) Color(0xFF1F2937) else Color(0xE6FFFFFF)
    val borderColor = if (bgMainColor.red > 0.8f) Color(0x1A000000) else Color(0x1AFFFFFF)

    CompositionLocalProvider(
        LocalBgMain provides bgMainColor,
        LocalBgDark provides bgDarkColor,
        LocalAccentColor provides accentColor,
        LocalTextColor provides textColor,
        LocalBorderColor provides borderColor
    ) {
        val context = LocalContext.current
        val activity = context as? Activity
        
        LaunchedEffect(isFullscreen) {
            activity?.window?.let { window ->
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                if (isFullscreen) {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        if (settingsState.isSettingsOpen) {
            SettingsDialog(
                settingsState = settingsState,
                onClose = { settingsViewModel.toggleSettingsModal() },
                onSave = { 
                    settingsViewModel.updateSettings(it)
                    settingsViewModel.toggleSettingsModal()
                },
                onReset = { settingsViewModel.resetToDefaults() },
                currentThemeMain = bgMainColor,
                currentThemeDark = bgDarkColor,
                currentThemeAccent = accentColor
            )
        }

    if (vaultState.isVaultOpen) {
        VaultDialog(
            vaultState = vaultState,
            onClose = { vaultViewModel.toggleVaultModal() },
            onUnlock = { vaultViewModel.unlockVault(it) },
            onLock = { vaultViewModel.lockVault() },
            onAddPassword = { vaultViewModel.addVaultItem(it) },
            onDeletePassword = { vaultViewModel.deleteVaultItem(it) }
        )
    }

    if (syncState.isSyncOpen) {
        SyncDialog(
            syncState = syncState,
            onClose = { syncViewModel.toggleSyncModal() },
            onLogin = { u, p -> syncViewModel.login(u, p) },
            onRegister = { u, p -> syncViewModel.register(u, p) },
            onLogout = { syncViewModel.logout() },
            onPush = { syncViewModel.pushSync() },
            onPull = {
                syncViewModel.pullSync { _, _ ->
                    browserViewModel.loadLibrary()
                }
            }
        )
    }

    if (isTablet && !settings.compactMode) {
        Row(modifier = modifier.fillMaxSize().background(BgMain)) {
            if (settings.sidebarPos == "left" && !isFullscreen) {
                SidebarContent(
                    modifier = Modifier.width(260.dp),
                    uiState = uiState,
                    onNewTab = { browserViewModel.createNewTab("resource://android/assets/newtab.html?engine=${settings.searchEngine}") },
                    onNewIncognitoTab = { browserViewModel.createNewTab("resource://android/assets/newtab.html?engine=${settings.searchEngine}", isIncognito = true) },
                    onTabClick = { index -> browserViewModel.switchTab(index) },
                    onTabClose = { index -> browserViewModel.closeTab(index) },
                    onLibraryItemClick = { url -> browserViewModel.createNewTab(url) },
                    onDeleteBookmark = { index -> browserViewModel.deleteBookmark(index) },
                    onDeleteHistory = { index -> browserViewModel.deleteHistoryItem(index) },
                    onClearHistory = { browserViewModel.clearHistory() },
                    onOpenVault = { vaultViewModel.toggleVaultModal() },
                    onOpenSync = { syncViewModel.toggleSyncModal() },
                    onOpenSettings = { settingsViewModel.toggleSettingsModal() }
                )
            }
            MainContent(
                modifier = Modifier.weight(1f),
                url = typedUrl,
                uiState = uiState,
                onUrlChange = { typedUrl = it },
                onGoClick = { browserViewModel.loadUrlInActiveTab(typedUrl, settings.searchEngine) },
                onBackClick = { browserViewModel.goBackActiveTab() },
                onForwardClick = { browserViewModel.goForwardActiveTab() },
                onReloadClick = { browserViewModel.reloadActiveTab() },
                onToggleBookmark = { browserViewModel.toggleBookmark(typedUrl, activeTab?.title ?: typedUrl) },
                activeSession = activeTab?.session,
                isFullscreen = isFullscreen,
                onMenuClick = null // No menu button needed on tablet
            )
            if (settings.sidebarPos == "right" && !isFullscreen) {
                SidebarContent(
                    modifier = Modifier.width(260.dp),
                    uiState = uiState,
                    onNewTab = { browserViewModel.createNewTab("resource://android/assets/newtab.html?engine=${settings.searchEngine}") },
                    onNewIncognitoTab = { browserViewModel.createNewTab("resource://android/assets/newtab.html?engine=${settings.searchEngine}", isIncognito = true) },
                    onTabClick = { index -> browserViewModel.switchTab(index) },
                    onTabClose = { index -> browserViewModel.closeTab(index) },
                    onLibraryItemClick = { url -> browserViewModel.createNewTab(url) },
                    onDeleteBookmark = { index -> browserViewModel.deleteBookmark(index) },
                    onDeleteHistory = { index -> browserViewModel.deleteHistoryItem(index) },
                    onClearHistory = { browserViewModel.clearHistory() },
                    onOpenVault = { vaultViewModel.toggleVaultModal() },
                    onOpenSync = { syncViewModel.toggleSyncModal() },
                    onOpenSettings = { settingsViewModel.toggleSettingsModal() }
                )
            }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = BgDark,
                    modifier = Modifier.width(280.dp)
                ) {
                    SidebarContent(
                        modifier = Modifier.fillMaxSize(),
                        uiState = uiState,
                        onNewTab = { 
                            browserViewModel.createNewTab("resource://android/assets/newtab.html?engine=${settings.searchEngine}")
                            scope.launch { drawerState.close() }
                        },
                        onNewIncognitoTab = {
                            browserViewModel.createNewTab("resource://android/assets/newtab.html?engine=${settings.searchEngine}", isIncognito = true)
                            scope.launch { drawerState.close() }
                        },
                        onTabClick = { index -> 
                            browserViewModel.switchTab(index)
                            scope.launch { drawerState.close() }
                        },
                        onTabClose = { index -> browserViewModel.closeTab(index) },
                        onLibraryItemClick = { url -> 
                            browserViewModel.createNewTab(url)
                            scope.launch { drawerState.close() }
                        },
                        onDeleteBookmark = { index -> browserViewModel.deleteBookmark(index) },
                        onDeleteHistory = { index -> browserViewModel.deleteHistoryItem(index) },
                        onClearHistory = { browserViewModel.clearHistory() },
                        onOpenVault = { 
                            vaultViewModel.toggleVaultModal()
                            scope.launch { drawerState.close() }
                        },
                        onOpenSync = {
                            syncViewModel.toggleSyncModal()
                            scope.launch { drawerState.close() }
                        },
                        onOpenSettings = {
                            settingsViewModel.toggleSettingsModal()
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            MainContent(
                modifier = modifier.fillMaxSize(),
                url = typedUrl,
                uiState = uiState,
                onUrlChange = { typedUrl = it },
                onGoClick = { browserViewModel.loadUrlInActiveTab(typedUrl, settings.searchEngine) },
                onBackClick = { browserViewModel.goBackActiveTab() },
                onForwardClick = { browserViewModel.goForwardActiveTab() },
                onReloadClick = { browserViewModel.reloadActiveTab() },
                onToggleBookmark = { browserViewModel.toggleBookmark(typedUrl, activeTab?.title ?: typedUrl) },
                activeSession = activeTab?.session,
                isFullscreen = isFullscreen,
                onMenuClick = { scope.launch { drawerState.open() } }
            )
        }

        uiState.updateInfo?.let { updateInfo ->
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = { browserViewModel.dismissUpdateDialog() },
                title = { Text("Update Available: ${updateInfo.latestVersion}", color = TextColor) },
                text = { Text(updateInfo.releaseNotes, color = TextColor.copy(alpha = 0.8f)) },
                confirmButton = {
                    TextButton(onClick = {
                        UpdateDownloader.downloadAndInstall(context, updateInfo.downloadUrl, updateInfo.latestVersion)
                        browserViewModel.dismissUpdateDialog()
                    }) {
                        Text("Download & Install", color = AccentColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { browserViewModel.dismissUpdateDialog() }) {
                        Text("Later", color = TextColor)
                    }
                },
                containerColor = BgMain,
                titleContentColor = TextColor,
                textContentColor = TextColor
            )
        }
    }
    } // Close CompositionLocalProvider
}

@Composable
fun SidebarContent(
    modifier: Modifier = Modifier,
    uiState: BrowserState,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onTabClick: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    onLibraryItemClick: (String) -> Unit,
    onDeleteBookmark: (Int) -> Unit,
    onDeleteHistory: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var activeLibraryTab by remember { mutableStateOf("Bookmarks") }

    Column(
        modifier = modifier
            .background(BgDark)
            .systemBarsPadding()
            .padding(10.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        SidebarButton(
            icon = Icons.Default.Add, 
            text = "New Tab", 
            bgColor = TextColor.copy(alpha = 0.05f),
            onClick = onNewTab
        )
        SidebarButton(icon = Icons.Default.Lock, text = "Password Vault", bgColor = TextColor.copy(alpha = 0.05f), onClick = onOpenVault)
        SidebarButton(icon = Icons.Outlined.VisibilityOff, text = "Incognito", bgColor = TextColor.copy(alpha = 0.05f), onClick = onNewIncognitoTab)
        SidebarButton(icon = Icons.Outlined.Cloud, text = "Bodhi Sync", bgColor = Color(0x1A00FF64), textColor = Color(0xFF00FF64), onClick = onOpenSync)
        SidebarButton(icon = Icons.Outlined.Settings, text = "Settings", bgColor = TextColor.copy(alpha = 0.05f), onClick = onOpenSettings)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "OPEN TABS",
            color = TextColor.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 5.dp, bottom = 5.dp)
        )
        
        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
            itemsIndexed(uiState.tabs) { index, tab ->
                val isActive = index == uiState.activeTabIndex
                val bgColor by animateColorAsState(targetValue = if (isActive) Color(0x26FFFFFF) else Color.Transparent)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(bgColor)
                        .clickable { onTabClick(index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = TextColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tab.title.ifEmpty { tab.url },
                        color = TextColor,
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onTabClose(index) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Tab", tint = TextColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))

        // Library Tabs
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text(
                text = "Bookmarks",
                color = if (activeLibraryTab == "Bookmarks") Color.White else TextColor.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { activeLibraryTab = "Bookmarks" }.padding(4.dp)
            )
            Text(
                text = "History",
                color = if (activeLibraryTab == "History") Color.White else TextColor.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { activeLibraryTab = "History" }.padding(4.dp)
            )
        }

        val currentList = if (activeLibraryTab == "Bookmarks") uiState.bookmarks else uiState.history
        
        if (activeLibraryTab == "History" && currentList.isNotEmpty()) {
            Text(
                text = "Clear History",
                color = Color(0xFFFF6B6B),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClearHistory).padding(8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        if (currentList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "No ${activeLibraryTab.lowercase()} yet.", color = TextColor.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(currentList) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TextColor.copy(alpha = 0.05f))
                            .clickable { onLibraryItemClick(item.url) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title, color = TextColor, fontSize = 13.sp, maxLines = 1)
                            Text(text = item.url, color = TextColor.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1)
                        }
                        IconButton(
                            onClick = { 
                                if (activeLibraryTab == "Bookmarks") onDeleteBookmark(index) else onDeleteHistory(index) 
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        
        HorizontalDivider(color = BorderColor)
        
        // Footer
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.creator),
                contentDescription = "Creator",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Priyanshu Patel", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Creator", color = TextColor.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun SidebarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    bgColor: Color,
    textColor: Color = TextColor,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    url: String,
    uiState: BrowserState,
    onUrlChange: (String) -> Unit,
    onGoClick: () -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onReloadClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    activeSession: GeckoSession?,
    isFullscreen: Boolean,
    onMenuClick: (() -> Unit)?
) {
    val isBookmarked = uiState.bookmarks.any { it.url == url }
    
    val canGoBack = activeSession?.let { session ->
        uiState.tabs.find { it.session == session }?.canGoBack
    } == true
    
    BackHandler(enabled = canGoBack) {
        onBackClick()
    }

    Column(modifier = modifier.background(BgMain).systemBarsPadding()) {
        if (!isFullscreen) {
            // Titlebar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(BgMain)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextColor)
                }
            } else {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextColor)
                }
                IconButton(onClick = onForwardClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = TextColor)
                }
                IconButton(onClick = onReloadClick) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = TextColor)
                }
            }

            // Address Bar Container
            var isUrlFocused by remember { mutableStateOf(false) }
            val urlBorderColor by animateColorAsState(targetValue = if (isUrlFocused) AccentColor else Color.Transparent)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x33000000))
                    .border(1.dp, urlBorderColor, RoundedCornerShape(20.dp))
            ) {
                BasicTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    textStyle = TextStyle(color = TextColor, fontSize = 14.sp),
                    cursorBrush = SolidColor(TextColor),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .onFocusChanged { isUrlFocused = it.isFocused }
                        .padding(start = 20.dp, end = 70.dp, top = 10.dp, bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showShieldDropdown by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showShieldDropdown = true }, modifier = Modifier.size(32.dp)) {
                            Icon(androidx.compose.material.icons.Icons.Outlined.Security, contentDescription = "Shields", tint = AccentColor, modifier = Modifier.size(20.dp))
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showShieldDropdown,
                            onDismissRequest = { showShieldDropdown = false },
                            modifier = Modifier.background(BgDark)
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("B-Sync Shields ON", color = TextColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                onClick = { showShieldDropdown = false }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Powered by uBlock Origin", color = Color.Gray, fontSize = 12.sp) },
                                onClick = { showShieldDropdown = false }
                            )
                        }
                    }
                    IconButton(onClick = onToggleBookmark, modifier = Modifier.size(32.dp)) {
                        AnimatedContent(
                            targetState = isBookmarked,
                            transitionSpec = { fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150)) },
                            label = "bookmark_anim"
                        ) { bookmarked ->
                            Icon(
                                if (bookmarked) Icons.Default.Star else Icons.Outlined.StarBorder, 
                                contentDescription = "Bookmark", 
                                tint = if (bookmarked) Color(0xFFFFD700) else TextColor, 
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onGoClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Go", color = Color.White)
            }
        }
        
        HorizontalDivider(color = BorderColor)
        } // end of if(!isFullscreen)

        // Browser
        if (activeSession != null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                BrowserView(activeSession = activeSession)
            }
        } else {
            // Empty state when no tabs are open
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No tabs open. Click 'New Tab' to start browsing.", color = Color(0x80FFFFFF))
            }
        }
    }
}

@Composable
fun BrowserView(activeSession: GeckoSession) {
    val context = LocalContext.current
    val geckoView = remember {
        GeckoView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { geckoView },
        update = { view ->
            view.setSession(activeSession)
        },
        onRelease = { view ->
            view.releaseSession()
        }
    )
}
