package com.Bobr.mill.data

import android.content.Context
import android.content.SharedPreferences

class DataManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("MillAppPrefs", Context.MODE_PRIVATE)

    var playerName: String
        get() = prefs.getString("PLAYER_NAME", "") ?: ""
        set(value) = prefs.edit().putString("PLAYER_NAME", value).apply()

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