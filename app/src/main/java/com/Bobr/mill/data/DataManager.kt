package com.Bobr.mill.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DataManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("MillAppPrefs", Context.MODE_PRIVATE)

    // --- NEU: Live-Sender für Jetpack Compose ---
    private val _playerNameFlow = MutableStateFlow(prefs.getString("PLAYER_NAME", "") ?: "")
    val playerNameFlow: StateFlow<String> = _playerNameFlow.asStateFlow()

    private val _isGoogleLoginFlow = MutableStateFlow(prefs.getBoolean("IS_GOOGLE_LOGIN", false))
    val isGoogleLoginFlow: StateFlow<Boolean> = _isGoogleLoginFlow.asStateFlow()

    // --- DEINE ALTEN VARIABLEN (aber jetzt funken sie Updates!) ---
    var playerName: String
        get() = prefs.getString("PLAYER_NAME", "") ?: ""
        set(value) {
            prefs.edit().putString("PLAYER_NAME", value).apply()
            _playerNameFlow.value = value // Sagt Compose: "Neu zeichnen!"
        }

    var isGoogleLogin: Boolean
        get() = prefs.getBoolean("IS_GOOGLE_LOGIN", false)
        set(value) {
            prefs.edit().putBoolean("IS_GOOGLE_LOGIN", value).apply()
            _isGoogleLoginFlow.value = value // Sagt Compose: "Schloss verriegeln/öffnen!"
        }

    // ... hier unten bleiben totalWins, totalLosses usw. GANZ GENAU SO wie sie vorher waren ...
    var totalWins: Int
        get() = prefs.getInt("TOTAL_WINS", 0)
        set(value) = prefs.edit().putInt("TOTAL_WINS", value).apply()

    var totalLosses: Int
        get() = prefs.getInt("TOTAL_LOSSES", 0)
        set(value) = prefs.edit().putInt("TOTAL_LOSSES", value).apply()

    var totalPiecesTaken: Int
        get() = prefs.getInt("TOTAL_PIECES_TAKEN", 0)
        set(value) = prefs.edit().putInt("TOTAL_PIECES_TAKEN", value).apply()

    var totalPiecesLost: Int
        get() = prefs.getInt("TOTAL_PIECES_LOST", 0)
        set(value) = prefs.edit().putInt("TOTAL_PIECES_LOST", value).apply()

    // --- NEU: Lautstärke für Effekte und Musik (Standard: 100% bzw. 50%) ---
    var sfxVolume: Float
        get() = prefs.getFloat("SFX_VOLUME", 1.0f)
        set(value) = prefs.edit().putFloat("SFX_VOLUME", value).apply()

    var musicVolume: Float
        get() = prefs.getFloat("MUSIC_VOLUME", 0.5f)
        set(value) = prefs.edit().putFloat("MUSIC_VOLUME", value).apply()

    var matchHistory: List<String>
        get() {
            val savedString = prefs.getString("MATCH_HISTORY_LIST", "") ?: ""
            if (savedString.isBlank()) return emptyList()
            return savedString.split(";;;")
        }
        set(value) {
            prefs.edit().putString("MATCH_HISTORY_LIST", value.joinToString(";;;")).apply()
        }

    // --- NEU: ACHIEVEMENTS (Automatisch berechnet) ---
    val achFirstSteps: Boolean get() = totalWins > 0
    val achMillBuilder: Boolean get() = totalPiecesTaken >= 50
    val achLumberjack: Boolean get() = totalPiecesTaken >= 250

    // --- NEU: ACHIEVEMENTS (Gespeichert) ---
    var achChallenger: Boolean
        get() = prefs.getBoolean("ACH_CHALLENGER", false)
        set(value) = prefs.edit().putBoolean("ACH_CHALLENGER", value).apply()

    var achGrandmaster: Boolean
        get() = prefs.getBoolean("ACH_GRANDMASTER", false)
        set(value) = prefs.edit().putBoolean("ACH_GRANDMASTER", value).apply()

    var achUntouchable: Boolean
        get() = prefs.getBoolean("ACH_UNTOUCHABLE", false)
        set(value) = prefs.edit().putBoolean("ACH_UNTOUCHABLE", value).apply()
    // --- NEU: ERWEITERTE ACHIEVEMENTS ---
    val achVeteran: Boolean get() = (totalWins + totalLosses) >= 50

    var achPerfection: Boolean
        get() = prefs.getBoolean("ACH_PERFECTION", false)
        set(value) = prefs.edit().putBoolean("ACH_PERFECTION", value).apply()

    var achJailer: Boolean
        get() = prefs.getBoolean("ACH_JAILER", false)
        set(value) = prefs.edit().putBoolean("ACH_JAILER", value).apply()

    var achComeback: Boolean
        get() = prefs.getBoolean("ACH_COMEBACK", false)
        set(value) = prefs.edit().putBoolean("ACH_COMEBACK", value).apply()
    fun getOpponentWins(opponentName: String): Int {
        return prefs.getInt("OPP_WINS_$opponentName", 0)
    }

    fun getOpponentLosses(opponentName: String): Int {
        return prefs.getInt("OPP_LOSSES_$opponentName", 0)
    }

    fun recordMatch(opponentName: String, won: Boolean, piecesTaken: Int, piecesLost: Int) {
        if (won) totalWins++ else totalLosses++

        if (won) {
            val currentOppWins = getOpponentWins(opponentName)
            prefs.edit().putInt("OPP_WINS_$opponentName", currentOppWins + 1).apply()
        } else {
            val currentOppLosses = getOpponentLosses(opponentName)
            prefs.edit().putInt("OPP_LOSSES_$opponentName", currentOppLosses + 1).apply()
        }

        totalPiecesTaken += piecesTaken
        totalPiecesLost += piecesLost

        val resultString = if (won) "WIN" else "LOSS"
        val historyEntry = "$opponentName|$resultString"

        val currentHistory = matchHistory.toMutableList()
        currentHistory.add(0, historyEntry)
        if (currentHistory.size > 20) currentHistory.removeAt(currentHistory.lastIndex)

        matchHistory = currentHistory
    }

    fun getWinRate(): Int {
        val total = totalWins + totalLosses
        if (total == 0) return 0
        return ((totalWins.toDouble() / total.toDouble()) * 100).toInt()
    }
}