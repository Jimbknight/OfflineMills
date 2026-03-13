package com.Bobr.mill.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.Bobr.mill.R
import com.Bobr.mill.domain.engine.BoardDefinitions
import com.Bobr.mill.domain.models.Phase
import com.Bobr.mill.domain.models.Player
import com.Bobr.mill.ui.viewmodels.GameEvent
import com.Bobr.mill.ui.viewmodels.MillViewModel
import com.Bobr.mill.utils.SoundManager

private data class MoveAnimInfo(val fromIndex: Int, val toIndex: Int, val player: Player)
private data class RemoveAnimInfo(val fromIndex: Int, val player: Player, val destinationIsTop: Boolean)

@Composable
fun GameScreen(
    viewModel: MillViewModel,
    soundManager: SoundManager,
    onPointClicked: (Int) -> Unit,
    onQuitGame: () -> Unit,
    onConcede: () -> Unit,
    onRematchRequested: () -> Unit = {},
    onSendSignal: (Int) -> Unit
) {
    val isRpsActive by viewModel.isRpsActive.collectAsState()
    val rpsMyChoice by viewModel.rpsMyChoice.collectAsState()
    val rpsWinnerRole by viewModel.rpsWinnerRole.collectAsState()
    val isRpsTie by viewModel.isRpsTie.collectAsState()
    val state by viewModel.state.collectAsState()
    val myRole by viewModel.myLocalRole.collectAsState()
    // isOnline wird für die UI-Logik nicht mehr zwingend gebraucht, da Bot und Online sich gleich anfühlen sollen

    val isMyTurn = state.currentTurn == myRole

    val whitePieceImg = ImageBitmap.imageResource(id = R.drawable.piece_white)
    val blackPieceImg = ImageBitmap.imageResource(id = R.drawable.piece_black)
    val woodTextureImg = ImageBitmap.imageResource(id = R.drawable.game_board)

    var showConcedeDialog by remember { mutableStateOf(false) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var previousBoard by remember { mutableStateOf(state.board) }

    var moveAnimInfo by remember { mutableStateOf<MoveAnimInfo?>(null) }
    val moveProgress = remember { Animatable(1f) }

    var removeAnimInfo by remember { mutableStateOf<RemoveAnimInfo?>(null) }
    val removeProgress = remember { Animatable(0f) }

    val p1Unplaced = state.unplacedPiecesPlayerOne
    val p2Unplaced = state.unplacedPiecesPlayerTwo

    val p1Lost = 9 - state.piecesOnBoardPlayerOne - p1Unplaced
    val p2Lost = 9 - state.piecesOnBoardPlayerTwo - p2Unplaced

    // NEU: Egal ob Bot oder Online, "mein" Bereich (myRole) ist immer unten!
    val isBottomPlayerTwo = myRole == Player.PLAYER_TWO

    val bottomUnplacedCount = if (isBottomPlayerTwo) p2Unplaced else p1Unplaced
    val bottomCapturedCount = if (isBottomPlayerTwo) p1Lost else p2Lost
    val bottomUnplacedImg = if (isBottomPlayerTwo) blackPieceImg else whitePieceImg
    val bottomCapturedImg = if (isBottomPlayerTwo) whitePieceImg else blackPieceImg

    val topUnplacedCount = if (isBottomPlayerTwo) p1Unplaced else p2Unplaced
    val topCapturedCount = if (isBottomPlayerTwo) p2Lost else p1Lost
    val topUnplacedImg = if (isBottomPlayerTwo) whitePieceImg else blackPieceImg
    val topCapturedImg = if (isBottomPlayerTwo) blackPieceImg else whitePieceImg

    LaunchedEffect(state.currentPhase) {
        if (state.currentPhase == Phase.GAME_OVER) {
            showGameOverDialog = true
        } else {
            showGameOverDialog = false
        }
    }
    LaunchedEffect(state.board) {
        if (previousBoard != state.board) {
            val removed = mutableListOf<Int>()
            val added = mutableListOf<Int>()

            for (i in 0..23) {
                if (previousBoard[i] != state.board[i]) {
                    if (state.board[i] == Player.NONE) removed.add(i)
                    else if (previousBoard[i] == Player.NONE) added.add(i)
                }
            }

            if (removed.size == 1 && added.size == 1 && previousBoard[removed[0]] == state.board[added[0]]) {
                soundManager.playMoveSound()
                moveAnimInfo = MoveAnimInfo(removed[0], added[0], state.board[added[0]])
                moveProgress.snapTo(0f)
                moveProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                moveAnimInfo = null
            }
            else if (removed.size == 1 && added.isEmpty()) {
                soundManager.playRemoveSound()
                val removedPlayer = previousBoard[removed[0]]

                // NEU: Animation geht nach oben, wenn der geschlagene Stein MIR gehört (Gegner bekommt ihn)
                val destinationIsTop = removedPlayer == myRole

                removeAnimInfo = RemoveAnimInfo(removed[0], removedPlayer, destinationIsTop)
                removeProgress.snapTo(0f)
                removeProgress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
                removeAnimInfo = null
            }
            else if (added.size == 1 && removed.isEmpty()) {
                soundManager.playPlaceSound()
            }

            previousBoard = state.board
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val animatedPieceScales = state.board.indices.map { index ->
        val isSelected = state.selectedPieceIndex == index
        val targetScale = if (isSelected) 0.48f else 0.38f

        animateFloatAsState(
            targetValue = targetScale,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "scale_$index"
        )
    }

    val gridMap = mapOf(
        0 to Pair(0, 0), 1 to Pair(3, 0), 2 to Pair(6, 0),
        3 to Pair(6, 3), 4 to Pair(6, 6), 5 to Pair(3, 6),
        6 to Pair(0, 6), 7 to Pair(0, 3),
        8 to Pair(1, 1), 9 to Pair(3, 1), 10 to Pair(5, 1),
        11 to Pair(5, 3), 12 to Pair(5, 5), 13 to Pair(3, 5),
        14 to Pair(1, 5), 15 to Pair(1, 3),
        16 to Pair(2, 2), 17 to Pair(3, 2), 18 to Pair(4, 2),
        19 to Pair(4, 3), 20 to Pair(4, 4), 21 to Pair(3, 4),
        22 to Pair(2, 4), 23 to Pair(2, 3)
    )

    val validMoves = remember(state.selectedPieceIndex, state.currentPhase, state.board) {
        if (state.selectedPieceIndex == null) emptyList()
        else if (state.currentPhase == Phase.FLYING) state.board.indices.filter { state.board[it] == Player.NONE }
        else BoardDefinitions.adjacencyMap[state.selectedPieceIndex]?.filter { state.board[it] == Player.NONE } ?: emptyList()
    }

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
                .padding(start = 8.dp, end = 8.dp, top = 50.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE3E2723)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFD4AF37))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // NEU: Zeigt immer deine Farbe an
                    val roleText = when (myRole) {
                        Player.PLAYER_ONE -> "You are playing White"
                        Player.PLAYER_TWO -> "You are playing Black"
                        else -> "Waiting for assignment..."
                    }
                    Text(text = roleText, fontSize = 12.sp, color = Color(0xFFD4AF37))

                    Spacer(modifier = Modifier.height(4.dp))

                    // NEU: Einheitliche Anzeige, wer dran ist
                    val turnText = if (state.currentPhase == Phase.GAME_OVER) {
                        "GAME OVER"
                    } else if (isMyTurn) {
                        "YOUR TURN"
                    } else {
                        "OPPONENT'S TURN"
                    }

                    val turnColor = if (state.currentPhase != Phase.GAME_OVER) {
                        if (isMyTurn) Color(0xFFFFD700) else Color(0xFFA1887F)
                    } else {
                        Color.White
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (state.currentTurn == Player.PLAYER_ONE) Color.White else Color.Black)
                                .border(1.dp, Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = turnText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = turnColor)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // NEU: Wenn man nicht dran ist, immer Warten anzeigen
                    val instructionText = if (!isMyTurn && state.currentPhase != Phase.GAME_OVER) {
                        "Waiting for opponent to move..."
                    } else if (state.currentPhase == Phase.GAME_OVER) {
                        state.infoMessage
                    } else {
                        if (state.infoMessage.contains("turn", ignoreCase = true)) {
                            when (state.currentPhase) {
                                Phase.PLACING -> "Place a piece on the board."
                                Phase.MOVING -> "Select a piece to move."
                                Phase.FLYING -> "Flying Phase! Move to any empty spot."
                                Phase.REMOVING -> "Mill! Remove an opponent's piece."
                                else -> "Make your move."
                            }
                        } else {
                            state.infoMessage
                        }
                    }

                    val placingInfo = if (state.currentPhase == Phase.PLACING) {
                        val unplaced = if (state.currentTurn == Player.PLAYER_ONE) state.unplacedPiecesPlayerOne else state.unplacedPiecesPlayerTwo
                        "\n($unplaced left to place)"
                    } else ""

                    Text(
                        text = instructionText + placingInfo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE0E0E0),
                        textAlign = TextAlign.Center,
                        minLines = 2
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    repeat(topCapturedCount) {
                        Image(bitmap = topCapturedImg, contentDescription = null, modifier = Modifier.size(40.dp).padding(end = 2.dp))
                    }
                }
                Row {
                    repeat(topUnplacedCount) {
                        Image(bitmap = topUnplacedImg, contentDescription = null, modifier = Modifier.size(40.dp).padding(start = 2.dp))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .aspectRatio(1f)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                // NEU: Sperrt Klicks konsequent, wenn der Bot (oder Online-Gegner) dran ist
                                if (!isMyTurn && state.currentPhase != Phase.GAME_OVER) return@detectTapGestures

                                val margin = size.width * 0.06f
                                val drawableWidth = size.width - (2 * margin)
                                val step = drawableWidth / 6f

                                for (i in 0..23) {
                                    val gridPos = gridMap[i]!!
                                    val pointX = margin + gridPos.first * step
                                    val pointY = margin + gridPos.second * step
                                    val distance = Math.hypot((tapOffset.x - pointX).toDouble(), (tapOffset.y - pointY).toDouble())
                                    if (distance < step * 0.6f) {
                                        onPointClicked(i)
                                        break
                                    }
                                }
                            }
                        }
                ) {
                    val boardWidth = size.width
                    val margin = boardWidth * 0.06f
                    val drawableWidth = boardWidth - (2 * margin)
                    val step = drawableWidth / 6f
                    val baseRadius = step * 0.38f

                    fun getCenter(index: Int): Offset {
                        val gridPos = gridMap[index]!!
                        return Offset(margin + gridPos.first * step, margin + gridPos.second * step)
                    }

                    val zoomFactor = 0.008f
                    val bleedX = (size.width * zoomFactor).toInt()
                    val bleedY = (size.height * zoomFactor).toInt()

                    drawImage(
                        image = woodTextureImg,
                        dstOffset = IntOffset(-bleedX, -bleedY),
                        dstSize = IntSize((size.width + 2 * bleedX).toInt(), (size.height + 2 * bleedY).toInt())
                    )

                    val lineColor = Color(0xFF000000)
                    BoardDefinitions.adjacencyMap.forEach { (pointIndex, connectedPoints) ->
                        val startOffset = getCenter(pointIndex)
                        connectedPoints.forEach { connectedIndex ->
                            val endOffset = getCenter(connectedIndex)
                            drawLine(color = Color(0x55FFFFFF), start = startOffset + Offset(2f, 2f), end = endOffset + Offset(2f,2f), strokeWidth = 8f)
                            drawLine(color = lineColor, start = startOffset, end = endOffset, strokeWidth = 8f)
                        }
                    }

                    // NEU: Zeigt gültige Züge nur noch an, wenn DU dran bist
                    if (isMyTurn) {
                        validMoves.forEach { index ->
                            drawCircle(color = Color(0xAAFFEB3B), radius = step / 4f, center = getCenter(index), style = Stroke(width = 8f))
                        }
                    }

                    fun isPartOfMillLocal(board: List<Player>, index: Int, player: Player): Boolean {
                        return BoardDefinitions.millCombinations.filter { it.contains(index) }.any { combo ->
                            combo.all { board[it] == player }
                        }
                    }

                    fun areAllOpponentPiecesInMills(board: List<Player>, opponent: Player): Boolean {
                        val allOpponentIndices = board.indices.filter { board[it] == opponent }
                        return allOpponentIndices.all { isPartOfMillLocal(board, it, opponent) }
                    }

                    val currentOpponent = if (state.currentTurn == Player.PLAYER_ONE) Player.PLAYER_TWO else Player.PLAYER_ONE
                    val allOpponentInMills = areAllOpponentPiecesInMills(state.board, currentOpponent)
// --- NEU: Highlight für den letzten Zug ---
                    // Zeigt rot an, von wo der Stein kam
                    state.lastMoveFrom?.let { index ->
                        drawCircle(
                            color = Color(0x55FF5252), // Halbtransparentes Rot
                            radius = step / 3f,
                            center = getCenter(index)
                        )
                    }
                    // Zeigt grün an, wo der Stein gelandet ist
                    state.lastMoveTo?.let { index ->
                        drawCircle(
                            color = Color(0x554CAF50), // Halbtransparentes Grün
                            radius = step / 2.5f,
                            center = getCenter(index)
                        )
                    }
                    state.board.forEachIndexed { index, player ->
                        if (moveAnimInfo != null && index == moveAnimInfo!!.toIndex) return@forEachIndexed
                        if (removeAnimInfo != null && index == removeAnimInfo!!.fromIndex) return@forEachIndexed

                        val center = getCenter(index)
                        val baseAnimatedRadius = animatedPieceScales[index].value * step

                        var isRemovableTarget = false

                        if (state.currentPhase == Phase.REMOVING && player == currentOpponent) {
                            if (isMyTurn) { // NEU: Nur noch animieren, wenn wir am Zug sind
                                val inMill = isPartOfMillLocal(state.board, index, currentOpponent)
                                if (!inMill || allOpponentInMills) {
                                    isRemovableTarget = true
                                }
                            }
                        }

                        val finalRadius = if (isRemovableTarget) baseAnimatedRadius * pulseScale else baseAnimatedRadius

                        if (state.selectedPieceIndex == index) {
                            drawCircle(color = Color(0x66FFEB3B), radius = finalRadius + 10f, center = center)
                        }

                        if (player != Player.NONE) {
                            val imgToDraw = if (player == Player.PLAYER_ONE) whitePieceImg else blackPieceImg
                            val imgSize = (finalRadius * 2).toInt()
                            val topLeft = IntOffset((center.x - finalRadius).toInt(), (center.y - finalRadius).toInt())
                            drawImage(image = imgToDraw, dstOffset = topLeft, dstSize = IntSize(imgSize, imgSize))
                        }
                    }

                    moveAnimInfo?.let { animInfo ->
                        val startCenter = getCenter(animInfo.fromIndex)
                        val endCenter = getCenter(animInfo.toIndex)
                        val currentCenter = lerp(startCenter, endCenter, moveProgress.value)

                        val finalRadius = animatedPieceScales[animInfo.toIndex].value * step
                        val imgToDraw = if (animInfo.player == Player.PLAYER_ONE) whitePieceImg else blackPieceImg
                        val imgSize = (finalRadius * 2).toInt()

                        val topLeft = IntOffset((currentCenter.x - finalRadius).toInt(), (currentCenter.y - finalRadius).toInt())
                        drawImage(image = imgToDraw, dstOffset = topLeft, dstSize = IntSize(imgSize, imgSize))
                    }

                    removeAnimInfo?.let { animInfo ->
                        val startCenter = getCenter(animInfo.fromIndex)
                        val targetX = 0f
                        val targetY = if (animInfo.destinationIsTop) 0f else size.height
                        val endCenter = Offset(targetX, targetY)

                        val currentCenter = lerp(startCenter, endCenter, removeProgress.value)
                        val imgToDraw = if (animInfo.player == Player.PLAYER_ONE) whitePieceImg else blackPieceImg
                        val imgSize = (baseRadius * 2).toInt()

                        val alpha = 1f - removeProgress.value

                        val topLeft = IntOffset((currentCenter.x - baseRadius).toInt(), (currentCenter.y - baseRadius).toInt())
                        drawImage(image = imgToDraw, dstOffset = topLeft, dstSize = IntSize(imgSize, imgSize), alpha = alpha)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    repeat(bottomCapturedCount) {
                        Image(bitmap = bottomCapturedImg, contentDescription = null, modifier = Modifier.size(40.dp).padding(end = 2.dp))
                    }
                }
                Row {
                    repeat(bottomUnplacedCount) {
                        Image(bitmap = bottomUnplacedImg, contentDescription = null, modifier = Modifier.size(40.dp).padding(start = 2.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.currentPhase != Phase.GAME_OVER) {
                    Button(
                        onClick = { showConcedeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                        border = BorderStroke(1.dp, Color(0xFFD4AF37))
                    ) {
                        Text("Concede", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                    }
                } else {
                    // NEU: Wenn das Spiel vorbei ist, zeige hier einen "Options" Button, um das Popup zurückzuholen
                    Button(
                        onClick = { showGameOverDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                        border = BorderStroke(1.dp, Color(0xFFD4AF37))
                    ) {
                        Text("Match Options", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showConcedeDialog) {
            Dialog(onDismissRequest = { showConcedeDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE3E2723)),
                    modifier = Modifier.border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Concede Match?", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Are you sure you want to give up? This will count as a win for your opponent.", color = Color.LightGray, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(
                                onClick = { showConcedeDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                                border = BorderStroke(1.dp, Color(0xFFD4AF37))
                            ) { Text("Cancel", color = Color(0xFFD4AF37)) }

                            Button(
                                onClick = {
                                    showConcedeDialog = false
                                    onConcede()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                            ) { Text("Yes, Concede", color = Color.Black, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

// NEU: Das Popup wird nur angezeigt, wenn showGameOverDialog true ist
        if (state.currentPhase == Phase.GAME_OVER && showGameOverDialog) {
            val winner = state.winner
            val didIWin = winner == myRole
            val titleText = if (didIWin) "VICTORY!" else "DEFEAT"
            val reasonText = state.infoMessage
            val subtitleText = if (didIWin) "You outsmarted your opponent." else "Better luck next time."
            val popupColor = if (!didIWin) Color(0xFFFF5252) else Color(0xFFFFD700)

            Dialog(onDismissRequest = { showGameOverDialog = false }) { // NEU: Lässt sich per Klick daneben schließen
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFAAAAAA)),
                    modifier = Modifier.border(2.dp, popupColor, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.background(Color(0xEE111111)).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = titleText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = popupColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = reasonText, fontSize = 16.sp, color = Color.White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = subtitleText, fontSize = 16.sp, color = Color.White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = onRematchRequested,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                        ) {
                            Text("Play Again", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // NEU: Button, um sich das Brett anzusehen
                        Button(
                            onClick = { showGameOverDialog = false },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37))
                        ) {
                            Text("View Board", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onQuitGame,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37))
                        ) {
                            Text("Quit to Main Menu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                        }
                    }
                }
            }
        }

        if (isRpsActive) {
            Dialog(onDismissRequest = {}) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE3E2723)),
                    modifier = Modifier.border(2.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isRpsTie) {
                            Text("It's a Tie!", color = Color(0xFFFFD700), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Both players chose the same.", color = Color.White)

                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(2000)
                                viewModel.resetRpsRound()
                            }
                        } else if (rpsWinnerRole != null) {
                            if (rpsWinnerRole == myRole) {
                                Text("You Won!", color = Color.Green, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Choose your color:", color = Color.White)

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Button(
                                        onClick = {
                                            viewModel.finalizeMyColorChoice(true)
                                            onSendSignal(-20)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                    ) { Text("White", color = Color.Black, fontWeight = FontWeight.Bold) }

                                    Button(
                                        onClick = {
                                            viewModel.finalizeMyColorChoice(false)
                                            onSendSignal(-21)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                                    ) { Text("Black", color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("White begins", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Opponent Won!", color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Waiting for opponent to choose color...", color = Color.LightGray, textAlign = TextAlign.Center)
                            }
                        } else if (rpsMyChoice == 0) {
                            Text("Rock, Paper, Scissors!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Button(onClick = { viewModel.setMyRpsChoice(1); onSendSignal(-10) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))) { Text("🪨", fontSize = 24.sp) }
                                Button(onClick = { viewModel.setMyRpsChoice(2); onSendSignal(-11) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))) { Text("📄", fontSize = 24.sp) }
                                Button(onClick = { viewModel.setMyRpsChoice(3); onSendSignal(-12) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))) { Text("✂️", fontSize = 24.sp) }
                            }
                        } else {
                            Text("Waiting for opponent...", color = Color.LightGray, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}