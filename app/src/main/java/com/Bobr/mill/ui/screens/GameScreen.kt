package com.Bobr.mill.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Bobr.mill.domain.engine.BoardDefinitions
import com.Bobr.mill.domain.models.Phase
import com.Bobr.mill.domain.models.Player
import com.Bobr.mill.ui.viewmodels.GameEvent
import com.Bobr.mill.ui.viewmodels.MillViewModel

@Composable
fun GameScreen(
    viewModel: MillViewModel,
    onPointClicked: (Int) -> Unit,
    onQuitGame: () -> Unit // NEU: Der Callback, um das Spiel abzubrechen
) {
    val state by viewModel.state.collectAsState()
    val myRole by viewModel.myLocalRole.collectAsState()

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
        if (state.selectedPieceIndex == null) {
            emptyList()
        } else if (state.currentPhase == Phase.FLYING) {
            state.board.indices.filter { state.board[it] == Player.NONE }
        } else {
            BoardDefinitions.adjacencyMap[state.selectedPieceIndex]
                ?.filter { state.board[it] == Player.NONE } ?: emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF224C2E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Phase: ${state.currentPhase}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)

            val roleText = when {
                viewModel.isLocalMode -> "Lokales Spiel"
                myRole == Player.PLAYER_ONE -> "Du bist Weiß"
                myRole == Player.PLAYER_TWO -> "Du bist Schwarz"
                else -> "Warte auf Zuweisung..."
            }
            Text(text = roleText, fontSize = 16.sp, color = Color(0xFFAAAAAA))

            Text(
                text = state.infoMessage,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (state.currentTurn == Player.PLAYER_ONE) Color(0xFF64B5F6) else Color(0xFFFF8A80),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF8D6E63))
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val margin = size.width * 0.12f
                            val drawableWidth = size.width - (2 * margin)
                            val step = drawableWidth / 6f

                            for (i in 0..23) {
                                val gridPos = gridMap[i]!!
                                val pointX = margin + gridPos.first * step
                                val pointY = margin + gridPos.second * step
                                val distance = Math.hypot((tapOffset.x - pointX).toDouble(), (tapOffset.y - pointY).toDouble())
                                if (distance < step * 0.7f) {
                                    onPointClicked(i)
                                    break
                                }
                            }
                        }
                    }
            ) {
                val boardWidth = size.width
                val margin = boardWidth * 0.12f
                val drawableWidth = boardWidth - (2 * margin)
                val step = drawableWidth / 6f

                BoardDefinitions.adjacencyMap.forEach { (pointIndex, connectedPoints) ->
                    val startPos = gridMap[pointIndex]!!
                    val startOffset = Offset(margin + startPos.first * step, margin + startPos.second * step)

                    connectedPoints.forEach { connectedIndex ->
                        val endPos = gridMap[connectedIndex]!!
                        val endOffset = Offset(margin + endPos.first * step, margin + endPos.second * step)
                        drawLine(color = Color(0xFF3E2723), start = startOffset, end = endOffset, strokeWidth = 6f)
                    }
                }

                validMoves.forEach { index ->
                    val gridPos = gridMap[index]!!
                    val center = Offset(margin + gridPos.first * step, margin + gridPos.second * step)
                    drawCircle(color = Color(0xAAFFEB3B), radius = step / 4f, center = center, style = Stroke(width = 8f))
                }

                state.board.forEachIndexed { index, player ->
                    val gridPos = gridMap[index]!!
                    val center = Offset(margin + gridPos.first * step, margin + gridPos.second * step)
                    val isSelected = state.selectedPieceIndex == index
                    val pieceRadius = if (isSelected) step / 2.2f else step / 3.2f

                    if (isSelected) {
                        drawCircle(color = Color(0x66FFEB3B), radius = pieceRadius + 8f, center = center)
                    }

                    when (player) {
                        Player.PLAYER_ONE -> {
                            drawCircle(color = Color.White, radius = pieceRadius, center = center)
                            drawCircle(color = Color.Black, radius = pieceRadius, center = center, style = Stroke(4f))
                        }
                        Player.PLAYER_TWO -> {
                            drawCircle(color = Color.Black, radius = pieceRadius, center = center)
                            drawCircle(color = Color.White, radius = pieceRadius, center = center, style = Stroke(2f))
                        }
                        Player.NONE -> {
                            drawCircle(color = Color(0x44000000), radius = 8f, center = center)
                        }
                    }
                }
            }
        }

        // --- BOTTOM BEREICH (Buttons) ---
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (viewModel.isLocalMode) {
                Button(onClick = { viewModel.onEvent(GameEvent.OnResetClicked) }) {
                    Text("Neustart")
                }
            }

            // Verlassen-Button (immer da, um zurück in die Lobby zu kommen)
            Button(
                onClick = onQuitGame,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)) // Roter Button
            ) {
                Text("Spiel verlassen", color = Color.White)
            }
        }
    }
}