package com.Bobr.mill.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.Bobr.mill.R
import com.Bobr.mill.data.network.DiscoveredGame
import com.Bobr.mill.data.DataManager
import com.Bobr.mill.utils.SoundManager
import androidx.compose.animation.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlin.collections.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.shape.CircleShape

@Composable
fun PremiumButton(
    text: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    dataManager: DataManager,
    soundManager: SoundManager,
    statusMessage: String,
    transientMessage: String, // <-- NEU
    discoveredGames: List<DiscoveredGame>,
    onHostStart: (String, Boolean) -> Unit,
    onJoinStart: () -> Unit,
    onGameSelect: (String, String) -> Unit,
    onLocalPlay: () -> Unit,
    onCancel: () -> Unit,
    onBotStart: (Int) -> Unit
) {
    var playerName by remember { mutableStateOf(dataManager.playerName) }
    val livePlayerName by dataManager.playerNameFlow.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) } // NEU: Steuert das Join-Popup
    var selectedOpponent by remember { mutableStateOf<String?>(null) }
    var showBotDialog by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(1) } // 1=Leicht, 2=Mittel, 3=Schwer

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(top = 30.dp)
                .align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = livePlayerName.ifEmpty { "Guest" }, // HIER: livePlayerName statt playerName
                color = Color(0xFFD4AF37),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                    .background(Color(0x66000000), RoundedCornerShape(50))
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.mills_logo),
                contentDescription = "Game Logo",
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(min = 160.dp)
                    .padding(bottom = 32.dp),
                contentScale = ContentScale.FillWidth
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xAA3E2723), RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0x668D6E63), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PremiumButton(
                        text = "Host Game nearby",
                        icon = Icons.Default.Person,
                        gradientColors = listOf(Color(0xFF795548), Color(0xFF3E2723)),
                        onClick = { onHostStart(playerName, true) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumButton(
                        text = "Join Game",
                        icon = Icons.Default.Search,
                        gradientColors = listOf(Color(0xFF8D6E63), Color(0xFF4E342E)),
                        onClick = {
                            showJoinDialog = true
                            onJoinStart()
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumButton(
                        text = "Play vs Bot",
                        icon = Icons.Default.SmartToy,
                        gradientColors = listOf(Color(0xFF59453D), Color(0xFF3B2923)),
                        onClick = { showBotDialog = true }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumButton(
                        text = "Stats and Achievments",
                        icon = Icons.Default.Info,
                        gradientColors = listOf(Color(0xFF455A64), Color(0xFF263238)),
                        onClick = { showStatsDialog = true }
                    )
                }

                if (statusMessage.isNotEmpty() && !showJoinDialog) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xEE3E2723)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = statusMessage, color = Color(0xFFFFD700), fontWeight = FontWeight.Medium, modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }


        if (showJoinDialog) {
            Dialog(onDismissRequest = {
                showJoinDialog = false
                onCancel()
            }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE3E2723)),
                    modifier = Modifier.border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Available Games", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (discoveredGames.isEmpty()) {
                            CircularProgressIndicator(color = Color(0xFFD4AF37), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Searching for hosts...", color = Color.LightGray)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 250.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(8.dp))
                                    .background(Color(0xAA3E2723))
                            ) {
                                items(discoveredGames) { game ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showJoinDialog = false
                                                onGameSelect(game.endpointId, playerName)
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = game.hostName, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                                        Text(text = "JOIN", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    HorizontalDivider(color = Color(0x55FFFFFF))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                showJoinDialog = false
                                onCancel()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) { Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // --- EINSTELLUNGEN POPUP (Aufgeräumt) ---
        if (showSettingsDialog) {
            Dialog(onDismissRequest = { showSettingsDialog = false }) {
                var tempName by remember { mutableStateOf(playerName) }
                var tempSfxVolume by remember { mutableFloatStateOf(dataManager.sfxVolume) }
                var tempMusicVolume by remember { mutableFloatStateOf(dataManager.musicVolume) }

                Card(
                    shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xEE3E2723)),
                    modifier = Modifier.border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))



// ... in deiner @Composable Funktion:
                        val livePlayerName by dataManager.playerNameFlow.collectAsState()
                        val isGoogleLogin by dataManager.isGoogleLoginFlow.collectAsState()

// ... dein Textfeld:
                        OutlinedTextField(
                            value = livePlayerName,
                            onValueChange = { newName ->
                                // Nur speichern, wenn wir NICHT über Google eingeloggt sind
                                if (!isGoogleLogin) {
                                    dataManager.playerName = newName
                                }
                            },
                            label = { Text(if (isGoogleLogin) "Google Play Name " else "Enter Player Name") },
                            // Das Schloss: Wenn Google eingeloggt ist, ist das Feld ausgegraut und gesperrt!
                            enabled = !isGoogleLogin,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = Color(0x55D4AF37))
                        Spacer(modifier = Modifier.height(24.dp))

                        // MUSIK REGLER
                        Text("Music Volume:", color = Color.LightGray)
                        Slider(
                            value = tempMusicVolume,
                            onValueChange = {
                                tempMusicVolume = it
                                dataManager.musicVolume = it
                                soundManager.updateMusicVolume()
                            },
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFD4AF37), activeTrackColor = Color(0xFFD4AF37))
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // SFX REGLER
                        Text("Sound Effects (SFX):", color = Color.LightGray)
                        Slider(
                            value = tempSfxVolume,
                            onValueChange = {
                                tempSfxVolume = it
                                dataManager.sfxVolume = it
                            },
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFD4AF37), activeTrackColor = Color(0xFFD4AF37))
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // SCHLIESSEN BUTTON (Da alles automatisch speichert)
                        Button(
                            onClick = { showSettingsDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37))
                        ) { Text("Close", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // --- STATISTIK POPUP ---
        // --- PROFIL & ACHIEVEMENTS POPUP ---
        if (showStatsDialog) {
            Dialog(onDismissRequest = { showStatsDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xEE3E2723)),
                    // fillMaxHeight(0.85f) sorgt dafür, dass das Fenster hoch genug für die Listen ist
                    modifier = Modifier.border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp)).fillMaxHeight(0.85f)
                ) {
                    var selectedTab by remember { mutableIntStateOf(0) }

                    Column(modifier = Modifier.padding(24.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Player Profile", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // --- TABS (Stats | Achievements) ---
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFD4AF37),
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = Color(0xFFD4AF37)
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Stats", color = if (selectedTab == 0) Color(0xFFD4AF37) else Color.LightGray) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Achievements", color = if (selectedTab == 1) Color(0xFFD4AF37) else Color.LightGray) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- CONTENT BEREICH ---
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (selectedTab == 0) {
                                // --- TAB 1: STATISTIKEN (Dein alter Code) ---
                                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Wins", color = Color.Green, fontWeight = FontWeight.Bold)
                                            Text("${dataManager.totalWins}", color = Color.White, fontSize = 24.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Losses", color = Color.Red, fontWeight = FontWeight.Bold)
                                            Text("${dataManager.totalLosses}", color = Color.White, fontSize = 24.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Winrate", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                                            Text("${dataManager.getWinRate()}%", color = Color.White, fontSize = 24.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Pieces Taken", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                            Text("${dataManager.totalPiecesTaken}", color = Color.White, fontSize = 24.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Pieces Lost", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                                            Text("${dataManager.totalPiecesLost}", color = Color.White, fontSize = 24.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("Recent Matches", color = Color.White, fontWeight = FontWeight.Bold)
                                    HorizontalDivider(color = Color(0xFFD4AF37), modifier = Modifier.padding(vertical = 8.dp))

                                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                        val history = dataManager.matchHistory
                                        if (history.isEmpty()) {
                                            item { Text("No matches played yet.", color = Color.LightGray, modifier = Modifier.padding(8.dp)) }
                                        } else {
                                            items(history) { entry ->
                                                val parts = entry.split("|")
                                                if(parts.size == 2) {
                                                    val isWin = parts[1] == "WIN"
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { selectedOpponent = parts[0] }
                                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("vs. ${parts[0]}", color = Color.White)
                                                        Text(if(isWin) "WON" else "LOST", color = if(isWin) Color.Green else Color.Red, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // --- TAB 2: ACHIEVEMENTS ---
                                // --- TAB 2: TIERED ACHIEVEMENTS ---
                                LazyColumn(modifier = Modifier.fillMaxSize()) {

                                    // 1. MATCHES TIER (First Steps -> Veteran -> Enthusiast -> Legend)
                                    item {
                                        val matches = dataManager.totalWins + dataManager.totalLosses
                                        when {
                                            matches < 1 -> AchievementItem("First Steps", "Play your first match (Tier 1/4).", false, "$matches/1")
                                            matches < 50 -> AchievementItem("Veteran", "Play a total of 50 matches (Tier 2/4).", false, "$matches/50")
                                            matches < 100 -> AchievementItem("Enthusiast", "Play a total of 100 matches (Tier 3/4).", false, "$matches/100")
                                            matches < 500 -> AchievementItem("Legend", "Play a total of 500 matches (Tier 4/4).", false, "$matches/500")
                                            else -> AchievementItem("Legend", "Played a total of 500 matches (Max Tier).", true, "500/500")
                                        }
                                    }

                                    // 2. PIECES TIER (Mill Builder -> Lumberjack -> Executioner -> Terminator)
                                    item {
                                        val taken = dataManager.totalPiecesTaken
                                        when {
                                            taken < 50 -> AchievementItem("Mill Builder", "Capture 50 opponent pieces (Tier 1/4).", false, "$taken/50")
                                            taken < 250 -> AchievementItem("Lumberjack", "Capture 250 opponent pieces (Tier 2/4).", false, "$taken/250")
                                            taken < 500 -> AchievementItem("Executioner", "Capture 500 opponent pieces (Tier 3/4).", false, "$taken/500")
                                            taken < 1000 -> AchievementItem("Terminator", "Capture 1000 opponent pieces (Tier 4/4).", false, "$taken/1000")
                                            else -> AchievementItem("Terminator", "Captured 1000 opponent pieces (Max Tier).", true, "1000/1000")
                                        }
                                    }

                                    // 3. BOT SLAYER TIER (Challenger -> Grandmaster)
                                    item {
                                        when {
                                            !dataManager.achChallenger -> AchievementItem("Challenger", "Defeat the bot on 'Medium' (Tier 1/2).", false, "0/1")
                                            !dataManager.achGrandmaster -> AchievementItem("Grandmaster", "Defeat the bot on 'Hard' (Tier 2/2).", false, "0/1")
                                            else -> AchievementItem("Grandmaster", "Defeated the bot on 'Hard' (Max Tier).", true, "1/1")
                                        }
                                    }

                                    // 4. STANDALONE ACHIEVEMENTS
                                    item { AchievementItem("Untouchable", "Win a game losing a maximum of 3 pieces.", dataManager.achUntouchable, if (dataManager.achUntouchable) "1/1" else "0/1") }
                                    item { AchievementItem("Perfection", "Win a game without losing a single piece.", dataManager.achPerfection, if (dataManager.achPerfection) "1/1" else "0/1") }
                                    item { AchievementItem("Jailer", "Win by completely trapping your opponent.", dataManager.achJailer, if (dataManager.achJailer) "1/1" else "0/1") }
                                    item { AchievementItem("Comeback", "Win after being reduced to exactly 3 pieces.", dataManager.achComeback, if (dataManager.achComeback) "1/1" else "0/1") }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showStatsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                        ) { Text("Close", color = Color.Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }


        // --- KOPF-AN-KOPF POPUP ---
        if (selectedOpponent != null) {
            Dialog(onDismissRequest = { selectedOpponent = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE3E2723)),
                    modifier = Modifier.border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("vs. $selectedOpponent", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))

                        val oppWins = dataManager.getOpponentWins(selectedOpponent!!)
                        val oppLosses = dataManager.getOpponentLosses(selectedOpponent!!)
                        val totalOppMatches = oppWins + oppLosses
                        val oppWinrate = if (totalOppMatches > 0) ((oppWins.toDouble() / totalOppMatches) * 100).toInt() else 0

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Wins", color = Color.Green, fontWeight = FontWeight.Bold)
                                Text("$oppWins", color = Color.White, fontSize = 24.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Losses", color = Color.Red, fontWeight = FontWeight.Bold)
                                Text("$oppLosses", color = Color.White, fontSize = 24.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Winrate: $oppWinrate%", color = Color(0xFFD4AF37), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { selectedOpponent = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                        ) { Text("Close", color = Color.Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = transientMessage.isNotEmpty(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp) // Schwebt schön über dem unteren Rand
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEEB71C1C)), // Kräftiges Dunkelrot
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = transientMessage,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
    if (showBotDialog) {
        Dialog(onDismissRequest = { showBotDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xEE3E2723)),
                modifier = Modifier.border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select Difficulty", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = { selectedDifficulty = 1 }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedDifficulty == 1) Color(0xFFD4AF37) else Color(0xFF3E2723)), border = BorderStroke(1.dp, Color(0xFFD4AF37))) { Text("Easy", color = if (selectedDifficulty == 1) Color.Black else Color.White) }
                        Button(onClick = { selectedDifficulty = 2 }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedDifficulty == 2) Color(0xFFD4AF37) else Color(0xFF3E2723)), border = BorderStroke(1.dp, Color(0xFFD4AF37))) { Text("Medium", color = if (selectedDifficulty == 2) Color.Black else Color.White) }
                        Button(onClick = { selectedDifficulty = 3 }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedDifficulty == 3) Color(0xFFD4AF37) else Color(0xFF3E2723)), border = BorderStroke(1.dp, Color(0xFFD4AF37))) { Text("Hard", color = if (selectedDifficulty == 3) Color.Black else Color.White) }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { showBotDialog = false }) { Text("Cancel", color = Color.LightGray) }
                        Button(onClick = { showBotDialog = false; onBotStart(selectedDifficulty) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))) { Text("Start Match", color = Color.Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

}

@Composable
fun AchievementItem(title: String, description: String, isUnlocked: Boolean, progress: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Das Icon-Kreis (Gold wenn unlocked, Grau wenn locked)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) Color(0x33D4AF37) else Color(0x33AAAAAA))
                .border(2.dp, if (isUnlocked) Color(0xFFD4AF37) else Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isUnlocked) Color(0xFFFFD700) else Color.Gray
            )
        }
        Spacer(modifier = Modifier.width(16.dp))

        // Texte
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (isUnlocked) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(description, color = Color.LightGray, fontSize = 12.sp)
        }

        // Fortschrittsanzeige (z.B. "25/50")
        Text(progress, color = if (isUnlocked) Color(0xFFD4AF37) else Color.Gray, fontWeight = FontWeight.Bold)
    }
}