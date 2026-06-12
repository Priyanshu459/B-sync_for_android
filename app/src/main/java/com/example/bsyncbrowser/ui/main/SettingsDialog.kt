package com.example.bsyncbrowser.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SettingsDialog(
    settingsState: SettingsState,
    onClose: () -> Unit,
    onSave: (BrowserSettings) -> Unit,
    onReset: () -> Unit,
    currentThemeMain: Color,
    currentThemeDark: Color,
    currentThemeAccent: Color
) {
    var searchEngine by remember { mutableStateOf(settingsState.settings.searchEngine) }
    var compactMode by remember { mutableStateOf(settingsState.settings.compactMode) }
    var sidebarPos by remember { mutableStateOf(settingsState.settings.sidebarPos) }
    var themeBgMain by remember { mutableStateOf(settingsState.settings.themeBgMain) }
    var themeBgDark by remember { mutableStateOf(settingsState.settings.themeBgDark) }
    var themeAccent by remember { mutableStateOf(settingsState.settings.themeAccent) }

    val isLight = currentThemeMain.red > 0.8f
    val textColor = if (isLight) Color(0xFF1F2937) else Color(0xE6FFFFFF)
    val dividerColor = if (isLight) Color(0x1A000000) else Color(0x1AFFFFFF)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentThemeMain)
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Settings & Theming", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                    }
                }
                
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 10.dp))

                // Search Engine
                Text("Search Engine", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.padding(vertical = 8.dp).horizontalScroll(rememberScrollState())) {
                    listOf("google", "bing", "duckduckgo", "brave").forEach { engine ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { searchEngine = engine }
                        ) {
                            RadioButton(
                                selected = searchEngine == engine,
                                onClick = { searchEngine = engine },
                                colors = RadioButtonDefaults.colors(selectedColor = currentThemeAccent, unselectedColor = textColor.copy(alpha = 0.5f))
                            )
                            Text(engine.replaceFirstChar { it.uppercase() }, color = textColor, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // UI Layout
                Text("UI Layout", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = compactMode,
                        onCheckedChange = { compactMode = it },
                        colors = CheckboxDefaults.colors(checkedColor = currentThemeAccent, uncheckedColor = textColor.copy(alpha = 0.5f))
                    )
                    Text("Compact Mode", color = textColor, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sidebar Position: ", color = textColor, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { sidebarPos = if (sidebarPos == "left") "right" else "left" }, colors = ButtonDefaults.buttonColors(containerColor = dividerColor), modifier = Modifier.height(32.dp)) {
                        Text(sidebarPos.replaceFirstChar { it.uppercase() }, color = textColor, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Theming
                Text("Zen Theming", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    ThemeCircle(Color(0xFF1E1E1E), "Dark", { themeBgMain = 0xFF1E1E1E; themeBgDark = 0xFF0F0F0F; themeAccent = 0xFF3B82F6 }, currentThemeMain)
                    ThemeCircle(Color(0xFFE5E7EB), "Light", { themeBgMain = 0xFFE5E7EB; themeBgDark = 0xFFF3F4F6; themeAccent = 0xFF10B981 }, currentThemeMain)
                    ThemeCircle(Color(0xFF1E1B4B), "Midnight", { themeBgMain = 0xFF1E1B4B; themeBgDark = 0xFF0F172A; themeAccent = 0xFF8B5CF6 }, currentThemeMain)
                    ThemeCircle(Color(0xFF064E3B), "Forest", { themeBgMain = 0xFF064E3B; themeBgDark = 0xFF022C22; themeAccent = 0xFF34D399 }, currentThemeMain)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = onReset,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Reset to Defaults", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            onSave(BrowserSettings(
                                searchEngine = searchEngine,
                                compactMode = compactMode,
                                sidebarPos = sidebarPos,
                                themeBgMain = themeBgMain,
                                themeBgDark = themeBgDark,
                                themeAccent = themeAccent
                            ))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentThemeAccent)
                    ) {
                        Text("Save & Apply", color = Color.White) // Keep white on colored button
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeCircle(color: Color, name: String, onClick: () -> Unit, currentBg: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, color = if (currentBg.red > 0.8f) Color.Black else Color.White, fontSize = 10.sp)
    }
}
