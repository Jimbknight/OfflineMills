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
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.Player as GmsPlayer
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents

class MainActivity : ComponentActivity() {
    private val viewModel: MillViewModel by viewModels()
    private var nearbyManager: NearbyConnectionsManager? = null

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataManager = DataManager(this)
        soundManager = SoundManager(this, dataManager)
// Prüfen, ob der Spieler durch PGS v2 im Hintergrund eingeloggt wurde
        PlayGames.getGamesSignInClient(this).isAuthenticated.addOnCompleteListener { isAuthenticatedTask ->
            val isAuthenticated = if (isAuthenticatedTask.isSuccessful) {
                isAuthenticatedTask.result.isAuthenticated
            } else {
                false
            }

            if (isAuthenticated) {
                Log.d("PlayGames", "Erfolgreich eingeloggt!")

                // Wir holen uns das Spieler-Profil
                // Wir holen uns das Spieler-Profil
                PlayGames.getPlayersClient(this).currentPlayer.addOnCompleteListener { playerTask ->
                    if (playerTask.isSuccessful) {
                        val player: GmsPlayer? = playerTask.result
                        val googlePlayName = player?.displayName ?: "Guest"

                        Log.d("PlayGames", "Willkommen, $googlePlayName!")

                        // 1. Namen im Speicher überschreiben
                        dataManager.playerName = googlePlayName
                        // 2. Das Schloss aktivieren (Name darf nicht mehr geändert werden)
                        dataManager.isGoogleLogin = true

                        // Cloud-Daten laden...
                        val cloudManager = com.Bobr.mill.data.CloudSaveManager(this@MainActivity, dataManager)
                        cloudManager.loadFromCloud()
                    } else  {
                    Log.d("PlayGames", "Nicht eingeloggt. Nutze lokalen Offline-Namen.")
                    dataManager.isGoogleLogin = false // Offline = Name darf geändert werden!
                    }
                }
            } else {
                Log.d("PlayGames", "Nicht eingeloggt. Nutze lokalen Offline-Namen.")
                // Hier greift dein Fallback (z.B. der Name, den der User selbst eingetippt hat)
            }


        }
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

            val unlockedAchievements = remember { mutableStateListOf<String>() }

            LaunchedEffect(unlockedAchievements.size) {
                if (unlockedAchievements.isNotEmpty()) {
                    delay(3500) // Show popup for 3.5 seconds
                    unlockedAchievements.removeAt(0)
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isConnected) {
                            val currentState by viewModel.state.collectAsState()
                            val myRole by viewModel.myLocalRole.collectAsState()

                            LaunchedEffect(currentState.winner) {
                                val winner = currentState.winner
                                if (winner != null && winner != Player.NONE && (!viewModel.isLocalMode || viewModel.isBotMode)) {
                                    val didIWin = (winner == myRole)
                                    val p1LostCount =
                                        9 - (currentState.piecesOnBoardPlayerOne + currentState.unplacedPiecesPlayerOne)
                                    val p2LostCount =
                                        9 - (currentState.piecesOnBoardPlayerTwo + currentState.unplacedPiecesPlayerTwo)
                                    val myLost =
                                        if (myRole == Player.PLAYER_ONE) p1LostCount else p2LostCount
                                    val myTaken =
                                        if (myRole == Player.PLAYER_ONE) p2LostCount else p1LostCount

                                    val opponentNameToSave =
                                        if (viewModel.isBotMode) "Bot (Lvl ${viewModel.botDifficulty.ordinal + 1})" else currentOpponentName
                                    dataManager.recordMatch(
                                        opponentNameToSave,
                                        didIWin,
                                        myTaken,
                                        myLost
                                    )

// --- SMART POPUP LOGIC ---
                                    val totalMatchesNow = dataManager.totalWins + dataManager.totalLosses

                                    // Matches Tier Popups
                                    if (totalMatchesNow == 1) unlockedAchievements.add("First Steps")
                                    if (totalMatchesNow == 50) unlockedAchievements.add("Veteran")
                                    if (totalMatchesNow == 100) unlockedAchievements.add("Enthusiast")
                                    if (totalMatchesNow == 500) unlockedAchievements.add("Legend")

                                    // Pieces Tier Popups
                                    if (dataManager.totalPiecesTaken >= 50 && (dataManager.totalPiecesTaken - myTaken) < 50) unlockedAchievements.add("Mill Builder")
                                    if (dataManager.totalPiecesTaken >= 250 && (dataManager.totalPiecesTaken - myTaken) < 250) unlockedAchievements.add("Lumberjack")
                                    if (dataManager.totalPiecesTaken >= 500 && (dataManager.totalPiecesTaken - myTaken) < 500) unlockedAchievements.add("Executioner")
                                    if (dataManager.totalPiecesTaken >= 1000 && (dataManager.totalPiecesTaken - myTaken) < 1000) unlockedAchievements.add("Terminator")

                                    if (didIWin) {
                                        // Standalone Popups
                                        if (myLost <= 3 && !dataManager.achUntouchable) {
                                            dataManager.achUntouchable = true
                                            unlockedAchievements.add("Untouchable")
                                        }
                                        if (myLost == 0 && !dataManager.achPerfection) {
                                            dataManager.achPerfection = true
                                            unlockedAchievements.add("Perfection")
                                        }
                                        if (myLost == 6 && !dataManager.achComeback) {
                                            dataManager.achComeback = true
                                            unlockedAchievements.add("Comeback")
                                        }
                                        if (currentState.infoMessage.contains("trapped", ignoreCase = true) && !dataManager.achJailer) {
                                            dataManager.achJailer = true
                                            unlockedAchievements.add("Jailer")
                                        }

                                        // Bot Slayer Tier Popups
                                        if (viewModel.isBotMode) {
                                            if (viewModel.botDifficulty == com.Bobr.mill.domain.engine.BotDifficulty.MEDIUM && !dataManager.achChallenger) {
                                                dataManager.achChallenger = true
                                                unlockedAchievements.add("Challenger")
                                            } else if (viewModel.botDifficulty == com.Bobr.mill.domain.engine.BotDifficulty.HARD && !dataManager.achGrandmaster) {
                                                dataManager.achGrandmaster = true
                                                unlockedAchievements.add("Grandmaster")
                                            }
                                        }
                                    }

                                    if (dataManager.isGoogleLogin) {
                                        com.Bobr.mill.data.CloudSaveManager(
                                            this@MainActivity,
                                            dataManager
                                        ).saveToCloud()
                                    }
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
                                    intentionalDisconnect =
                                        true // Verhindert falsche Popups beim Abbrechen der Suche
                                    nearbyManager?.cancel()
                                    statusMessage = ""
                                },
                                onBotStart = { difficultyLvl ->
                                    viewModel.isLocalMode = true
                                    viewModel.isBotMode = true
                                    viewModel.botDifficulty = when (difficultyLvl) {
                                        1 -> com.Bobr.mill.domain.engine.BotDifficulty.EASY
                                        2 -> com.Bobr.mill.domain.engine.BotDifficulty.MEDIUM
                                        else -> com.Bobr.mill.domain.engine.BotDifficulty.HARD
                                    }
                                    viewModel.startRps() // Startet Schere-Stein-Papier gegen den Bot!
                                    isConnected = true
                                },

                                )
                        }
                        // --- THE NEW ACHIEVEMENT OVERLAY ---
                        androidx.compose.animation.AnimatedVisibility(
                            visible = unlockedAchievements.isNotEmpty(),
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(
                                initialOffsetY = { -it }),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(
                                targetOffsetY = { -it }),
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.TopCenter)
                                .padding(top = 48.dp)
                        ) {
                            if (unlockedAchievements.isNotEmpty()) {
                                androidx.compose.material3.Card(
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = Color(0xEE3E2723)
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    modifier = Modifier.border(
                                        2.dp,
                                        Color(0xFFFFD700),
                                        androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 24.dp,
                                            vertical = 12.dp
                                        ),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.EmojiEvents,
                                            contentDescription = "Achievement",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            androidx.compose.material3.Text(
                                                "Achievement Unlocked!",
                                                color = Color.LightGray,
                                                fontSize = 12.sp
                                            )
                                            androidx.compose.material3.Text(
                                                text = unlockedAchievements.first(),
                                                color = Color.White,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
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