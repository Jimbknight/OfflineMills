package com.Bobr.millbybobr.domain.engine

import com.Bobr.millbybobr.domain.models.GameState
import com.Bobr.millbybobr.domain.models.Phase
import com.Bobr.millbybobr.domain.models.Player
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MillGameEngineTest {

    // Die Engine, die wir testen wollen
    private val engine = MillGameEngine()

    @Test
    fun `when placing piece on empty spot, then board updates and turn switches`() {
        // 1. ARRANGE (Die Startbedingungen festlegen)
        val initialState = GameState(
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.PLACING,
            unplacedPiecesPlayerOne = 9,
            piecesOnBoardPlayerOne = 0
        )

        // 2. ACT (Die Aktion ausführen - Wir tippen auf Feld 0)
        val newState = engine.processClick(initialState, 0)

        // 3. ASSERT (Ist das Ergebnis korrekt?)
        // Liegt der Stein jetzt auf Feld 0?
        assertThat(newState.board[0]).isEqualTo(Player.PLAYER_ONE)

        // Wurden die Handsteine von 9 auf 8 reduziert?
        assertThat(newState.unplacedPiecesPlayerOne).isEqualTo(8)

        // Liegt jetzt 1 Stein von Spieler 1 auf dem Brett?
        assertThat(newState.piecesOnBoardPlayerOne).isEqualTo(1)

        // Ist jetzt Spieler 2 am Zug?
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_TWO)

        // Sind wir immer noch in der Setz-Phase?
        assertThat(newState.currentPhase).isEqualTo(Phase.PLACING)
    }

    @Test
    fun `when placing piece on occupied spot, then state remains unchanged`() {
        // 1. ARRANGE: Feld 0 ist bereits von Spieler 2 besetzt
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_TWO

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.PLACING
        )

        // 2. ACT: Spieler 1 versucht, auf Feld 0 zu setzen
        val newState = engine.processClick(initialState, 0)

        // 3. ASSERT: Es darf absolut nichts passieren, der State muss exakt gleich bleiben
        assertThat(newState).isEqualTo(initialState)
    }

    @Test
    fun `when placing piece completes a mill, then phase changes to REMOVING`() {
        // 1. ARRANGE: Spieler 1 hat schon Steine auf 0 und 1. Feld 2 ist frei.
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE
        board[1] = Player.PLAYER_ONE

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.PLACING,
            unplacedPiecesPlayerOne = 7,
            piecesOnBoardPlayerOne = 2
        )

        // 2. ACT: Spieler 1 schließt die Mühle auf Feld 2 (0, 1, 2)
        val newState = engine.processClick(initialState, 2)

        // 3. ASSERT: Der Stein muss liegen und wir müssen in der "Lösch-Phase" sein!
        assertThat(newState.board[2]).isEqualTo(Player.PLAYER_ONE)
        assertThat(newState.currentPhase).isEqualTo(Phase.REMOVING)
        // WICHTIG: Es muss weiterhin Spieler 1 am Zug sein, da er ja jetzt löschen darf!
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_ONE)
    }

    @Test
    fun `when clicking own piece in MOVING phase, then piece is selected`() {
        // 1. ARRANGE: Setzphase ist vorbei, Spieler 1 ist dran und klickt seinen Stein
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            unplacedPiecesPlayerOne = 0, // Keine Steine mehr auf der Hand
            unplacedPiecesPlayerTwo = 0
        )

        // 2. ACT: Spieler 1 klickt auf seinen eigenen Stein auf Feld 0
        val newState = engine.processClick(initialState, 0)

        // 3. ASSERT: Der Stein muss als "ausgewählt" markiert sein
        assertThat(newState.selectedPieceIndex).isEqualTo(0)
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_ONE) // Zug ist noch nicht vorbei
    }

    @Test
    fun `when moving selected piece to valid empty spot, then board updates and turn switches`() {
        // 1. ARRANGE: Spieler 1 hat den Stein auf 0 bereits ausgewählt. Feld 1 ist frei.
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            selectedPieceIndex = 0 // Stein ist bereits ausgewählt!
        )

        // 2. ACT: Spieler 1 klickt auf das benachbarte freie Feld 1
        val newState = engine.processClick(initialState, 1)

        // 3. ASSERT: Der Stein ist gewandert, die Auswahl ist weg, Spieler 2 ist dran
        assertThat(newState.board[0]).isEqualTo(Player.NONE)
        assertThat(newState.board[1]).isEqualTo(Player.PLAYER_ONE)
        assertThat(newState.selectedPieceIndex).isNull()
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_TWO)
    }

    @Test
    fun `when trying to remove own piece in REMOVING phase, then state remains unchanged`() {
        // 1. ARRANGE: Player 1's turn to remove, but clicks their own piece
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE
        board[1] = Player.PLAYER_TWO

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.REMOVING
        )

        // 2. ACT: Player 1 clicks on their OWN piece at index 0
        val newState = engine.processClick(initialState, 0)

        // 3. ASSERT: Nothing should happen
        assertThat(newState).isEqualTo(initialState)
    }

    @Test
    fun `when trying to remove piece in a mill while free pieces exist, then state remains unchanged`() {
        // 1. ARRANGE: Player 2 has a mill (0, 1, 2) AND a free piece (3).
        // Player 1 is in REMOVING phase.
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_TWO
        board[1] = Player.PLAYER_TWO
        board[2] = Player.PLAYER_TWO // Mill
        board[3] = Player.PLAYER_TWO // Free piece

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.REMOVING
        )

        // 2. ACT: Player 1 tries to remove piece at index 0 (which is in a mill)
        val newState = engine.processClick(initialState, 0)

        // 3. ASSERT: The move is blocked. Board stays the same.
        // Note: infoMessage might change to "Cannot break a mill...", so we just check the board and phase
        assertThat(newState.board[0]).isEqualTo(Player.PLAYER_TWO)
        assertThat(newState.currentPhase).isEqualTo(Phase.REMOVING)
    }

    @Test
    fun `when removing valid opponent piece, then piece is removed and turn switches`() {
        // 1. ARRANGE: Player 1's turn to remove. Player 2 has a piece at index 5.
        // Both players still have unplaced pieces, so we should go back to PLACING phase.
        val board = MutableList(24) { Player.NONE }
        board[5] = Player.PLAYER_TWO

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.REMOVING,
            unplacedPiecesPlayerOne = 5,
            unplacedPiecesPlayerTwo = 5,
            piecesOnBoardPlayerTwo = 1
        )

        // 2. ACT: Player 1 removes the piece at index 5
        val newState = engine.processClick(initialState, 5)

        // 3. ASSERT: Piece is gone, phase changes to PLACING (because unplaced > 0), turn changes to P2
        assertThat(newState.board[5]).isEqualTo(Player.NONE)
        assertThat(newState.piecesOnBoardPlayerTwo).isEqualTo(0)
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_TWO)
        assertThat(newState.currentPhase).isEqualTo(Phase.PLACING)
    }

    @Test
    fun `when removing piece drops opponent to 2 pieces, then game is over and winner is declared`() {
        // 1. ARRANGE: Player 2 has exactly 3 pieces on board and 0 unplaced.
        // Player 1 removes one of them.
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_TWO
        board[1] = Player.PLAYER_TWO
        board[2] = Player.PLAYER_TWO

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.REMOVING,
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            piecesOnBoardPlayerOne = 4,
            piecesOnBoardPlayerTwo = 3 // Only 3 left!
        )

        // 2. ACT: Player 1 removes the piece at index 0
        val newState = engine.processClick(initialState, 0)

        // 3. ASSERT: Phase changes to GAME_OVER, Player 1 is the winner!
        assertThat(newState.board[0]).isEqualTo(Player.NONE)
        assertThat(newState.currentPhase).isEqualTo(Phase.GAME_OVER)
        assertThat(newState.winner).isEqualTo(Player.PLAYER_ONE)
    }

    @Test
    fun `when 50 moves pass without capture or mill, then game ends in draw`() {
        // 1. ARRANGE: We simulate that 49 moves have already passed.
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 0, // Player 1 selected the piece at 0
            movesWithoutCaptureOrMill = 49, // One move away from a draw!
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0
        )

        // 2. ACT: Player 1 makes a normal move (0 -> 1)
        val newState = engine.processClick(initialState, 1)

        // 3. ASSERT: The game must end in a draw
        assertThat(newState.currentPhase).isEqualTo(Phase.GAME_OVER)
        assertThat(newState.isDraw).isTrue()
        assertThat(newState.winner).isNull() // No one wins in a draw
    }

    @Test
    fun `when same board state occurs 3 times, then game ends in draw`() {
        // 1. ARRANGE: Player 1 is going to move 0 -> 1.
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE

        // We calculate exactly what the board will look like after the move
        val expectedNewBoard = MutableList(24) { Player.NONE }
        expectedNewBoard[1] = Player.PLAYER_ONE

        // This string is how our engine remembers the state (Board + Next Player)
        val stateKey = "${expectedNewBoard.joinToString("")}_PLAYER_TWO"

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 0,
            boardHistory = mapOf(stateKey to 2), // This exact state has already happened 2 times!
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0
        )

        // 2. ACT: Player 1 makes the move, triggering the 3rd repetition
        val newState = engine.processClick(initialState, 1)

        // 3. ASSERT: The game must end in a draw
        assertThat(newState.currentPhase).isEqualTo(Phase.GAME_OVER)
        assertThat(newState.isDraw).isTrue()
        assertThat(newState.winner).isNull()
    }

    @Test
    fun `when opponent has no valid moves left, then current player wins`() {
        // 1. ARRANGE: We trap Player 2 in the top left corner.
        val board = MutableList(24) { Player.NONE }

        // Player 2's pieces
        board[0] = Player.PLAYER_TWO
        board[1] = Player.PLAYER_TWO
        board[2] = Player.PLAYER_TWO

        // Player 1 blocks all exits for Player 2
        board[7] = Player.PLAYER_ONE // Blocks index 0
        board[9] = Player.PLAYER_ONE // Blocks index 1
        board[14] = Player.PLAYER_ONE
    }
    @Test
    fun `when clicking in GAME_OVER phase, then state remains unchanged`() {
        val initialState = GameState(currentPhase = Phase.GAME_OVER)
        val newState = engine.processClick(initialState, 0)
        assertThat(newState).isEqualTo(initialState)
    }

    @Test
    fun `when Player 2 places piece, then P2 unplaced count drops and turn switches`() {
        // Hits the Player 2 branches in handlePlacing
        val initialState = GameState(
            currentTurn = Player.PLAYER_TWO,
            currentPhase = Phase.PLACING,
            unplacedPiecesPlayerTwo = 9,
            piecesOnBoardPlayerTwo = 0
        )
        val newState = engine.processClick(initialState, 0)
        assertThat(newState.board[0]).isEqualTo(Player.PLAYER_TWO)
        assertThat(newState.unplacedPiecesPlayerTwo).isEqualTo(8)
        assertThat(newState.piecesOnBoardPlayerTwo).isEqualTo(1)
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_ONE)
    }

    @Test
    fun `when clicking empty spot in MOVING phase without selection, then state remains unchanged`() {
        // Hits the selectedPieceIndex null check
        val initialState = GameState(
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = null
        )
        val newState = engine.processClick(initialState, 0)
        assertThat(newState).isEqualTo(initialState)
    }

    @Test
    fun `when moving to non-adjacent spot in MOVING phase, then state remains unchanged`() {
        // Hits the adjacency map false branch
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE
        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 0
        )
        // 23 is on the opposite side of the board
        val newState = engine.processClick(initialState, 23)
        assertThat(newState).isEqualTo(initialState)
    }

    @Test
    fun `when moving to non-adjacent spot in FLYING phase, then move is valid`() {
        // Hits the FLYING phase true branch
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE
        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.FLYING,
            selectedPieceIndex = 0,
            unplacedPiecesPlayerOne = 0,
            piecesOnBoardPlayerOne = 3
        )
        val newState = engine.processClick(initialState, 23)
        assertThat(newState.board[23]).isEqualTo(Player.PLAYER_ONE)
        assertThat(newState.board[0]).isEqualTo(Player.NONE)
    }

    @Test
    fun `when trying to remove empty spot in REMOVING phase, then state remains unchanged`() {
        // Hits the empty spot check in handleRemoving
        val initialState = GameState(
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.REMOVING
        )
        val newState = engine.processClick(initialState, 0)
        assertThat(newState).isEqualTo(initialState)
    }

    @Test
    fun `when all opponent pieces are in mills, then removing a mill piece is allowed`() {
        // Hits the nested mill check exception
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_TWO
        board[1] = Player.PLAYER_TWO
        board[2] = Player.PLAYER_TWO // Player 2 only has these 3 pieces, all in one mill

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.REMOVING,
            unplacedPiecesPlayerTwo = 5,
            piecesOnBoardPlayerTwo = 3
        )
        val newState = engine.processClick(initialState, 0)
        assertThat(newState.board[0]).isEqualTo(Player.NONE)
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_TWO)
    }

    @Test
    fun `when player drops to 3 pieces on board, then their next phase is FLYING`() {
        val board = MutableList(24) { Player.NONE }

        // Player 1 needs at least 4 pieces so the engine doesn't declare GAME_OVER
        board[0] = Player.PLAYER_ONE
        board[3] = Player.PLAYER_ONE
        board[5] = Player.PLAYER_ONE
        board[7] = Player.PLAYER_ONE

        // Player 2 has exactly 3 pieces and 0 unplaced
        board[23] = Player.PLAYER_TWO
        board[22] = Player.PLAYER_TWO
        board[21] = Player.PLAYER_TWO

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 0,
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            piecesOnBoardPlayerOne = 4, // <-- Added this so Player 1 stays alive!
            piecesOnBoardPlayerTwo = 3
        )

        // P1 makes a valid move from 0 to 1
        val newState = engine.processClick(initialState, 1)

        // It is now Player 2's turn, and because they only have 3 pieces, they should be FLYING
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_TWO)
        assertThat(newState.currentPhase).isEqualTo(Phase.FLYING)
    }
    @Test
    fun `when clicking already selected piece in MOVING phase, then it remains selected`() {
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 0 // Stein ist ausgewählt
        )

        val newState = engine.processClick(initialState, 0) // Klickt denselben Stein nochmal

        // Erwartung: Die Engine wählt ihn einfach erneut aus (bleibt also 0)
        assertThat(newState.selectedPieceIndex).isEqualTo(0)
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_ONE)
    }

    @Test
    fun `when clicking another own piece in MOVING phase, then selection changes`() {
        // Deckt ab: Spieler hat Stein A ausgewählt, klickt dann aber auf eigenen Stein B
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE
        board[1] = Player.PLAYER_ONE

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 0 // Stein 0 ist ausgewählt
        )

        val newState = engine.processClick(initialState, 1) // Klickt jetzt auf Stein 1

        assertThat(newState.selectedPieceIndex).isEqualTo(1) // Auswahl muss zu 1 wechseln
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_ONE)
    }

    @Test
    fun `when clicking opponent piece to select in MOVING phase, then state remains unchanged`() {
        // Deckt ab: Spieler versucht, in der Zugphase einen gegnerischen Stein anzuklicken
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_TWO

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = null
        )

        val newState = engine.processClick(initialState, 0)

        assertThat(newState).isEqualTo(initialState) // Nichts darf passieren
    }

    @Test
    fun `when moving selected piece to occupied spot, then state remains unchanged`() {
        // Deckt ab: Spieler versucht, seinen Stein auf ein Feld zu ziehen, wo schon ein Stein liegt
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE
        board[1] = Player.PLAYER_TWO // Feld 1 ist blockiert

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 0
        )

        val newState = engine.processClick(initialState, 1)

        assertThat(newState).isEqualTo(initialState) // Zug ungültig, nichts passiert
    }

    @Test
    fun `when Player 1 drops to 2 pieces, then Player 2 wins`() {
        // Deckt ab: Das Spielende, wenn Spieler 1 (statt Spieler 2) verliert
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE
        board[1] = Player.PLAYER_ONE
        board[2] = Player.PLAYER_ONE // Player 1 hat nur noch 3 Steine

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_TWO,
            currentPhase = Phase.REMOVING,
            unplacedPiecesPlayerOne = 0,
            piecesOnBoardPlayerOne = 3
        )

        // Player 2 schlägt einen Stein von Player 1
        val newState = engine.processClick(initialState, 0)

        assertThat(newState.currentPhase).isEqualTo(Phase.GAME_OVER)
        assertThat(newState.winner).isEqualTo(Player.PLAYER_TWO) // P2 muss gewinnen
    }

    @Test
    fun `when Player 1 has no moves left, then Player 2 wins`() {
        val board = MutableList(24) { Player.NONE }

        // P1 liegt isoliert auf den 4 Ecken des äußeren Rings
        board[0] = Player.PLAYER_ONE
        board[2] = Player.PLAYER_ONE
        board[4] = Player.PLAYER_ONE
        board[6] = Player.PLAYER_ONE

        // P2 blockiert bereits 3 der 4 Ausgänge für P1
        board[3] = Player.PLAYER_TWO
        board[5] = Player.PLAYER_TWO
        board[7] = Player.PLAYER_TWO

        // P2 steht auf Feld 9 und wird gleich auf Feld 1 ziehen, um die Falle zu schließen!
        board[9] = Player.PLAYER_TWO

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_TWO,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 9, // P2 wählt seinen Stein auf 9
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            piecesOnBoardPlayerOne = 4, // Kein Flugmodus für P1
            piecesOnBoardPlayerTwo = 4
        )

        // ACT: P2 zieht von der 9 auf die 1
        val newState = engine.processClick(initialState, 1)

        // P2 schließt KEINE Mühle. ABER P1 ist jetzt zu 100 % eingesperrt:
        // 0 -> blockiert durch 1 und 7
        // 2 -> blockiert durch 1 und 3
        // 4 -> blockiert durch 3 und 5
        // 6 -> blockiert durch 5 und 7
        assertThat(newState.currentPhase).isEqualTo(Phase.GAME_OVER)
        assertThat(newState.winner).isEqualTo(Player.PLAYER_TWO)
    }
    @Test
    fun `when placing last unplaced piece without mill, then phase changes to MOVING`() {
        val board = MutableList(24) { Player.NONE }

        // Give P2 some pieces on the board so they aren't "trapped" and the game continues
        board[10] = Player.PLAYER_TWO
        board[11] = Player.PLAYER_TWO
        board[12] = Player.PLAYER_TWO
        board[13] = Player.PLAYER_TWO

        // P1 has some pieces too, and will place their last one on 0
        board[3] = Player.PLAYER_ONE
        board[4] = Player.PLAYER_ONE
        board[5] = Player.PLAYER_ONE

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.PLACING,
            unplacedPiecesPlayerOne = 1, // P1's last piece
            unplacedPiecesPlayerTwo = 0, // P2 is already done placing
            piecesOnBoardPlayerOne = 3,
            piecesOnBoardPlayerTwo = 4
        )

        val newState = engine.processClick(initialState, 0) // P1 places on 0

        assertThat(newState.unplacedPiecesPlayerOne).isEqualTo(0)
        assertThat(newState.currentPhase).isEqualTo(Phase.MOVING) // Engine now safely goes to MOVING
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_TWO)
    }

    @Test
    fun `when removing piece and no unplaced pieces left, then next phase is MOVING`() {
        val board = MutableList(24) { Player.NONE }

        // 0, 1, 2 ist eine geschlossene Mühle von Spieler 2
        board[0] = Player.PLAYER_TWO
        board[1] = Player.PLAYER_TWO
        board[2] = Player.PLAYER_TWO

        // 9 ist der freie Stein, den wir gleich schlagen werden
        board[9] = Player.PLAYER_TWO

        // NEU: 10 ist ein Extra-Stein, damit P2 nach dem Schlagen nicht in die Flugphase kommt!
        board[10] = Player.PLAYER_TWO

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.REMOVING,
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            piecesOnBoardPlayerOne = 4,
            piecesOnBoardPlayerTwo = 5 // START MIT 5 STEINEN (fällt nach dem Schlagen auf 4)
        )

        // ACT: P1 klickt auf die 9 (den freien Stein)
        val newState = engine.processClick(initialState, 9)

        assertThat(newState.board[9]).isEqualTo(Player.NONE)

        // Da P2 jetzt noch 4 Steine hat, geht das Spiel normal in die Zug-Phase über!
        assertThat(newState.currentPhase).isEqualTo(Phase.MOVING)
        assertThat(newState.currentTurn).isEqualTo(Player.PLAYER_TWO)
    }

    @Test
    fun `when mill is formed, then moves without capture counter is reset to 0`() {
        val board = MutableList(24) { Player.NONE }

        // Give P2 enough pieces to survive
        board[20] = Player.PLAYER_TWO
        board[21] = Player.PLAYER_TWO
        board[22] = Player.PLAYER_TWO
        board[23] = Player.PLAYER_TWO

        // P1 has pieces at 0 and 2.
        board[0] = Player.PLAYER_ONE
        board[2] = Player.PLAYER_ONE

        // P1 selects piece at 9. Point 9 is directly connected to 1!
        board[9] = Player.PLAYER_ONE

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 9,
            movesWithoutCaptureOrMill = 25, // We are 25 moves deep into a draw
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            piecesOnBoardPlayerOne = 3,
            piecesOnBoardPlayerTwo = 4
        )

        // ACT: P1 safely moves from 9 to 1, completing the mill (0, 1, 2)
        val newState = engine.processClick(initialState, 1)

        assertThat(newState.currentPhase).isEqualTo(Phase.REMOVING)
        assertThat(newState.movesWithoutCaptureOrMill).isEqualTo(0) // Reset to 0!
    }
    @Test
    fun `when Player 2 has no moves left, then Player 1 wins and message says White`() {
        val board = MutableList(24) { Player.NONE }

        // P2 liegt isoliert auf den 4 Ecken
        board[0] = Player.PLAYER_TWO
        board[2] = Player.PLAYER_TWO
        board[4] = Player.PLAYER_TWO
        board[6] = Player.PLAYER_TWO

        // P1 blockiert 3 Ausgänge
        board[3] = Player.PLAYER_ONE
        board[5] = Player.PLAYER_ONE
        board[7] = Player.PLAYER_ONE

        // P1 steht auf 9 und zieht gleich auf 1
        board[9] = Player.PLAYER_ONE

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE, // P1 ist am Zug!
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 9,
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            piecesOnBoardPlayerOne = 4,
            piecesOnBoardPlayerTwo = 4
        )

        val newState = engine.processClick(initialState, 1)

        assertThat(newState.currentPhase).isEqualTo(Phase.GAME_OVER)
        assertThat(newState.winner).isEqualTo(Player.PLAYER_ONE)
        assertThat(newState.infoMessage).contains("White") // Deckt das versteckte "if" ab!
    }
    @Test
    fun `when flying selected piece to occupied spot, then state remains unchanged`() {
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE // Der Stein, der fliegen will
        board[23] = Player.PLAYER_TWO // Das Zielfeld ist von P2 blockiert!

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.FLYING,
            selectedPieceIndex = 0, // P1 wählt seinen Stein auf 0
            unplacedPiecesPlayerOne = 0,
            piecesOnBoardPlayerOne = 3 // Flugmodus!
        )

        // P1 versucht, auf das besetzte Feld 23 zu fliegen
        val newState = engine.processClick(initialState, 23)

        // Nichts darf passieren, der Blockade-Check muss den Zug abbrechen
        assertThat(newState).isEqualTo(initialState)
    }
    @Test
    fun `when Player 1 moves and completely traps Player 2, then Player 1 wins`() {
        val board = MutableList(24) { Player.NONE }

        // P2 hat 4 Steine (Kein Flugmodus!) und steht isoliert
        board[0] = Player.PLAYER_TWO // Nachbarn: 1, 7
        board[2] = Player.PLAYER_TWO // Nachbarn: 1, 3
        board[4] = Player.PLAYER_TWO // Nachbarn: 3, 5
        board[8] = Player.PLAYER_TWO // Nachbarn: 9, 15

        // P1 blockiert bereits alle Ausgänge, bis auf das Feld 7
        board[1] = Player.PLAYER_ONE // Blockiert 0 und 2
        board[3] = Player.PLAYER_ONE // Blockiert 2 und 4
        board[5] = Player.PLAYER_ONE // Blockiert 4
        board[9] = Player.PLAYER_ONE // Blockiert 8
        board[15] = Player.PLAYER_ONE // Blockiert 8

        // P1 steht auf der 6 und zieht gleich auf die 7, um die Falle endgültig zu schließen!
        board[6] = Player.PLAYER_ONE

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 6, // P1 wählt seinen Stein auf der 6
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            piecesOnBoardPlayerOne = 6,
            piecesOnBoardPlayerTwo = 4
        )

        // ACT: P1 zieht von 6 auf 7. Das ist gültig, bildet keine Mühle und schließt die Lücke!
        val newState = engine.processClick(initialState, 7)

        // ASSERT: P2 ist jetzt restlos eingesperrt -> Game Over, White wins!
        assertThat(newState.currentPhase).isEqualTo(Phase.GAME_OVER)
        assertThat(newState.winner).isEqualTo(Player.PLAYER_ONE)
        assertThat(newState.infoMessage).contains("White")
    }
    @Test
    fun `when P1 removes piece and leaves P2 trapped, then P1 wins`() {
        val board = MutableList(24) { Player.NONE }

        // P2 hat 4 Steine auf dem inneren Ring (alle 4 sind eingesperrt)
        board[16] = Player.PLAYER_TWO // Nachbarn: 17, 23
        board[18] = Player.PLAYER_TWO // Nachbarn: 17, 19
        board[20] = Player.PLAYER_TWO // Nachbarn: 19, 21
        board[22] = Player.PLAYER_TWO // Nachbarn: 21, 23

        // P1 blockiert genau diese Nachbarn
        board[17] = Player.PLAYER_ONE
        board[19] = Player.PLAYER_ONE
        board[21] = Player.PLAYER_ONE
        board[23] = Player.PLAYER_ONE

        // P2 hat noch einen 5. Stein ganz weit weg auf Feld 0
        board[0] = Player.PLAYER_TWO

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE, // P1 ist am Zug zum Löschen!
            currentPhase = Phase.REMOVING,
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            piecesOnBoardPlayerOne = 4,
            piecesOnBoardPlayerTwo = 5 // Reduziert sich gleich auf 4
        )

        // ACT: P1 schlägt den 5. Stein auf Feld 0
        val newState = engine.processClick(initialState, 0)

        // ASSERT: P2 hat nur noch 4 eingesperrte Steine -> Game Over!
        assertThat(newState.currentPhase).isEqualTo(Phase.GAME_OVER)
        assertThat(newState.winner).isEqualTo(Player.PLAYER_ONE)
        assertThat(newState.infoMessage).contains("White") // Testet die P1 String-Verzweigung
    }

    @Test
    fun `when P2 removes piece and leaves P1 trapped, then P2 wins`() {
        val board = MutableList(24) { Player.NONE }

        // Genau umgekehrt: P1 ist auf dem inneren Ring eingesperrt
        board[16] = Player.PLAYER_ONE
        board[18] = Player.PLAYER_ONE
        board[20] = Player.PLAYER_ONE
        board[22] = Player.PLAYER_ONE

        // P2 blockiert P1
        board[17] = Player.PLAYER_TWO
        board[19] = Player.PLAYER_TWO
        board[21] = Player.PLAYER_TWO
        board[23] = Player.PLAYER_TWO

        // P1s freier Stein auf 0
        board[0] = Player.PLAYER_ONE

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_TWO, // P2 ist am Zug zum Löschen!
            currentPhase = Phase.REMOVING,
            unplacedPiecesPlayerOne = 0,
            unplacedPiecesPlayerTwo = 0,
            piecesOnBoardPlayerOne = 5,
            piecesOnBoardPlayerTwo = 4
        )

        // ACT: P2 schlägt den Stein von P1 auf Feld 0
        val newState = engine.processClick(initialState, 0)

        // ASSERT: Game Over, Black wins!
        assertThat(newState.currentPhase).isEqualTo(Phase.GAME_OVER)
        assertThat(newState.winner).isEqualTo(Player.PLAYER_TWO)
        assertThat(newState.infoMessage).contains("Black") // Testet die P2 String-Verzweigung
    }
    @Test
    fun `when moving piece to non-adjacent spot in MOVING phase, then isValidMove returns false`() {
        // ARRANGE: Normaler Moving-Modus (kein Fliegen)
        val board = MutableList(24) { Player.NONE }
        board[0] = Player.PLAYER_ONE // Stein auf Ecke oben links

        val initialState = GameState(
            board = board,
            currentTurn = Player.PLAYER_ONE,
            currentPhase = Phase.MOVING,
            selectedPieceIndex = 0, // Stein auf 0 ist ausgewählt
            unplacedPiecesPlayerOne = 0,
            piecesOnBoardPlayerOne = 4 // Mehr als 3 Steine -> kein Flugmodus
        )

        // ACT: Versuch, von 0 auf 2 zu ziehen (nicht benachbart laut AdjacencyMap)
        // Laut BoardDefinitions.adjacencyMap ist 0 nur mit 1 und 7 verbunden
        val newState = engine.processClick(initialState, 2)

        // ASSERT: Der Zug muss abgelehnt werden, da adjacencyMap[0].contains(2) == false ist
        assertThat(newState).isEqualTo(initialState)
    }
}