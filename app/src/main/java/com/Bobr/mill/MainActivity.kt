package com.Bobr.mill

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.Bobr.mill.data.DataManager
import com.Bobr.mill.data.network.DiscoveredGame
import com.Bobr.mill.data.network.NearbyConnectionsManager
import com.Bobr.mill.domain.models.Player
import com.Bobr.mill.ui.screens.GameScreen
import com.Bobr.mill.ui.screens.StartScreen
import com.Bobr.mill.ui.theme.MillTheme
import com.Bobr.mill.ui.viewmodels.GameEvent
import com.Bobr.mill.ui.viewmodels.MillViewModel
import com.Bobr.mill.utils.SoundManager
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: MillViewModel by viewModels()
    private var nearbyManager: NearbyConnectionsManager? = null

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataManager = DataManager(this)
        soundManager = SoundManager(this, dataManager)

        setContent {
            var isConnected by remember { mutableStateOf(false) }
            var statusMessage by remember { mutableStateOf("") }

            // Für das verschwindende Popup und die Quit-Erkennung
            var transientMessage by remember { mutableStateOf("") }
            var intentionalDisconnect by remember { mutableStateOf(false) }

            var discoveredGames by remember { mutableStateOf<List<DiscoveredGame>>(emptyList()) }
            var currentOpponentName by remember { mutableStateOf("Unknown Player") }

            // Timer: Lässt das Popup nach 3 Sekunden verschwinden
            LaunchedEffect(transientMessage) {
                if (transientMessage.isNotEmpty()) {
                    delay(3000)
                    transientMessage = ""
                }
            }

            LaunchedEffect(isConnected) {
                if (!isConnected) {
                    viewModel.onEvent(GameEvent.OnResetClicked)
                } else if (!viewModel.isLocalMode) {
                    viewModel.startRps()
                }
            }

            LaunchedEffect(Unit) {
                nearbyManager = NearbyConnectionsManager(
                    context = this@MainActivity,
                    onConnectionStatusChanged = { connected, message ->
                        isConnected = connected
                        if (!connected) {
                            if (intentionalDisconnect) {
                                // Wenn wir selbst auf Quit oder Cancel gedrückt haben, ignorieren wir die Meldung!
                                intentionalDisconnect = false
                                statusMessage = ""
                            } else if (message.contains("Hosting", true) || message.contains("Discovering", true)) {
                                statusMessage = message
                            } else {
                                statusMessage = ""
                                transientMessage = message
                            }
                        } else {
                            statusMessage = ""
                        }
                    },
                    onMoveReceived = { receivedMove ->
                        when (receivedMove) {
                            -1 -> viewModel.onEvent(GameEvent.OnResetClicked)
                            -2 -> {
                                val oppRole = if (viewModel.myLocalRole.value == Player.PLAYER_ONE) Player.PLAYER_TWO else Player.PLAYER_ONE
                                viewModel.concedeGame(oppRole)
                            }
                            -10 -> viewModel.setOpponentRpsChoice(1)
                            -11 -> viewModel.setOpponentRpsChoice(2)
                            -12 -> viewModel.setOpponentRpsChoice(3)
                            -20 -> viewModel.applyColorChoice(opponentChoseWhite = true)
                            -21 -> viewModel.applyColorChoice(opponentChoseWhite = false)
                            else -> viewModel.onEvent(GameEvent.OnNetworkMoveReceived(receivedMove))
                        }
                    },
                    onRoleAssigned = { isPlayerOne ->
                        val role = if (isPlayerOne) Player.PLAYER_ONE else Player.PLAYER_TWO
                        viewModel.setLocalRole(role)
                    },
                    onGamesDiscovered = { discoveredGames = it },
                    onOpponentNameReceived = { name ->
                        currentOpponentName = name
                    }
                )
            }

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
                        val currentState by viewModel.state.collectAsState()
                        val myRole by viewModel.myLocalRole.collectAsState()

                        LaunchedEffect(currentState.winner) {
                            val winner = currentState.winner
                            if (!viewModel.isLocalMode && winner != null && winner != Player.NONE) {
                                val didIWin = (winner == myRole)
                                val p1LostCount = 9 - (currentState.piecesOnBoardPlayerOne + currentState.unplacedPiecesPlayerOne)
                                val p2LostCount = 9 - (currentState.piecesOnBoardPlayerTwo + currentState.unplacedPiecesPlayerTwo)
                                val myLost = if (myRole == Player.PLAYER_ONE) p1LostCount else p2LostCount
                                val myTaken = if (myRole == Player.PLAYER_ONE) p2LostCount else p1LostCount

                                dataManager.recordMatch(currentOpponentName, didIWin, myTaken, myLost)
                            }
                        }

                        GameScreen(
                            viewModel = viewModel,
                            soundManager = soundManager,
                            onPointClicked = { pointIndex ->
                                val wasValid = viewModel.tryLocalMove(pointIndex)
                                if (wasValid && !viewModel.isLocalMode) {
                                    nearbyManager?.sendMove(pointIndex)
                                }
                            },
                            onQuitGame = {
                                intentionalDisconnect = true
                                if (!viewModel.isLocalMode) {
                                    nearbyManager?.disconnect()
                                }
                                isConnected = false
                                statusMessage = ""
                            },
                            onConcede = {
                                val myRoleLocal = viewModel.myLocalRole.value
                                viewModel.concedeGame(myRoleLocal)
                                if (!viewModel.isLocalMode) {
                                    nearbyManager?.sendMove(-2)
                                }
                            },
                            onRematchRequested = {
                                viewModel.onEvent(GameEvent.OnResetClicked)
                                if (!viewModel.isLocalMode) {
                                    nearbyManager?.sendMove(-1)
                                }
                            },
                            onSendSignal = { signal ->
                                if (!viewModel.isLocalMode) nearbyManager?.sendMove(signal)
                            }
                        )
                    } else {
                        StartScreen(
                            dataManager = dataManager,
                            soundManager = soundManager,
                            statusMessage = statusMessage,
                            transientMessage = transientMessage, // Hier wird es jetzt sauber übergeben!
                            discoveredGames = discoveredGames,
                            onHostStart = { name, isWhite ->
                                viewModel.isLocalMode = false
                                nearbyManager?.startHosting(name, isWhite)
                            },
                            onJoinStart = {
                                viewModel.isLocalMode = false
                                nearbyManager?.startDiscovering()
                            },
                            onGameSelect = { id, name ->
                                viewModel.isLocalMode = false
                                nearbyManager?.joinGame(id, name)
                            },
                            onLocalPlay = {
                                viewModel.isLocalMode = true
                                viewModel.setLocalRole(Player.NONE)
                                nearbyManager?.cancel()
                                isConnected = true
                            },
                            onCancel = {
                                intentionalDisconnect = true // Verhindert falsche Popups beim Abbrechen der Suche
                                nearbyManager?.cancel()
                                statusMessage = ""
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::soundManager.isInitialized) {
            soundManager.startMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::soundManager.isInitialized) {
            soundManager.pauseMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nearbyManager?.disconnect()
        if (::soundManager.isInitialized) {
            soundManager.release()
        }
    }
}