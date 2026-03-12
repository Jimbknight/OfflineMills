package com.Bobr.mill.domain.engine

import com.Bobr.mill.domain.models.GameState
import com.Bobr.mill.domain.models.Phase
import com.Bobr.mill.domain.models.Player
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BotEngineTest {

    private val botEngine = BotEngine()

    @Test
    fun `bot should choose a cross point in opening phase when board is empty`() {
        // ARRANGE: Komplett leeres Brett, Bot (P2) ist dran
        val initialState = GameState(
            currentTurn = Player.PLAYER_TWO,
            currentPhase = Phase.PLACING
        )

        // ACT: Bot berechnet seinen Zug
        val clicks = botEngine.calculateClicks(initialState, Player.PLAYER_TWO, BotDifficulty.HARD)

        // ASSERT: Laut Opening Book sollte er einen der Kreuzungspunkte (9, 11, 13, 15) wählen
        val crossPoints = listOf(9, 11, 13, 15)
        assertThat(clicks).hasSize(1)
        assertThat(clicks[0]).isIn(crossPoints)
    }

    @Test
    fun `bot should block opponent's immediate mill threat during placing`() {
        // ARRANGE: Spieler 1 hat Steine auf 0 und 1. Er würde auf 2 eine Mühle schließen.
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE
        board[1] = Player.PLAYER_ONE

        val state = GameState(
            board = board,
            currentTurn = Player.PLAYER_TWO,
            currentPhase = Phase.PLACING,
            unplacedPiecesPlayerOne = 7,
            unplacedPiecesPlayerTwo = 8,
            piecesOnBoardPlayerOne = 2,
            piecesOnBoardPlayerTwo = 0
        )

        // ACT: Bot berechnet Zug
        val clicks = botEngine.calculateClicks(state, Player.PLAYER_TWO, BotDifficulty.HARD)

        // ASSERT: Der Bot MUSS auf Feld 2 setzen, um die Mühle zu blockieren (Opening Book Logik)
        assertThat(clicks).containsExactly(2)
    }

    @Test
    fun `bot must move to close mill when only one piece can move`() {
        val board = MutableList(24) { Player.NONE }
        // Bot (P2) hat Steine auf 0 und 2. Nur Stein 9 kann sich bewegen.
        board[0] = Player.PLAYER_TWO
        board[2] = Player.PLAYER_TWO
        board[9] = Player.PLAYER_TWO

        // Blockiere alle anderen Wege für Stein 0 und 2 (laut AdjacencyMap)
        board[7] = Player.PLAYER_ONE // Blockiert 0
        board[3] = Player.PLAYER_ONE // Blockiert 2

        // Opfersteine für P1
        board[10] = Player.PLAYER_ONE
        board[11] = Player.PLAYER_ONE
        board[12] = Player.PLAYER_ONE
        board[13] = Player.PLAYER_ONE
        board[14] = Player.PLAYER_ONE

        val state = GameState(
            board = board,
            currentTurn = Player.PLAYER_TWO,
            currentPhase = Phase.MOVING,
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            piecesOnBoardPlayerOne = 5,
            piecesOnBoardPlayerTwo = 3
        )

        val clicks = botEngine.calculateClicks(state, Player.PLAYER_TWO, BotDifficulty.HARD)

        // Wenn hier immer noch [0, 1] kommt, obwohl 0 blockiert ist,
        // dann nutzt der Bot eine veraltete AdjacencyMap oder ignoriert die Engine-Regeln!
        assertThat(clicks[0]).isEqualTo(9)
        assertThat(clicks[1]).isEqualTo(1)
        assertThat(clicks).hasSize(3)
    }
    @Test
    fun `bot should close its own mill by moving piece from 9 to 1`() {
        val board = MutableList(24) { Player.NONE }

        // Bot (P2) hat Steine auf 0 und 2. Stein 9 soll nach 1 ziehen.
        board[0] = Player.PLAYER_TWO
        board[2] = Player.PLAYER_TWO
        board[9] = Player.PLAYER_TWO

        // WICHTIG: Wir blockieren den Zug von 0 nach 1, indem wir Feld 7 besetzen.
        // So hat Stein 0 KEINE legalen Züge (0 ist nur mit 1 und 7 verbunden).
        board[7] = Player.PLAYER_ONE

        // Gegner (P1) braucht genug Steine für die REMOVING-Phase
        board[10] = Player.PLAYER_ONE
        board[11] = Player.PLAYER_ONE
        board[12] = Player.PLAYER_ONE
        board[13] = Player.PLAYER_ONE
        board[14] = Player.PLAYER_ONE

        val state = GameState(
            board = board,
            currentTurn = Player.PLAYER_TWO,
            currentPhase = Phase.MOVING,
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            piecesOnBoardPlayerOne = 5,
            piecesOnBoardPlayerTwo = 3
        )

        // HARD Modus nutzen, damit Minimax voll greift
        val clicks = botEngine.calculateClicks(state, Player.PLAYER_TWO, BotDifficulty.HARD)

        // ASSERT
        // Da Stein 0 blockiert ist, MUSS der Bot Stein 9 wählen.
        assertThat(clicks[0]).isEqualTo(9)
        assertThat(clicks[1]).isEqualTo(1)
        // Durch das Schließen der Mühle MUSS ein dritter Klick folgen
        assertThat(clicks).hasSize(3)
    }
    @Test
    fun `bot should block opponent mill threat in placing phase`() {
        // ARRANGE: P1 hat 0 und 1 belegt. Bot (P2) muss 2 blockieren.
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE
        board[1] = Player.PLAYER_ONE

        val state = GameState(
            board = board,
            currentTurn = Player.PLAYER_TWO,
            currentPhase = Phase.PLACING,
            unplacedPiecesPlayerOne = 7,
            unplacedPiecesPlayerTwo = 8,
            piecesOnBoardPlayerOne = 2,
            piecesOnBoardPlayerTwo = 0
        )

        // ACT
        val clicks = botEngine.calculateClicks(state, Player.PLAYER_TWO, BotDifficulty.HARD)

        // ASSERT: Muss Feld 2 sein
        assertThat(clicks).containsExactly(2)
    }

}