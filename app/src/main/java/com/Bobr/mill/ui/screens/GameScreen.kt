package com.Bobr.mill.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
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

private data class MoveAnimInfo(val fromIndex: Int, val toIndex: Int, val player: Player)
private data class RemoveAnimInfo(val fromIndex: Int, val player: Player, val destinationIsTop: Boolean)

@Composable
fun GameScreen(
    viewModel: MillViewModel,
    onPointClicked: (Int) -> Unit,
    onQuitGame: () -> Unit,
    onRematchRequested: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val myRole by viewModel.myLocalRole.collectAsState()
    val isOnline = !viewModel.isLocalMode

    val isMyTurn = state.currentTurn == myRole

    val whitePieceImg = ImageBitmap.imageResource(id = R.drawable.piece_white)
    val blackPieceImg = ImageBitmap.imageResource(id = R.drawable.piece_black)
    val woodTextureImg = ImageBitmap.imageResource(id = R.drawable.game_board)

    // --- ANIMATIONS-STATUS ---
    var previousBoard by remember { mutableStateOf(state.board) }

    var moveAnimInfo by remember { mutableStateOf<MoveAnimInfo?>(null) }
    val moveProgress = remember { Animatable(1f) }

    var removeAnimInfo by remember { mutableStateOf<RemoveAnimInfo?>(null) }
    val removeProgress = remember { Animatable(0f) }

    // --- VERLORENE STEINE ZÄHLER ---
    val p1Lost = 9 - state.piecesOnBoardPlayerOne - state.unplacedPiecesPlayerOne
    val p2Lost = 9 - state.piecesOnBoardPlayerTwo - state.unplacedPiecesPlayerTwo

    val topLostCount = if (isOnline) (if (myRole == Player.PLAYER_ONE) p1Lost else p2Lost) else p2Lost
    val bottomLostCount = if (isOnline) (if (myRole == Player.PLAYER_ONE) p2Lost else p1Lost) else p1Lost

    val topPieceImg = if (isOnline) (if (myRole == Player.PLAYER_ONE) whitePieceImg else blackPieceImg) else blackPieceImg
    val bottomPieceImg = if (isOnline) (if (myRole == Player.PLAYER_ONE) blackPieceImg else whitePieceImg) else whitePieceImg

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

            // 1. Zug-Animation
            if (removed.size == 1 && added.size == 1 && previousBoard[removed[0]] == state.board[added[0]]) {
                moveAnimInfo = MoveAnimInfo(removed[0], added[0], state.board[added[0]])
                moveProgress.snapTo(0f)
                moveProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                moveAnimInfo = null
            }
            // 2. Lösch-Animation (Fliegen zur Ablage)
            else if (removed.size == 1 && added.isEmpty()) {
                val removedPlayer = previousBoard[removed[0]]
                val destinationIsTop = if (isOnline) removedPlayer == myRole else removedPlayer == Player.PLAYER_TWO

                removeAnimInfo = RemoveAnimInfo(removed[0], removedPlayer, destinationIsTop)
                removeProgress.snapTo(0f)
                removeProgress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
                removeAnimInfo = null
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

            // --- DYNAMIC TOP INFO CARD ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x99000000)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val roleText = when {
                        !isOnline -> "Local Match"
                        myRole == Player.PLAYER_ONE -> "You are playing White"
                        myRole == Player.PLAYER_TWO -> "You are playing Black"
                        else -> "Waiting for assignment..."
                    }
                    Text(text = roleText, fontSize = 12.sp, color = Color.LightGray)

                    Spacer(modifier = Modifier.height(4.dp))

                    val turnText = if (isOnline) {
                        if (state.currentPhase == Phase.GAME_OVER) "GAME OVER"
                        else if (isMyTurn) "YOUR TURN"
                        else "OPPONENT'S TURN"
                    } else {
                        if (state.currentPhase == Phase.GAME_OVER) "GAME OVER"
                        else if (state.currentTurn == Player.PLAYER_ONE) "WHITE'S TURN"
                        else "BLACK'S TURN"
                    }

                    val turnColor = if (isOnline && state.currentPhase != Phase.GAME_OVER) {
                        if (isMyTurn) Color(0xFF4CAF50) else Color(0xFFFF5252)
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

                    val instructionText = if (isOnline && !isMyTurn && state.currentPhase != Phase.GAME_OVER) {
                        "Waiting for opponent to move..."
                    } else if (state.currentPhase == Phase.GAME_OVER) {
                        "Match finished."
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

                    Text(
                        text = instructionText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE0E0E0),
                        textAlign = TextAlign.Center
                    )
                }
            }

// --- OBERE ABLAGE (Links über dem Brett) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp) // FESTE HÖHE: Verhindert das Verschieben des Layouts
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(topLostCount) {
                    Image(
                        bitmap = topPieceImg,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp).padding(end = 4.dp)
                    )
                }
            }

            // --- DAS SPIELBRETT ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .aspectRatio(1f)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .background(Color.Black, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val isCurrentlyMyTurn = state.currentTurn == myRole
                                if (isOnline && !isCurrentlyMyTurn && state.currentPhase != Phase.GAME_OVER) return@detectTapGestures

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

                    // 1. HOLZBRETT ZEICHNEN
                    drawImage(
                        image = woodTextureImg,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )

                    // 2. LINIEN ZEICHNEN
                    val lineColor = Color(0xFF2D1A11)
                    BoardDefinitions.adjacencyMap.forEach { (pointIndex, connectedPoints) ->
                        val startOffset = getCenter(pointIndex)
                        connectedPoints.forEach { connectedIndex ->
                            val endOffset = getCenter(connectedIndex)
                            drawLine(color = Color(0x55FFFFFF), start = startOffset + Offset(2f, 2f), end = endOffset + Offset(2f,2f), strokeWidth = 8f)
                            drawLine(color = lineColor, start = startOffset, end = endOffset, strokeWidth = 8f)
                        }
                    }

                    // 3. MARKIERUNGEN ZEICHNEN
                    if (!isOnline || isMyTurn) {
                        validMoves.forEach { index ->
                            drawCircle(color = Color(0xAAFFEB3B), radius = step / 4f, center = getCenter(index), style = Stroke(width = 8f))
                        }
                    }

                    // 4. STATISCHE STEINE ZEICHNEN
                    state.board.forEachIndexed { index, player ->
                        if (moveAnimInfo != null && index == moveAnimInfo!!.toIndex) return@forEachIndexed
                        if (removeAnimInfo != null && index == removeAnimInfo!!.fromIndex) return@forEachIndexed

                        val center = getCenter(index)
                        val baseAnimatedRadius = animatedPieceScales[index].value * step
                        val isRemovableTarget = state.currentPhase == Phase.REMOVING && player != state.currentTurn && player != Player.NONE
                        val finalRadius = if (isRemovableTarget && (!isOnline || isMyTurn)) baseAnimatedRadius * pulseScale else baseAnimatedRadius

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

                    // 5. FLUG-ANIMATION (BEWEGEN)
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

                    // 6. FLUG-ANIMATION (SCHLAGEN - Fliegt vom Feld zur linken Kante)
                    removeAnimInfo?.let { animInfo ->
                        val startCenter = getCenter(animInfo.fromIndex)

                        // Fliegt in Richtung der linken Kante des Brettes und verblasst
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

// --- UNTERE ABLAGE (Links unter dem Brett) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp) // FESTE HÖHE: Verhindert das Verschieben des Layouts
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(bottomLostCount) {
                    Image(
                        bitmap = bottomPieceImg,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp).padding(end = 4.dp)
                    )
                }
            }

            // --- BOTTOM AREA ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isOnline) {
                    Button(
                        onClick = { viewModel.onEvent(GameEvent.OnResetClicked) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Restart", color = Color.White)
                    }
                }

                Button(
                    onClick = onQuitGame,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Quit Game", color = Color.White)
                }
            }
        }

        // --- GAME OVER POPUP ---
        if (state.currentPhase == Phase.GAME_OVER) {
            val didIWin = isOnline && state.currentTurn == myRole
            val winner = state.currentTurn

            val titleText = if (isOnline) {
                if (didIWin) "VICTORY!" else "DEFEAT"
            } else {
                if (winner == Player.PLAYER_ONE) "WHITE WINS!" else "BLACK WINS!"
            }

            val subtitleText = if (isOnline) {
                if (didIWin) "You outsmarted your opponent." else "Better luck next time."
            } else {
                "A well-played match!"
            }

            val popupColor = if (isOnline && !didIWin) Color(0xFFFF5252) else Color(0xFFFFD700)

            Dialog(onDismissRequest = {}) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFAAAAAA)),
                    modifier = Modifier.border(2.dp, popupColor, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.background(Color(0xEE111111)).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = titleText,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = popupColor
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = subtitleText,
                            fontSize = 16.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = onRematchRequested,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text(if (isOnline) "Request Rematch" else "Play Again", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onQuitGame,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Quit to Main Menu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}