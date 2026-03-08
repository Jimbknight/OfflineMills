package com.Bobr.mill.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.Bobr.mill.domain.engine.MillGameEngine
import com.Bobr.mill.domain.models.GameState
import com.Bobr.mill.domain.models.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MillViewModel : ViewModel() {
    private val engine = MillGameEngine()
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    // Beobachtbare Rolle: PLAYER_ONE, PLAYER_TWO oder NONE (für Lokales Spiel)
    private val _myLocalRole = MutableStateFlow(Player.NONE)
    val myLocalRole: StateFlow<Player> = _myLocalRole.asStateFlow()

    var isLocalMode: Boolean = false

    fun setLocalRole(player: Player) {
        _myLocalRole.value = player
    }

    fun tryLocalMove(pointIndex: Int): Boolean {
        // Im lokalen Modus darf man immer ziehen, sonst nur wenn man dran ist
        if (!isLocalMode && state.value.currentTurn != _myLocalRole.value) {
            return false
        }

        val currentState = state.value
        val newState = engine.processClick(currentState, pointIndex)

        if (currentState != newState) {
            _state.value = newState
            return true
        }
        return false
    }

    fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.OnNetworkMoveReceived -> {
                _state.update { currentState ->
                    engine.processClick(currentState, event.pointIndex)
                }
            }
            is GameEvent.OnResetClicked -> {
                _state.update { GameState() }
            }
            else -> {}
        }
    }

    fun generateRandomName(): String {
        val adjectives = listOf("Fast", "Sneaky", "Mighty", "Crazy", "Epic", "Hidden", "Brave", "Wild", "Sleepy", "Fierce", "Giant", "Lucky")
        return "${adjectives.random()} Bobr"
    }
}