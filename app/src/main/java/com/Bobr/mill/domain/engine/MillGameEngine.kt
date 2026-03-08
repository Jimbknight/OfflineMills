package com.Bobr.mill.domain.engine

import com.Bobr.mill.domain.models.GameState
import com.Bobr.mill.domain.models.Phase
import com.Bobr.mill.domain.models.Player

class MillGameEngine {

    private fun Player.opponent(): Player = if (this == Player.PLAYER_ONE) Player.PLAYER_TWO else Player.PLAYER_ONE

    fun processClick(state: GameState, clickedIndex: Int): GameState {
        if (state.currentPhase == Phase.GAME_OVER) return state

        return when (state.currentPhase) {
            Phase.PLACING -> handlePlacing(state, clickedIndex)
            Phase.MOVING, Phase.FLYING -> handleMovement(state, clickedIndex)
            Phase.REMOVING -> handleRemoving(state, clickedIndex)
            Phase.GAME_OVER -> state
        }
    }

    private fun handlePlacing(state: GameState, index: Int): GameState {
        if (state.board[index] != Player.NONE) return state

        val newBoard = state.board.toMutableList()
        newBoard[index] = state.currentTurn

        val unplacedP1 = if (state.currentTurn == Player.PLAYER_ONE) state.unplacedPiecesPlayerOne - 1 else state.unplacedPiecesPlayerOne
        val unplacedP2 = if (state.currentTurn == Player.PLAYER_TWO) state.unplacedPiecesPlayerTwo - 1 else state.unplacedPiecesPlayerTwo
        val onBoardP1 = if (state.currentTurn == Player.PLAYER_ONE) state.piecesOnBoardPlayerOne + 1 else state.piecesOnBoardPlayerOne
        val onBoardP2 = if (state.currentTurn == Player.PLAYER_TWO) state.piecesOnBoardPlayerTwo + 1 else state.piecesOnBoardPlayerTwo

        val nextState = state.copy(
            board = newBoard,
            unplacedPiecesPlayerOne = unplacedP1,
            unplacedPiecesPlayerTwo = unplacedP2,
            piecesOnBoardPlayerOne = onBoardP1,
            piecesOnBoardPlayerTwo = onBoardP2
        )

        return checkMillAndEndTurn(nextState, index)
    }

    private fun handleMovement(state: GameState, index: Int): GameState {
        if (state.board[index] == state.currentTurn) {
            return state.copy(selectedPieceIndex = index, infoMessage = "Stein gewählt. Tippe auf ein leeres Feld.")
        }

        val selectedIndex = state.selectedPieceIndex ?: return state
        if (state.board[index] != Player.NONE) return state

        val isValidMove = if (state.currentPhase == Phase.FLYING) {
            true
        } else {
            BoardDefinitions.adjacencyMap[selectedIndex]?.contains(index) == true
        }

        if (!isValidMove) return state

        val newBoard = state.board.toMutableList()
        newBoard[selectedIndex] = Player.NONE
        newBoard[index] = state.currentTurn

        val nextState = state.copy(board = newBoard, selectedPieceIndex = null)

        return checkMillAndEndTurn(nextState, index)
    }

    private fun handleRemoving(state: GameState, index: Int): GameState {
        val opponent = state.currentTurn.opponent()

        if (state.board[index] != opponent) return state

        val isTargetInMill = isPartOfMill(state.board, index, opponent)
        if (isTargetInMill) {
            val allOpponentPiecesInMills = state.board.indices
                .filter { state.board[it] == opponent }
                .all { isPartOfMill(state.board, it, opponent) }

            if (!allOpponentPiecesInMills) {
                return state.copy(infoMessage = "Du kannst keine Mühle brechen, wenn andere Steine frei sind!")
            }
        }

        val newBoard = state.board.toMutableList()
        newBoard[index] = Player.NONE

        val onBoardP1 = if (opponent == Player.PLAYER_ONE) state.piecesOnBoardPlayerOne - 1 else state.piecesOnBoardPlayerOne
        val onBoardP2 = if (opponent == Player.PLAYER_TWO) state.piecesOnBoardPlayerTwo - 1 else state.piecesOnBoardPlayerTwo

        val opponentUnplaced = if (opponent == Player.PLAYER_ONE) state.unplacedPiecesPlayerOne else state.unplacedPiecesPlayerTwo
        val opponentOnBoard = if (opponent == Player.PLAYER_ONE) onBoardP1 else onBoardP2

        // Check 1: Hat der Gegner weniger als 3 Steine? (Und Plazierungsphase ist vorbei)
        if (opponentUnplaced == 0 && opponentOnBoard < 3) {
            return state.copy(
                board = newBoard, piecesOnBoardPlayerOne = onBoardP1, piecesOnBoardPlayerTwo = onBoardP2,
                currentPhase = Phase.GAME_OVER,
                infoMessage = "Spielende! ${if (state.currentTurn == Player.PLAYER_ONE) "Weiß" else "Schwarz"} gewinnt!"
            )
        }

        val nextState = state.copy(
            board = newBoard, piecesOnBoardPlayerOne = onBoardP1, piecesOnBoardPlayerTwo = onBoardP2,
            currentTurn = opponent
        )

        // Check 2: Ist der Gegner jetzt blockiert?
        if (isPlayerTrapped(nextState, opponent)) {
            return nextState.copy(
                currentPhase = Phase.GAME_OVER, currentTurn = state.currentTurn,
                infoMessage = "Spielende! ${if (state.currentTurn == Player.PLAYER_ONE) "Weiß" else "Schwarz"} gewinnt (Gegner blockiert)!"
            )
        }

        return determineNextPhase(nextState)
    }

    private fun checkMillAndEndTurn(state: GameState, lastMovedIndex: Int): GameState {
        if (isPartOfMill(state.board, lastMovedIndex, state.currentTurn)) {
            return state.copy(currentPhase = Phase.REMOVING, infoMessage = "Mühle! Entferne einen Stein.")
        }

        val opponent = state.currentTurn.opponent()
        val nextState = state.copy(currentTurn = opponent)

        // Check: Ist der nächste Spieler blockiert?
        if (isPlayerTrapped(nextState, opponent)) {
            return nextState.copy(
                currentPhase = Phase.GAME_OVER, currentTurn = state.currentTurn,
                infoMessage = "Spielende! ${if (state.currentTurn == Player.PLAYER_ONE) "Weiß" else "Schwarz"} gewinnt (Gegner blockiert)!"
            )
        }

        return determineNextPhase(nextState)
    }

    private fun isPartOfMill(board: List<Player>, index: Int, player: Player): Boolean {
        return BoardDefinitions.millCombinations.filter { it.contains(index) }.any { combo ->
            combo.all { board[it] == player }
        }
    }

    // NEU: Die Blockade-Prüfung
    private fun isPlayerTrapped(state: GameState, playerToCheck: Player): Boolean {
        val unplaced = if (playerToCheck == Player.PLAYER_ONE) state.unplacedPiecesPlayerOne else state.unplacedPiecesPlayerTwo
        val onBoard = if (playerToCheck == Player.PLAYER_ONE) state.piecesOnBoardPlayerOne else state.piecesOnBoardPlayerTwo

        if (unplaced > 0) return false // Kann noch setzen
        if (onBoard <= 3) return false // Kann fliegen (oder hat schon verloren)

        // Prüfen, ob IRGENDEIN Stein dieses Spielers ein leeres Nachbarfeld hat
        val playerPieces = state.board.indices.filter { state.board[it] == playerToCheck }
        for (index in playerPieces) {
            val adjacent = BoardDefinitions.adjacencyMap[index] ?: emptyList()
            if (adjacent.any { state.board[it] == Player.NONE }) {
                return false // Es gibt noch einen gültigen Zug
            }
        }
        return true // Keine Züge mehr möglich = Eingezwickt!
    }

    private fun determineNextPhase(state: GameState): GameState {
        val unplaced = if (state.currentTurn == Player.PLAYER_ONE) state.unplacedPiecesPlayerOne else state.unplacedPiecesPlayerTwo
        val onBoard = if (state.currentTurn == Player.PLAYER_ONE) state.piecesOnBoardPlayerOne else state.piecesOnBoardPlayerTwo

        val newPhase = when {
            unplaced > 0 -> Phase.PLACING
            onBoard == 3 -> Phase.FLYING
            else -> Phase.MOVING
        }

        return state.copy(currentPhase = newPhase, infoMessage = "${if (state.currentTurn == Player.PLAYER_ONE) "Weiß" else "Schwarz"} ist am Zug.")
    }
}