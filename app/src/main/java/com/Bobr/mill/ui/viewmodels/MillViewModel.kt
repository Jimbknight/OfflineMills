package com.Bobr.mill.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.Bobr.mill.domain.engine.MillGameEngine
import com.Bobr.mill.domain.models.GameState
import com.Bobr.mill.domain.models.Phase
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

    fun concedeGame(concedingPlayer: Player) {
        // Wenn Spieler 1 aufgibt, gewinnt Spieler 2 (und umgekehrt)
        val winner = if (concedingPlayer == Player.PLAYER_ONE) Player.PLAYER_TWO else Player.PLAYER_ONE

        _state.update { currentState ->
            currentState.copy(
                currentPhase = Phase.GAME_OVER,
                winner = winner,
                infoMessage = "Game Over! ${if (winner == Player.PLAYER_ONE) "White" else "Black"} wins (Opponent conceded)."
            )
        }
    }
    // --- SCHERE STEIN PAPIER LOGIK ---
    val isRpsActive = kotlinx.coroutines.flow.MutableStateFlow(false)
    val rpsMyChoice = kotlinx.coroutines.flow.MutableStateFlow(0) // 1=Stein, 2=Papier, 3=Schere
    val rpsOpponentChoice = kotlinx.coroutines.flow.MutableStateFlow(0)
    val rpsWinnerRole = kotlinx.coroutines.flow.MutableStateFlow<Player?>(null)
    val isRpsTie = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun startRps() {
        isRpsActive.value = true
        resetRpsRound()
        rpsWinnerRole.value = null
    }

    fun resetRpsRound() {
        rpsMyChoice.value = 0
        rpsOpponentChoice.value = 0
        isRpsTie.value = false
    }

    fun setMyRpsChoice(choice: Int) {
        rpsMyChoice.value = choice
        checkRpsResult()
    }

    fun setOpponentRpsChoice(choice: Int) {
        rpsOpponentChoice.value = choice
        checkRpsResult()
    }

    private fun checkRpsResult() {
        val me = rpsMyChoice.value
        val opp = rpsOpponentChoice.value
        if (me != 0 && opp != 0) {
            if (me == opp) {
                isRpsTie.value = true
            } else if ((me == 1 && opp == 3) || (me == 2 && opp == 1) || (me == 3 && opp == 2)) {
                rpsWinnerRole.value = myLocalRole.value // Ich gewinne
            } else {
                // Gegner gewinnt
                val oppRole = if (myLocalRole.value == Player.PLAYER_ONE) Player.PLAYER_TWO else Player.PLAYER_ONE
                rpsWinnerRole.value = oppRole
            }
        }
    }

    fun finalizeMyColorChoice(iChoseWhite: Boolean) {
        setLocalRole(if (iChoseWhite) Player.PLAYER_ONE else Player.PLAYER_TWO)
        isRpsActive.value = false
    }

    fun applyColorChoice(opponentChoseWhite: Boolean) {
        setLocalRole(if (opponentChoseWhite) Player.PLAYER_TWO else Player.PLAYER_ONE)
        isRpsActive.value = false
    }
}