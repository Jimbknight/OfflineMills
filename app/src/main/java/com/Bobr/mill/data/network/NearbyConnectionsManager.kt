package com.Bobr.mill.data.network

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

data class DiscoveredGame(val endpointId: String, val hostName: String)

class NearbyConnectionsManager(
    private val context: Context,
    private val onConnectionStatusChanged: (Boolean, String) -> Unit,
    private val onMoveReceived: (Int) -> Unit,
    private val onRoleAssigned: (isPlayerOne: Boolean) -> Unit,
    private val onGamesDiscovered: (List<DiscoveredGame>) -> Unit
) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "com.Bobr.mill.LOBBY"

    private var connectedEndpointId: String? = null
    private val discoveredGames = mutableListOf<DiscoveredGame>()

    // Speichert, ob der Host sich für Weiß (Player 1) oder Schwarz (Player 2) entschieden hat
    private var hostPlaysAsPlayerOne: Boolean = true

    fun startHosting(hostName: String, hostPlaysAsPlayerOne: Boolean) {
        this.hostPlaysAsPlayerOne = hostPlaysAsPlayerOne
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()

        connectionsClient.startAdvertising(hostName, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnSuccessListener { onConnectionStatusChanged(false, "Hosting...") }
            .addOnFailureListener { onConnectionStatusChanged(false, "Fehler beim Hosten.") }
    }

    fun startDiscovering() {
        discoveredGames.clear()
        onGamesDiscovered(emptyList())
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()

        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener { onConnectionStatusChanged(false, "Suche...") }
    }

    // Brich das Suchen oder Hosten ab
    fun cancel() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        discoveredGames.clear()
        onGamesDiscovered(emptyList())
        onConnectionStatusChanged(false, "")
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            discoveredGames.add(DiscoveredGame(endpointId, info.endpointName))
            onGamesDiscovered(discoveredGames.toList()) // Aktualisiert die Liste im UI
        }
        override fun onEndpointLost(endpointId: String) {
            discoveredGames.removeAll { it.endpointId == endpointId }
            onGamesDiscovered(discoveredGames.toList())
        }
    }

    fun joinGame(endpointId: String, playerName: String) {
        connectionsClient.requestConnection(playerName, endpointId, connectionLifecycleCallback)
        onConnectionStatusChanged(false, "Verbinde...")
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpointId = endpointId
                connectionsClient.stopAdvertising()
                connectionsClient.stopDiscovery()

                onConnectionStatusChanged(true, "Verbunden!")

                // Host teilt dem Joiner mit, wer wer ist!
                val roleMessage = if (hostPlaysAsPlayerOne) "ROLE:PLAYER_TWO" else "ROLE:PLAYER_ONE"
                connectionsClient.sendPayload(endpointId, Payload.fromBytes(roleMessage.toByteArray()))

                // Host setzt seine eigene Rolle
                onRoleAssigned(hostPlaysAsPlayerOne)
            } else {
                onConnectionStatusChanged(false, "Verbindung fehlgeschlagen.")
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpointId = null
            // Hier fangen wir den Verbindungsabbruch ab!
            onConnectionStatusChanged(false, "Verbindung verloren!")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val message = payload.asBytes()?.let { String(it) } ?: return

            if (message.startsWith("ROLE:")) {
                val isPlayerOne = message.split(":")[1] == "PLAYER_ONE"
                onRoleAssigned(isPlayerOne)
            } else {
                message.toIntOrNull()?.let { onMoveReceived(it) }
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    fun sendMove(pointIndex: Int) {
        connectedEndpointId?.let {
            connectionsClient.sendPayload(it, Payload.fromBytes(pointIndex.toString().toByteArray()))
        }
    }

    fun disconnect() {
        connectionsClient.stopAllEndpoints()
        connectedEndpointId = null
    }
}