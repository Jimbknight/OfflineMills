package com.Bobr.mill.domain.models

data class GameState(
    val board: List<Player> = List(24) { Player.NONE },
    val currentTurn: Player = Player.PLAYER_ONE,
    val currentPhase: Phase = Phase.PLACING,
    val unplacedPiecesPlayerOne: Int = 9,
    val unplacedPiecesPlayerTwo: Int = 9,
    val piecesOnBoardPlayerOne: Int = 0,
    val piecesOnBoardPlayerTwo: Int = 0,
    val selectedPieceIndex: Int? = null,
    val infoMessage: String = "Player 1's turn to place a piece.",

    // NEU: Zeigt an, wer gewonnen hat. Wenn null, läuft das Spiel noch.
    val winner: Player? = null
)