package com.Bobr.mill.domain.models

enum class Phase {
    PLACING,  // Phase 1: Putting the initial pieces on the board
    MOVING,   // Phase 2: Sliding pieces to adjacent spots
    FLYING,   // Phase 3: (Optional/Conditional) Moving to any empty spot
    REMOVING, // Triggered when a Mill is formed; player must tap an opponent's piece
    GAME_OVER // Someone won or it's a draw
}