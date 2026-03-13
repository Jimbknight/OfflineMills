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
    val winner: Player? = null,
    val zobristHash: Long = 0L,
    val lastMoveFrom: Int? = null,
    val lastMoveTo: Int? = null,
    val movesWithoutCaptureOrMill: Int = 0,
    val boardHistory: Map<String, Int> = emptyMap(),
    val isDraw: Boolean = false
)