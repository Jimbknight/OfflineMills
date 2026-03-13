package com.Bobr.mill.data

import android.app.Activity
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import org.json.JSONObject

class CloudSaveManager(private val activity: Activity, private val dataManager: DataManager) {

    private val saveFilename = "MillCloudSave"

    fun saveToCloud() {
        val json = JSONObject()
        json.put("totalWins", dataManager.totalWins)
        json.put("totalLosses", dataManager.totalLosses)
        json.put("totalPiecesTaken", dataManager.totalPiecesTaken)
        json.put("totalPiecesLost", dataManager.totalPiecesLost)

        // --- NEU: Achievements in die Cloud speichern ---
        json.put("achChallenger", dataManager.achChallenger)
        json.put("achGrandmaster", dataManager.achGrandmaster)
        json.put("achUntouchable", dataManager.achUntouchable)
        json.put("achPerfection", dataManager.achPerfection)
        json.put("achJailer", dataManager.achJailer)
        json.put("achComeback", dataManager.achComeback)
        val saveData = json.toString().toByteArray()

        val snapshotsClient = PlayGames.getSnapshotsClient(activity)
        snapshotsClient.open(saveFilename, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .addOnSuccessListener { dataOrConflict ->
                val snapshot = dataOrConflict.data
                if (snapshot != null) {
                    snapshot.snapshotContents.writeBytes(saveData)
                    val metadataChange = SnapshotMetadataChange.Builder()
                        .setDescription("Automatische Speicherung")
                        .build()
                    snapshotsClient.commitAndClose(snapshot, metadataChange)
                    Log.d("CloudSave", "Erfolgreich in der Cloud gespeichert!")
                }
            }
            .addOnFailureListener { e ->
                Log.e("CloudSave", "Fehler beim Speichern in der Cloud", e)
            }
    }

    fun loadFromCloud() {
        val snapshotsClient = PlayGames.getSnapshotsClient(activity)
        snapshotsClient.open(saveFilename, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .addOnSuccessListener { dataOrConflict ->
                val snapshot = dataOrConflict.data
                if (snapshot != null) {
                    try {
                        val bytes = snapshot.snapshotContents.readFully()
                        val jsonString = String(bytes)

                        if (jsonString.isNotEmpty()) {
                            val json = JSONObject(jsonString)

                            // --- NEU: Achievements aus der Cloud laden (OR-Logik) ---
                            // Erfolge kann man nicht "verlieren". Wenn die Cloud sagt "True", setzen wir es lokal auf True.
                            if (json.optBoolean("achChallenger", false)) dataManager.achChallenger = true
                            if (json.optBoolean("achGrandmaster", false)) dataManager.achGrandmaster = true
                            if (json.optBoolean("achUntouchable", false)) dataManager.achUntouchable = true
                            if (json.optBoolean("achPerfection", false)) dataManager.achPerfection = true
                            if (json.optBoolean("achJailer", false)) dataManager.achJailer = true
                            if (json.optBoolean("achComeback", false)) dataManager.achComeback = true
                            val cloudWins = json.optInt("totalWins", 0)
                            val cloudLosses = json.optInt("totalLosses", 0)

                            val localMatches = dataManager.totalWins + dataManager.totalLosses
                            val cloudMatches = cloudWins + cloudLosses

                            if (cloudMatches > localMatches) {
                                dataManager.totalWins = cloudWins
                                dataManager.totalLosses = cloudLosses
                                dataManager.totalPiecesTaken = json.optInt("totalPiecesTaken", 0)
                                dataManager.totalPiecesLost = json.optInt("totalPiecesLost", 0)
                                Log.d("CloudSave", "Lokale Daten wurden mit Cloud-Daten aktualisiert!")
                            } else if (localMatches > cloudMatches) {
                                saveToCloud()
                            } else {
                                Log.d("CloudSave", "Cloud und lokales Gerät sind auf dem gleichen Stand.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CloudSave", "Fehler beim Lesen der Cloud-Daten", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("CloudSave", "Fehler beim Laden aus der Cloud", e)
            }
    }
}