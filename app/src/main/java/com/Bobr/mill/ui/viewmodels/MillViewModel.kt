package com.Bobr.mill.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.Bobr.mill.domain.engine.BotDifficulty
import com.Bobr.mill.domain.engine.MillGameEngine
import com.Bobr.mill.domain.models.GameState
import com.Bobr.mill.domain.models.Phase
import com.Bobr.mill.domain.models.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MillViewModel : ViewModel() {
    // Dieser Draht wird später von der MainActivity verbunden
    var onMatchFinished: ((won: Boolean, piecesTaken: Int, piecesLost: Int) -> Unit)? = null
    private val engine = MillGameEngine()
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    // Beobachtbare Rolle: PLAYER_ONE, PLAYER_TWO oder NONE (für Lokales Spiel)
    private val _myLocalRole = MutableStateFlow(Player.NONE)
    val myLocalRole: StateFlow<Player> = _myLocalRole.asStateFlow()

    var isLocalMode: Boolean = false
    var isBotMode = false
    var botDifficulty = BotDifficulty.EASY
    var botRole = Player.PLAYER_TWO
    private val botEngine = com.Bobr.mill.domain.engine.BotEngine()
    private var botJob: kotlinx.coroutines.Job? = null

    fun setLocalRole(player: Player) {
        _myLocalRole.value = player
    }

    fun tryLocalMove(pointIndex: Int, isBotClick: Boolean = false): Boolean {
        // Wenn wir online sind und nicht dran sind, abbrechen
        if (!isLocalMode && state.value.currentTurn != myLocalRole.value) {
            return false
        }

        // SCHUTZ: Wenn der Bot dran ist, darfst du (der Spieler) nicht klicken!
        if (isBotMode && state.value.currentTurn == botRole && !isBotClick) {
            return false
        }

        val currentState = state.value
        val newState = engine.processClick(currentState, pointIndex)

        // Hat sich das Spielfeld verändert? Dann war der Zug gültig!
        if (currentState != newState) {
            _state.value = newState
            if (newState.winner != null && currentState.winner == null) {
                // Wir berechnen kurz die Steine und funken an die MainActivity
                val iWon = newState.winner == myLocalRole.value
                val myPieces = if (myLocalRole.value == Player.PLAYER_ONE) newState.piecesOnBoardPlayerOne else newState.piecesOnBoardPlayerTwo
                val oppPieces = if (myLocalRole.value == Player.PLAYER_ONE) newState.piecesOnBoardPlayerTwo else newState.piecesOnBoardPlayerOne

                // Wir tun hier so, als wären alle nicht mehr auf dem Brett befindlichen Steine "verloren/genommen" worden.
                // (Maximal 9 Steine pro Spieler)
                val piecesLost = 9 - myPieces
                val piecesTaken = 9 - oppPieces

                onMatchFinished?.invoke(iWon, piecesTaken, piecesLost)
            }
            // BOT TRIGGER: Wir wecken den Bot NUR, wenn der SPIELER geklickt hat
            // und dadurch der Bot an die Reihe kommt!
            if (isBotMode && !isBotClick && newState.currentTurn == botRole && newState.currentPhase != Phase.GAME_OVER) {
                triggerBotMove()
            }
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
        if (isBotMode) {
            rpsOpponentChoice.value = (1..3).random() // Bot wählt sofort zufällig
        }
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
                rpsWinnerRole.value = myLocalRole.value
            } else {
                val oppRole = if (myLocalRole.value == Player.PLAYER_ONE) Player.PLAYER_TWO else Player.PLAYER_ONE
                rpsWinnerRole.value = oppRole

                // WENN DER BOT GEWINNT: Er nimmt immer Weiß und fängt an!
                if (isBotMode) {
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(1500) // Kurz warten, damit man sieht, dass man verloren hat
                        applyColorChoice(opponentChoseWhite = true)
                        botRole = Player.PLAYER_ONE
                        triggerBotMove()
                    }
                }
            }
        }
    }

    fun finalizeMyColorChoice(iChoseWhite: Boolean) {
        setLocalRole(if (iChoseWhite) Player.PLAYER_ONE else Player.PLAYER_TWO)
        botRole = if (iChoseWhite) Player.PLAYER_TWO else Player.PLAYER_ONE
        isRpsActive.value = false
        if (isBotMode && botRole == Player.PLAYER_ONE) triggerBotMove()
    }

    fun applyColorChoice(opponentChoseWhite: Boolean) {
        setLocalRole(if (opponentChoseWhite) Player.PLAYER_TWO else Player.PLAYER_ONE)
        isRpsActive.value = false
    }

    fun triggerBotMove() {
        if (!isBotMode || state.value.currentPhase == Phase.GAME_OVER) return
        if (state.value.currentTurn != botRole) return

        botJob?.cancel()
        botJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            // RECHENINTENSIV: Auf einem Hintergrund-Thread ausführen, damit die UI flüssig bleibt!
            val clicks = withContext(Dispatchers.Default) {
                botEngine.calculateClicks(
                    state = state.value,
                    botRole = botRole,
                    difficulty = botDifficulty
                )
            }

            // KÜNSTLICHE BEDENKZEIT MESSEN
            val elapsedTime = System.currentTimeMillis() - startTime
            val minThinkTime = when (botDifficulty) {
                BotDifficulty.EASY -> 1200L   // Leicht: Bot denkt absichtlich mind. 1.2 Sekunden
                BotDifficulty.MEDIUM -> 1500L // Mittel: Bot denkt mind. 1.5 Sekunden
                BotDifficulty.HARD -> 200L    // Schwer: Reizt die Zeit eh aus, zieht sofort wenn fertig
            }

            // Wenn der Bot schneller war als die Mindest-Bedenkzeit, warten wir den Rest ab
            if (elapsedTime < minThinkTime) {
                delay(minThinkTime - elapsedTime)
            }

            for (click in clicks) {
                tryLocalMove(click, isBotClick = true)
                delay(700) // 0.7s Pause, wenn der Bot 2 Klicks hintereinander macht (Mühle schließen -> Schlagen)
            }
        }
    }
}