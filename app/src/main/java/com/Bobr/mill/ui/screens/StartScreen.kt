package com.Bobr.mill.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.Bobr.mill.R
import com.Bobr.mill.data.network.DiscoveredGame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    initialName: String,
    statusMessage: String,
    discoveredGames: List<DiscoveredGame>,
    onHostStart: (String, Boolean) -> Unit,
    onJoinStart: () -> Unit,
    onGameSelect: (String, String) -> Unit,
    onLocalPlay: () -> Unit,
    onCancel: () -> Unit
) {
    var playerName by remember { mutableStateOf(initialName) }
    var showHostDialog by remember { mutableStateOf(false) } // NEW: Controls the popup

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Mill by Bobr",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xBB000000)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("Player Name", color = Color.LightGray) },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.White,
                            unfocusedIndicatorColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Open the popup instead of directly starting
                    Button(
                        onClick = { showHostDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Host Game", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onJoinStart,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("Join Game", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onLocalPlay,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E9E9E))
                    ) {
                        Text("Local Play", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x99000000)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusMessage,
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (discoveredGames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Available Games:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x88000000))
                ) {
                    items(discoveredGames) { game ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGameSelect(game.endpointId, playerName) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = game.hostName, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            Text(text = "TAP TO JOIN", color = Color(0xFF4CAF50), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color.DarkGray)
                    }
                }
            }

            if (statusMessage.contains("Hosting") || statusMessage.contains("Discovering") || statusMessage.contains("Connecting")) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        }

        // --- NEW: HOST SETTINGS POPUP ---
        if (showHostDialog) {
            Dialog(onDismissRequest = { showHostDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE111111))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Choose your color",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // White Button
                            Button(
                                onClick = {
                                    showHostDialog = false
                                    onHostStart(playerName, true) // Pass true for White
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text("White", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            // Black Button
                            Button(
                                onClick = {
                                    showHostDialog = false
                                    onHostStart(playerName, false) // Pass false for Black
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text("Black", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = { showHostDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}