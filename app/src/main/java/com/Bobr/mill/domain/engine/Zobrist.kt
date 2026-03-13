package com.Bobr.mill.domain.engine

import com.Bobr.mill.domain.models.GameState
import com.Bobr.mill.domain.models.Phase
import com.Bobr.mill.domain.models.Player
import kotlin.random.Random

object Zobrist {
    private val pieceKeys = Array(24) { LongArray(3) } // 24 Felder, 3 Zustände (NONE, P1, P2)
    private val turnKey = LongArray(2)
    private val phaseKey = LongArray(Phase.values().size)
    private val unplacedP1Key = LongArray(10) // 0 bis 9 Steine
    private val unplacedP2Key = LongArray(10) // 0 bis 9 Steine

    init {
        val rng = Random(42) // Fester Seed, damit Hashes in derselben Session konsistent sind

        for (i in 0 until 24) {
            for (p in 0 until 3) pieceKeys[i][p] = rng.nextLong()
        }
        for (i in 0 until 2) turnKey[i] = rng.nextLong()
        for (i in phaseKey.indices) phaseKey[i] = rng.nextLong()
        for (i in 0..9) {
            unplacedP1Key[i] = rng.nextLong()
            unplacedP2Key[i] = rng.nextLong()
        }
    }

    private fun playerToIndex(player: Player): Int = when(player) {
        Player.NONE -> 0
        Player.PLAYER_ONE -> 1
        Player.PLAYER_TWO -> 2
    }

    fun compute(state: GameState): Long {
        var hash = 0L

        // 1. Brettpositionen einbeziehen
        for (i in 0 until 24) {
            hash = hash xor pieceKeys[i][playerToIndex(state.board[i])]
        }

        // 2. Wer ist am Zug?
        hash = hash xor turnKey[if (state.currentTurn == Player.PLAYER_ONE) 0 else 1]

        // 3. Welche Phase?
        hash = hash xor phaseKey[state.currentPhase.ordinal]

        // 4. Ungesetzte Steine
        hash = hash xor unplacedP1Key[state.unplacedPiecesPlayerOne]
        hash = hash xor unplacedP2Key[state.unplacedPiecesPlayerTwo]

        return hash
    }
}