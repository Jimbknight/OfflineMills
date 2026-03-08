package com.Bobr.mill.ui.viewmodels

sealed interface GameEvent {
    // When they tap one of the 24 intersections
    data class OnPointClicked(val pointIndex: Int) : GameEvent
    data class OnNetworkMoveReceived(val pointIndex: Int) : GameEvent
    // When they hit a restart button
    object OnResetClicked : GameEvent
}