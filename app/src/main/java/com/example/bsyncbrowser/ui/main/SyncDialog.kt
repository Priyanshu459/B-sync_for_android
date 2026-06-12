package com.example.bsyncbrowser.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SyncDialog(
    syncState: SyncState,
    onClose: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onLogout: () -> Unit,
    onPush: () -> Unit,
    onPull: () -> Unit
) {
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
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgMain)
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
                            Icons.Outlined.Cloud,
                            contentDescription = "Sync",
                            tint = Color(0xFF00FF64),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Bodhi Sync", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 10.dp))

                if (syncState.token == null) {
                    // Auth View
                    var username by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", fontSize = 12.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
                    )
                    
                    if (syncState.statusMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncState.statusMessage, 
                            color = if (syncState.isError) Color(0xFFFF4444) else Color(0xFF00FF66), 
                            fontSize = 12.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = { onRegister(username, password) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            enabled = !syncState.isLoading
                        ) {
                            Text("Register", color = Color.White)
                        }
                        Button(
                            onClick = { onLogin(username, password) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            enabled = !syncState.isLoading
                        ) {
                            Text("Login", color = Color.White)
                        }
                    }
                } else {
                    // Dashboard View
                    Text("Logged in as: ", color = Color(0x80FFFFFF), fontSize = 14.sp)
                    Text(syncState.username ?: "", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onPush,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !syncState.isLoading
                    ) {
                        Text("Push to Cloud (Backup)", color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onPull,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !syncState.isLoading
                    ) {
                        Text("Pull from Cloud (Restore)", color = Color.White)
                    }

                    if (syncState.statusMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = syncState.statusMessage, 
                            color = if (syncState.isError) Color(0xFFFF4444) else Color(0xFF00FF66), 
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        enabled = !syncState.isLoading
                    ) {
                        Text("Log Out", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
