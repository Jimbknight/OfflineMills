package com.Bobr.mill.domain.models

data class GameState(
    // The 24 points on the board, starting all empty
    val board: List<Player> = List(24) { Player.NONE },

    // Who is currently making a move?
    val currentTurn: Player = Player.PLAYER_ONE,

    // What phase is the current player in?
    val currentPhase: Phase = Phase.PLACING,

    // How many pieces do they have left in their hand to place?
    val unplacedPiecesPlayerOne: Int = 9,
    val unplacedPiecesPlayerTwo: Int = 9,

    // How many pieces do they have alive on the board?
    val piecesOnBoardPlayerOne: Int = 0,
    val piecesOnBoardPlayerTwo: Int = 0,

    // If someone is in the MOVING phase, they must tap their own piece first to select it
    val selectedPieceIndex: Int? = null,

    // Optional message to display to the UI
    val infoMessage: String = "Player 1's turn to place a piece."
)