package com.Bobr.mill

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.Bobr.mill.data.network.DiscoveredGame
import com.Bobr.mill.data.network.NearbyConnectionsManager
import com.Bobr.mill.domain.models.Player
import com.Bobr.mill.ui.screens.GameScreen
import com.Bobr.mill.ui.screens.StartScreen
import com.Bobr.mill.ui.theme.MillTheme
import com.Bobr.mill.ui.viewmodels.GameEvent
import com.Bobr.mill.ui.viewmodels.MillViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MillViewModel by viewModels()
    private var nearbyManager: NearbyConnectionsManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialPlayerName = viewModel.generateRandomName()

        setContent {
            var isConnected by remember { mutableStateOf(false) }
            var statusMessage by remember { mutableStateOf("") }
            var discoveredGames by remember { mutableStateOf<List<DiscoveredGame>>(emptyList()) }

            // ZOMBIE-STATE FIX: Wenn isConnected auf false wechselt, Brett sauber auf 0 setzen!
            LaunchedEffect(isConnected) {
                if (!isConnected) {
                    viewModel.onEvent(GameEvent.OnResetClicked)
                }
            }

            LaunchedEffect(Unit) {
                nearbyManager = NearbyConnectionsManager(
                    context = this@MainActivity,
                    onConnectionStatusChanged = { connected, message ->
                        isConnected = connected
                        statusMessage = message
                    },
                    onMoveReceived = { viewModel.onEvent(GameEvent.OnNetworkMoveReceived(it)) },
                    onRoleAssigned = { isPlayerOne ->
                        val role = if (isPlayerOne) Player.PLAYER_ONE else Player.PLAYER_TWO
                        viewModel.setLocalRole(role)
                    },
                    onGamesDiscovered = { discoveredGames = it }
                )
            }

            // Berechtigungen...
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
            LaunchedEffect(key1 = true) { permissionLauncher.launch(permissionsToRequest) }

            MillTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isConnected) {
                        GameScreen(
                            viewModel = viewModel,
                            onPointClicked = { pointIndex ->
                                val wasValid = viewModel.tryLocalMove(pointIndex)
                                if (wasValid && !viewModel.isLocalMode) {
                                    nearbyManager?.sendMove(pointIndex)
                                }
                            },
                            onQuitGame = {
                                // Wenn der Rote Button gedrückt wird:
                                if (!viewModel.isLocalMode) {
                                    nearbyManager?.disconnect() // Schmeißt den anderen Spieler raus
                                }
                                isConnected = false // Wechselt zum Menü & triggert den Reset!
                            }
                        )
                    } else {
                        StartScreen(
                            initialName = initialPlayerName,
                            statusMessage = statusMessage,
                            discoveredGames = discoveredGames,
                            onHostStart = { name, isWhite ->
                                viewModel.isLocalMode = false
                                nearbyManager?.startHosting(name, isWhite)
                            },
                            onJoinStart = {
                                viewModel.isLocalMode = false
                                nearbyManager?.startDiscovering()
                            },
                            onGameSelect = { id, name -> nearbyManager?.joinGame(id, name) },
                            onLocalPlay = {
                                viewModel.isLocalMode = true
                                viewModel.setLocalRole(Player.NONE)
                                isConnected = true
                            },
                            onCancel = { nearbyManager?.cancel() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nearbyManager?.disconnect()
    }
}