package com.example.bsyncbrowser.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun VaultDialog(
    vaultState: VaultState,
    onClose: () -> Unit,
    onUnlock: (String) -> Unit,
    onLock: () -> Unit,
    onAddPassword: (VaultItem) -> Unit,
    onDeletePassword: (Int) -> Unit
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
                            if (vaultState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Vault",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Encrypted Vault", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 10.dp))

                if (vaultState.isLocked) {
                    // Unlock Screen
                    var password by remember { mutableStateOf("") }
                    
                    Text("Enter Master Password", color = Color(0x80FFFFFF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    
                    if (vaultState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(vaultState.error, color = Color.Red, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onUnlock(password) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                    ) {
                        Text("Unlock", color = Color.White)
                    }
                } else {
                    // Vault Dashboard
                    Button(
                        onClick = onLock,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        modifier = Modifier.align(Alignment.End).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("Lock Vault", color = Color.White, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Add new password
                    var site by remember { mutableStateOf("") }
                    var username by remember { mutableStateOf("") }
                    var pwd by remember { mutableStateOf("") }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgDark)
                            .padding(12.dp)
                    ) {
                        Text("Add New Password", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(value = site, onValueChange = { site = it }, label = { Text("Site (e.g. google.com)", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth().height(50.dp), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth().height(50.dp), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = pwd, onValueChange = { pwd = it }, label = { Text("Password", fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth().height(50.dp), singleLine = true, visualTransformation = PasswordVisualTransformation(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White))
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (site.isNotBlank() && username.isNotBlank() && pwd.isNotBlank()) {
                                    onAddPassword(VaultItem(site, username, pwd))
                                    site = ""
                                    username = ""
                                    pwd = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("Save Entry", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Stored Passwords", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val clipboardManager = LocalClipboardManager.current
                    
                    LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 200.dp)) {
                        itemsIndexed(vaultState.items) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.site, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(item.username, color = Color(0x80FFFFFF), fontSize = 11.sp)
                                }
                                IconButton(onClick = { clipboardManager.setText(AnnotatedString(item.password)) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDeletePassword(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
