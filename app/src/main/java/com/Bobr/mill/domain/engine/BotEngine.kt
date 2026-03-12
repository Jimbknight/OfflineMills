package com.Bobr.mill.domain.engine

import com.Bobr.mill.domain.models.GameState
import com.Bobr.mill.domain.models.Phase
import com.Bobr.mill.domain.models.Player

enum class BotDifficulty { EASY, MEDIUM, HARD }

data class BotMove(val clicks: List<Int>)

enum class TTEntryType { EXACT, LOWERBOUND, UPPERBOUND }
data class TTEntry(val depth: Int, val value: Int, val type: TTEntryType)

class BotEngine {

    private val gameEngine = MillGameEngine()
    private val transpositionTable = HashMap<Long, TTEntry>()

    fun calculateClicks(state: GameState, botRole: Player, difficulty: BotDifficulty): List<Int> {
        val validMoves = generateFullTurns(state, state.currentTurn)
        if (validMoves.isEmpty()) return emptyList()

        transpositionTable.clear()

        // --- OPENING BOOK ---
        if (difficulty == BotDifficulty.HARD || difficulty == BotDifficulty.MEDIUM) {
            val bookMove = getOpeningMove(state, botRole)
            if (bookMove != null) {
                return bookMove.clicks
            }
        }

        val chosenMove = when (difficulty) {
            BotDifficulty.EASY -> validMoves.random()
            BotDifficulty.MEDIUM -> {
                val piecesOnBoard = state.board.count { it != Player.NONE }
                val depth = if (piecesOnBoard <= 8) 4 else 3
                findMoveFixedDepth(state, validMoves, botRole, depth = depth)
            }
            BotDifficulty.HARD -> {
                findMoveIterativeDeepening(state, validMoves, botRole, maxTimeMs = 1500L)
            }
        }

        return chosenMove.clicks
    }

    private fun getOpeningMove(state: GameState, botRole: Player): BotMove? {
        if (state.currentPhase != Phase.PLACING) return null

        val piecesOnBoard = state.board.count { it != Player.NONE }
        if (piecesOnBoard > 4) return null

        val oppRole = if (botRole == Player.PLAYER_ONE) Player.PLAYER_TWO else Player.PLAYER_ONE

        val immediateThreat = findImmediateThreat(state, oppRole)
        if (immediateThreat != null) {
            return BotMove(listOf(immediateThreat))
        }

        val crossPoints = listOf(9, 11, 13, 15)
        val availableCross = crossPoints.filter { state.board[it] == Player.NONE }
        if (availableCross.isNotEmpty()) {
            return BotMove(listOf(availableCross.random()))
        }

        val midPoints = listOf(1, 3, 5, 7, 17, 19, 21, 23)
        val availableMid = midPoints.filter { state.board[it] == Player.NONE }
        if (availableMid.isNotEmpty()) {
            return BotMove(listOf(availableMid.random()))
        }

        return null
    }

    private fun findImmediateThreat(state: GameState, opponent: Player): Int? {
        val emptySpots = state.board.indices.filter { state.board[it] == Player.NONE }
        for (spot in emptySpots) {
            val testBoard = state.board.toMutableList()
            testBoard[spot] = opponent
            if (isPartOfMill(testBoard, spot, opponent)) {
                return spot
            }
        }
        return null
    }

    private fun findMoveIterativeDeepening(state: GameState, validMoves: List<BotMove>, botRole: Player, maxTimeMs: Long): BotMove {
        val startTime = System.currentTimeMillis()
        var bestMove = validMoves.random()
        var currentDepth = 1

        var orderedMoves = validMoves.sortedByDescending { it.clicks.size }

        while (System.currentTimeMillis() - startTime < maxTimeMs && currentDepth <= 15) {
            var bestValueThisDepth = Int.MIN_VALUE
            var bestMoveThisDepth = orderedMoves.first()
            val evaluatedMoves = mutableListOf<Pair<BotMove, Int>>()

            for (move in orderedMoves) {
                if (System.currentTimeMillis() - startTime >= maxTimeMs) break

                val futureState = simulateFullTurn(state, move)
                val moveValue = minimaxTimeLimited(futureState, currentDepth - 1, false, -2000000, 2000000, botRole, startTime, maxTimeMs)

                evaluatedMoves.add(Pair(move, moveValue))

                if (moveValue > bestValueThisDepth) {
                    bestValueThisDepth = moveValue
                    bestMoveThisDepth = move
                }
            }

            if (System.currentTimeMillis() - startTime < maxTimeMs) {
                bestMove = bestMoveThisDepth
                orderedMoves = evaluatedMoves.sortedByDescending { it.second }.map { it.first }
                currentDepth++
            } else {
                break
            }
        }
        return bestMove
    }

    private fun minimaxTimeLimited(state: GameState, depth: Int, isMaximizing: Boolean, alpha: Int, beta: Int, botRole: Player, startTime: Long, maxTimeMs: Long): Int {
        if (System.currentTimeMillis() - startTime >= maxTimeMs) return 0

        val hash = Zobrist.compute(state)
        val ttEntry = transpositionTable[hash]

        if (ttEntry != null && ttEntry.depth >= depth) {
            when (ttEntry.type) {
                TTEntryType.EXACT -> return ttEntry.value
                TTEntryType.LOWERBOUND -> if (ttEntry.value >= beta) return ttEntry.value
                TTEntryType.UPPERBOUND -> if (ttEntry.value <= alpha) return ttEntry.value
            }
        }

        val oppRole = if (botRole == Player.PLAYER_ONE) Player.PLAYER_TWO else Player.PLAYER_ONE

        if (state.winner != null) {
            return if (state.winner == botRole) 1000000 + depth else -1000000 - depth
        }

        if (depth == 0) {
            val eval = evaluateState(state, botRole, oppRole)
            transpositionTable[hash] = TTEntry(depth, eval, TTEntryType.EXACT)
            return eval
        }

        val currentPlayer = state.currentTurn
        val validMoves = generateFullTurns(state, currentPlayer)

        if (validMoves.isEmpty()) {
            return if (currentPlayer == botRole) -1000000 - depth else 1000000 + depth
        }

        val orderedMoves = validMoves.sortedByDescending { it.clicks.size }
        var currentAlpha = alpha
        var currentBeta = beta
        var bestValue = if (isMaximizing) Int.MIN_VALUE else Int.MAX_VALUE

        if (isMaximizing) {
            for (move in orderedMoves) {
                val futureState = simulateFullTurn(state, move)
                val eval = minimaxTimeLimited(futureState, depth - 1, false, currentAlpha, currentBeta, botRole, startTime, maxTimeMs)
                bestValue = maxOf(bestValue, eval)
                currentAlpha = maxOf(currentAlpha, bestValue)
                if (currentBeta <= currentAlpha) break
            }
        } else {
            for (move in orderedMoves) {
                val futureState = simulateFullTurn(state, move)
                val eval = minimaxTimeLimited(futureState, depth - 1, true, currentAlpha, currentBeta, botRole, startTime, maxTimeMs)
                bestValue = minOf(bestValue, eval)
                currentBeta = minOf(currentBeta, bestValue)
                if (currentBeta <= currentAlpha) break
            }
        }

        val ttType = when {
            bestValue <= alpha -> TTEntryType.UPPERBOUND
            bestValue >= beta -> TTEntryType.LOWERBOUND
            else -> TTEntryType.EXACT
        }

        if (System.currentTimeMillis() - startTime < maxTimeMs) {
            transpositionTable[hash] = TTEntry(depth, bestValue, ttType)
        }

        return bestValue
    }

    private fun findMoveFixedDepth(state: GameState, validMoves: List<BotMove>, botRole: Player, depth: Int): BotMove {
        var bestMove = validMoves.random()
        var bestValue = Int.MIN_VALUE
        val orderedMoves = validMoves.sortedByDescending { it.clicks.size }

        for (move in orderedMoves) {
            val futureState = simulateFullTurn(state, move)
            val moveValue = minimaxFixed(futureState, depth - 1, false, -2000000, 2000000, botRole)

            if (moveValue > bestValue) {
                bestValue = moveValue
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimaxFixed(state: GameState, depth: Int, isMaximizing: Boolean, alpha: Int, beta: Int, botRole: Player): Int {
        val hash = Zobrist.compute(state)
        val ttEntry = transpositionTable[hash]

        if (ttEntry != null && ttEntry.depth >= depth) {
            when (ttEntry.type) {
                TTEntryType.EXACT -> return ttEntry.value
                TTEntryType.LOWERBOUND -> if (ttEntry.value >= beta) return ttEntry.value
                TTEntryType.UPPERBOUND -> if (ttEntry.value <= alpha) return ttEntry.value
            }
        }

        val oppRole = if (botRole == Player.PLAYER_ONE) Player.PLAYER_TWO else Player.PLAYER_ONE

        if (state.winner != null) {
            return if (state.winner == botRole) 1000000 + depth else -1000000 - depth
        }

        if (depth == 0) {
            val eval = evaluateState(state, botRole, oppRole)
            transpositionTable[hash] = TTEntry(depth, eval, TTEntryType.EXACT)
            return eval
        }

        val currentPlayer = state.currentTurn
        val validMoves = generateFullTurns(state, currentPlayer)

        if (validMoves.isEmpty()) {
            return if (currentPlayer == botRole) -1000000 - depth else 1000000 + depth
        }

        var currentAlpha = alpha
        var currentBeta = beta
        var bestValue = if (isMaximizing) Int.MIN_VALUE else Int.MAX_VALUE
        val orderedMoves = validMoves.sortedByDescending { it.clicks.size }

        if (isMaximizing) {
            for (move in orderedMoves) {
                val futureState = simulateFullTurn(state, move)
                val eval = minimaxFixed(futureState, depth - 1, false, currentAlpha, currentBeta, botRole)
                bestValue = maxOf(bestValue, eval)
                currentAlpha = maxOf(currentAlpha, bestValue)
                if (currentBeta <= currentAlpha) break
            }
        } else {
            for (move in orderedMoves) {
                val futureState = simulateFullTurn(state, move)
                val eval = minimaxFixed(futureState, depth - 1, true, currentAlpha, currentBeta, botRole)
                bestValue = minOf(bestValue, eval)
                currentBeta = minOf(currentBeta, bestValue)
                if (currentBeta <= currentAlpha) break
            }
        }

        val ttType = when {
            bestValue <= alpha -> TTEntryType.UPPERBOUND
            bestValue >= beta -> TTEntryType.LOWERBOUND
            else -> TTEntryType.EXACT
        }
        transpositionTable[hash] = TTEntry(depth, bestValue, ttType)

        return bestValue
    }

    private fun evaluateState(state: GameState, botRole: Player, oppRole: Player): Int {
        val botOnBoard = state.board.count { it == botRole }
        val oppOnBoard = state.board.count { it == oppRole }
        val botUnplaced = if (botRole == Player.PLAYER_ONE) state.unplacedPiecesPlayerOne else state.unplacedPiecesPlayerTwo
        val oppUnplaced = if (oppRole == Player.PLAYER_ONE) state.unplacedPiecesPlayerOne else state.unplacedPiecesPlayerTwo

        val botTotal = botOnBoard + botUnplaced
        val oppTotal = oppOnBoard + oppUnplaced

        val materialWeight = if (botTotal <= 4 || oppTotal <= 4) 15000 else 1000
        var score = (botTotal - oppTotal) * materialWeight

        var botMills = 0
        var oppMills = 0
        var botTwoPieces = 0
        var oppTwoPieces = 0

        val botPieceInTwoRow = mutableMapOf<Int, Int>()
        val oppPieceInTwoRow = mutableMapOf<Int, Int>()

        for (combo in BoardDefinitions.millCombinations) {
            var b = 0; var o = 0; var e = 0
            val bIndices = mutableListOf<Int>()
            val oIndices = mutableListOf<Int>()

            for (idx in combo) {
                when (state.board[idx]) {
                    botRole -> { b++; bIndices.add(idx) }
                    oppRole -> { o++; oIndices.add(idx) }
                    else -> e++
                }
            }
            if (b == 3) botMills++
            else if (o == 3) oppMills++
            else if (b == 2 && e == 1) {
                botTwoPieces++
                bIndices.forEach { botPieceInTwoRow[it] = botPieceInTwoRow.getOrDefault(it, 0) + 1 }
            }
            else if (o == 2 && e == 1) {
                oppTwoPieces++
                oIndices.forEach { oppPieceInTwoRow[it] = oppPieceInTwoRow.getOrDefault(it, 0) + 1 }
            }
        }

        score += botMills * 3000
        score -= oppMills * 3500

        val botZwickmuehlen = botPieceInTwoRow.values.count { it > 1 }
        val oppZwickmuehlen = oppPieceInTwoRow.values.count { it > 1 }
        score += botZwickmuehlen * 4000
        score -= oppZwickmuehlen * 4500

        val botInFlying = botTotal <= 3 && botUnplaced == 0
        val oppInFlying = oppTotal <= 3 && oppUnplaced == 0

        val botThreatWeight = if (botInFlying) 5000 else 800
        val oppThreatWeight = if (oppInFlying) 7000 else 1000

        score += botTwoPieces * botThreatWeight
        score -= oppTwoPieces * oppThreatWeight

        if (!botInFlying && botUnplaced == 0) {
            val botBlocked = state.board.indices.filter { state.board[it] == botRole }
                .count { idx -> BoardDefinitions.adjacencyMap[idx]?.all { state.board[it] != Player.NONE } == true }
            score -= botBlocked * 400
        }
        if (!oppInFlying && oppUnplaced == 0) {
            val oppBlocked = state.board.indices.filter { state.board[it] == oppRole }
                .count { idx -> BoardDefinitions.adjacencyMap[idx]?.all { state.board[it] != Player.NONE } == true }
            score += oppBlocked * 600
        }

        if (botUnplaced > 0) {
            val centers = listOf(4, 10, 13, 19)
            centers.forEach { if (state.board[it] == botRole) score += 60 }
            val midEdges = listOf(1, 7, 16, 22)
            midEdges.forEach { if (state.board[it] == botRole) score += 30 }
        }

        return score
    }

    private fun simulateFullTurn(startState: GameState, move: BotMove): GameState {
        var currentState = startState
        for (click in move.clicks) {
            currentState = gameEngine.processClick(currentState, click)
        }
        return currentState
    }

    private fun generateFullTurns(state: GameState, startingPlayer: Player, currentClicks: List<Int> = emptyList()): List<BotMove> {
        if (state.currentPhase == Phase.GAME_OVER || state.currentTurn != startingPlayer) {
            return listOf(BotMove(currentClicks))
        }

        val possibleClicks = getPossibleClicksForCurrentState(state)
        val fullMoves = mutableListOf<BotMove>()

        for (click in possibleClicks) {
            val nextState = gameEngine.processClick(state, click)
            val subMoves = generateFullTurns(nextState, startingPlayer, currentClicks + click)
            fullMoves.addAll(subMoves)
        }
        return fullMoves
    }

    private fun getPossibleClicksForCurrentState(state: GameState): List<Int> {
        val player = state.currentTurn
        return when (state.currentPhase) {
            Phase.PLACING -> state.board.indices.filter { state.board[it] == Player.NONE }
            Phase.MOVING, Phase.FLYING -> {
                if (state.selectedPieceIndex == null) {
                    state.board.indices.filter { state.board[it] == player }
                } else {
                    val from = state.selectedPieceIndex!!
                    state.board.indices.filter { target ->
                        if (state.board[target] != Player.NONE) false
                        else if (state.currentPhase == Phase.FLYING) true
                        else BoardDefinitions.adjacencyMap[from]?.contains(target) == true
                    }
                }
            }
            Phase.REMOVING -> {
                val opp = if (player == Player.PLAYER_ONE) Player.PLAYER_TWO else Player.PLAYER_ONE
                val oppPieces = state.board.indices.filter { state.board[it] == opp }
                val freePieces = oppPieces.filter { !isPartOfMill(state.board, it, opp) }
                if (freePieces.isNotEmpty()) freePieces else oppPieces
            }
            Phase.GAME_OVER -> emptyList()
        }
    }

    private fun isPartOfMill(board: List<Player>, index: Int, player: Player): Boolean {
        return BoardDefinitions.millCombinations.filter { it.contains(index) }.any { combo ->
            combo.all { board[it] == player }
        }
    }
}