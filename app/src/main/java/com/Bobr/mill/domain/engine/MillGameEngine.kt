package com.Bobr.mill.domain.engine

import com.Bobr.mill.domain.models.GameState
import com.Bobr.mill.domain.models.Phase
import com.Bobr.mill.domain.models.Player

class MillGameEngine {

    private fun Player.opponent(): Player = if (this == Player.PLAYER_ONE) Player.PLAYER_TWO else Player.PLAYER_ONE

    fun processClick(state: GameState, clickedIndex: Int): GameState {

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
            piecesOnBoardPlayerTwo = onBoardP2,
            lastMoveFrom = null, // Beim Setzen gibt es kein "Von"
            lastMoveTo = index,
            movesWithoutCaptureOrMill = 0, // Beim Setzen ändert sich das Material (irreversibel)
            boardHistory = emptyMap() // Historie wird resettet, da irreversibel
        )

        return checkMillAndEndTurn(nextState, index)
    }

    private fun handleMovement(state: GameState, index: Int): GameState {
        if (state.board[index] == state.currentTurn) {
            return state.copy(selectedPieceIndex = index, infoMessage = "Piece selected. Tap an empty spot.")
        }

        val selectedIndex = state.selectedPieceIndex ?: return state
        if (state.board[index] != Player.NONE) return state

        val isValidMove = if (state.currentPhase == Phase.FLYING) {
            true
        } else {
            BoardDefinitions.adjacencyMap.getValue(selectedIndex).contains(index)
        }

        if (!isValidMove) return state

        val newBoard = state.board.toMutableList()
        newBoard[selectedIndex] = Player.NONE
        newBoard[index] = state.currentTurn

        val nextState = state.copy(
            board = newBoard,
            selectedPieceIndex = null,
            lastMoveFrom = selectedIndex, // Speichere, von wo der Stein kam
            lastMoveTo = index,           // Speichere, wo er hinging
            movesWithoutCaptureOrMill = state.movesWithoutCaptureOrMill + 1 // Zugzähler erhöhen
        )

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
                return state.copy(infoMessage = "Cannot break a mill if other pieces are free!")
            }
        }

        val newBoard = state.board.toMutableList()
        newBoard[index] = Player.NONE

        val onBoardP1 = if (opponent == Player.PLAYER_ONE) state.piecesOnBoardPlayerOne - 1 else state.piecesOnBoardPlayerOne
        val onBoardP2 = if (opponent == Player.PLAYER_TWO) state.piecesOnBoardPlayerTwo - 1 else state.piecesOnBoardPlayerTwo

        val opponentUnplaced = if (opponent == Player.PLAYER_ONE) state.unplacedPiecesPlayerOne else state.unplacedPiecesPlayerTwo
        val opponentOnBoard = if (opponent == Player.PLAYER_ONE) onBoardP1 else onBoardP2

        // ENDBEDINGUNG 1: Gegner hat weniger als 3 Steine
        if (opponentUnplaced == 0 && opponentOnBoard < 3) {
            return state.copy(
                board = newBoard, piecesOnBoardPlayerOne = onBoardP1, piecesOnBoardPlayerTwo = onBoardP2,
                currentPhase = Phase.GAME_OVER,
                infoMessage = "Game Over! ${if (state.currentTurn == Player.PLAYER_ONE) "White" else "Black"} wins!",
                winner = state.currentTurn
            )
        }

        val nextState = state.copy(
            board = newBoard,
            piecesOnBoardPlayerOne = onBoardP1,
            piecesOnBoardPlayerTwo = onBoardP2,
            currentTurn = opponent,
            movesWithoutCaptureOrMill = 0, // Schlagen resettet die 50-Züge Regel!
            boardHistory = emptyMap() // Schlagen verändert das Material -> Historie resettet!
        )

        // ENDBEDINGUNG 2: Gegner ist eingesperrt
        if (isPlayerTrapped(nextState, opponent)) {
            return nextState.copy(
                currentPhase = Phase.GAME_OVER, currentTurn = state.currentTurn,
                infoMessage = "Game Over! ${if (state.currentTurn == Player.PLAYER_ONE) "White" else "Black"} wins (Opponent trapped)!",
                winner = state.currentTurn
            )
        }

        return determineNextPhase(nextState)
    }

    private fun checkMillAndEndTurn(state: GameState, lastMovedIndex: Int): GameState {
        if (isPartOfMill(state.board, lastMovedIndex, state.currentTurn)) {
            return state.copy(
                currentPhase = Phase.REMOVING,
                infoMessage = "Mill! Remove an opponent's piece.",
                movesWithoutCaptureOrMill = 0 // Mühle resettet den Zähler (da ein Stein fallen wird)
            )
        }

        val opponent = state.currentTurn.opponent()

        // --- DRAW LOGIC: Historie für Stellungswiederholung generieren ---
        // Ein Status wird durch das Brett und den Spieler am Zug definiert
        val stateKey = "${state.board.joinToString("")}_$opponent"
        val newHistory = state.boardHistory.toMutableMap()
        val occurrences = newHistory.getOrDefault(stateKey, 0) + 1
        newHistory[stateKey] = occurrences

        var nextState = state.copy(currentTurn = opponent, boardHistory = newHistory)

        // --- DRAW CHECK: 50 Züge Regel (25 volle Runden ohne Mühle/Schlagen) ---
        if (nextState.movesWithoutCaptureOrMill >= 50) {
            return nextState.copy(
                currentPhase = Phase.GAME_OVER,
                infoMessage = "Draw! 50 moves without capture or mill.",
                winner = null,
                isDraw = true
            )
        }

        // --- DRAW CHECK: 3-Fache Stellungswiederholung ---
        if (occurrences >= 3) {
            return nextState.copy(
                currentPhase = Phase.GAME_OVER,
                infoMessage = "Draw! 3-fold repetition.",
                winner = null,
                isDraw = true
            )
        }

        // ENDBEDINGUNG 3: Gegner ist eingesperrt
        if (isPlayerTrapped(nextState, opponent)) {
            return nextState.copy(
                currentPhase = Phase.GAME_OVER, currentTurn = state.currentTurn,
                infoMessage = "Game Over! ${if (state.currentTurn == Player.PLAYER_ONE) "White" else "Black"} wins (Opponent trapped)!",
                winner = state.currentTurn
            )
        }

        return determineNextPhase(nextState)
    }

    private fun isPartOfMill(board: List<Player>, index: Int, player: Player): Boolean {
        return BoardDefinitions.millCombinations.filter { it.contains(index) }.any { combo ->
            combo.all { board[it] == player }
        }
    }

    private fun isPlayerTrapped(state: GameState, playerToCheck: Player): Boolean {
        val unplaced = if (playerToCheck == Player.PLAYER_ONE) state.unplacedPiecesPlayerOne else state.unplacedPiecesPlayerTwo
        val onBoard = if (playerToCheck == Player.PLAYER_ONE) state.piecesOnBoardPlayerOne else state.piecesOnBoardPlayerTwo

        if (unplaced > 0) return false
        if (onBoard <= 3) return false

        val playerPieces = state.board.indices.filter { state.board[it] == playerToCheck }
        for (index in playerPieces) {
            val adjacent = BoardDefinitions.adjacencyMap.getValue(index)
            if (adjacent.any { state.board[it] == Player.NONE }) {
                return false
            }
        }
        return true
    }

    private fun determineNextPhase(state: GameState): GameState {
        val unplaced = if (state.currentTurn == Player.PLAYER_ONE) state.unplacedPiecesPlayerOne else state.unplacedPiecesPlayerTwo
        val onBoard = if (state.currentTurn == Player.PLAYER_ONE) state.piecesOnBoardPlayerOne else state.piecesOnBoardPlayerTwo

        val newPhase = when {
            unplaced > 0 -> Phase.PLACING
            onBoard == 3 -> Phase.FLYING
            else -> Phase.MOVING
        }

        return state.copy(currentPhase = newPhase, infoMessage = "${if (state.currentTurn == Player.PLAYER_ONE) "White's" else "Black's"} turn.")
    }
}