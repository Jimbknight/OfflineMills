package com.Bobr.mill.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Bobr.mill.data.network.DiscoveredGame

enum class LobbyState { MENU, HOST_SETUP, HOSTING, DISCOVERING }

@Composable
fun StartScreen(
    initialName: String,
    statusMessage: String,
    discoveredGames: List<DiscoveredGame>,
    onHostStart: (name: String, isWhite: Boolean) -> Unit,
    onJoinStart: () -> Unit,
    onGameSelect: (endpointId: String, playerName: String) -> Unit,
    onLocalPlay: () -> Unit, // NEU: Callback für lokales Spiel
    onCancel: () -> Unit
) {
    var playerName by remember { mutableStateOf(initialName) }
    var lobbyState by remember { mutableStateOf(LobbyState.MENU) }
    var hostWantsWhite by remember { mutableStateOf(true) }

    LaunchedEffect(statusMessage) {
        if (statusMessage == "Verbindung verloren!" || statusMessage == "Verbindung getrennt") {
            lobbyState = LobbyState.MENU
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mühle Offline",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (statusMessage.isNotEmpty() && lobbyState == LobbyState.MENU) {
            Text(
                text = statusMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        when (lobbyState) {
            LobbyState.MENU -> {
                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    label = { Text("Dein Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.8f).padding(bottom = 24.dp)
                )

                Button(
                    onClick = { lobbyState = LobbyState.HOST_SETUP },
                    modifier = Modifier.fillMaxWidth(0.8f).padding(bottom = 12.dp)
                ) {
                    Text("Spiel Hosten")
                }

                Button(
                    onClick = {
                        lobbyState = LobbyState.DISCOVERING
                        onJoinStart()
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).padding(bottom = 12.dp)
                ) {
                    Text("Spiele suchen")
                }

                // DER LOKALE BUTTON
                OutlinedButton(
                    onClick = onLocalPlay,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Lokal gegen Freund spielen")
                }
            }

            LobbyState.HOST_SETUP -> {
                Text("Deine Farbe:", fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                Row(modifier = Modifier.padding(bottom = 24.dp)) {
                    RadioButton(selected = hostWantsWhite, onClick = { hostWantsWhite = true })
                    Text("Weiß", modifier = Modifier.align(Alignment.CenterVertically).padding(end = 16.dp))

                    RadioButton(selected = !hostWantsWhite, onClick = { hostWantsWhite = false })
                    Text("Schwarz", modifier = Modifier.align(Alignment.CenterVertically))
                }

                Button(
                    onClick = {
                        lobbyState = LobbyState.HOSTING
                        onHostStart(playerName, hostWantsWhite)
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).padding(bottom = 16.dp)
                ) {
                    Text("Lobby eröffnen")
                }
                TextButton(onClick = { lobbyState = LobbyState.MENU }) { Text("Zurück") }
            }

            LobbyState.HOSTING -> {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                Text("Warte auf Mitspieler...", modifier = Modifier.padding(bottom = 24.dp))
                Button(onClick = {
                    onCancel()
                    lobbyState = LobbyState.MENU
                }) { Text("Abbrechen") }
            }

            LobbyState.DISCOVERING -> {
                Text("Verfügbare Spiele:", fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

                LazyColumn(modifier = Modifier.weight(1f, fill = false).fillMaxWidth(0.9f)) {
                    items(discoveredGames) { game ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onGameSelect(game.endpointId, playerName) }
                        ) {
                            Text("Host: ${game.hostName}", modifier = Modifier.padding(16.dp), fontSize = 18.sp)
                        }
                    }
                }

                if (discoveredGames.isEmpty()) {
                    Text("Suche...", color = Color.Gray, modifier = Modifier.padding(vertical = 24.dp))
                }

                Button(
                    onClick = {
                        onCancel()
                        lobbyState = LobbyState.MENU
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) { Text("Abbrechen") }
            }
        }
    }
}