package com.Bobr.mill.data.network

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.ByteBuffer

data class DiscoveredGame(val endpointId: String, val hostName: String)

class NearbyConnectionsManager(
    private val context: Context,
    private val onConnectionStatusChanged: (Boolean, String) -> Unit,
    private val onMoveReceived: (Int) -> Unit,
    private val onRoleAssigned: (Boolean) -> Unit,
    private val onGamesDiscovered: (List<DiscoveredGame>) -> Unit,
    private val onOpponentNameReceived: (String) -> Unit // NEU: Sendet den Namen an die MainActivity
) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_STAR
    private val serviceId = "com.Bobr.mill.SERVICE_ID"

    private var hostPlaysWhite = true
    private var isHosting = false
    private var opponentEndpointId: String? = null

    private val discoveredGamesMap = mutableMapOf<String, String>()

    // Merkt sich, ob WIR den Disconnect ausgelöst haben
    private var isDisconnectingLocally = false

    // --- 1. DATEN EMPFANGEN ---
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val receivedData = ByteBuffer.wrap(bytes).int
                when (receivedData) {
                    88 -> onRoleAssigned(false)
                    99 -> onRoleAssigned(true)
                    else -> onMoveReceived(receivedData)
                }
            }
        }
        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {}
    }

    // --- 2. VERBINDUNGSAUFBAU ---
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // NEU: Sobald der Handshake beginnt, schicken wir den echten Namen des Gegners raus
            onOpponentNameReceived(info.endpointName)

            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                opponentEndpointId = endpointId
                onConnectionStatusChanged(true, "Connected!")

                connectionsClient.stopAdvertising()
                connectionsClient.stopDiscovery()

                if (isHosting) {
                    onRoleAssigned(hostPlaysWhite)
                    val clientColorCode = if (hostPlaysWhite) 88 else 99
                    sendMove(clientColorCode)
                }
            } else {
                discoveredGamesMap.remove(endpointId)
                updateDiscoveredGamesList()
                onConnectionStatusChanged(false, "Game no longer available.")
            }
        }

        override fun onDisconnected(endpointId: String) {
            opponentEndpointId = null
            // Nur meckern, wenn der Gegner wirklich abgehauen ist!
            if (!isDisconnectingLocally) {
                onConnectionStatusChanged(false, "Opponent disconnected.")
            }
            isDisconnectingLocally = false // Wieder zurücksetzen
        }
    }

    // --- 3. SPIEL HOSTEN ---
    fun startHosting(userName: String, isWhite: Boolean) {
        isHosting = true
        hostPlaysWhite = isWhite
        onConnectionStatusChanged(false, "Hosting game... Waiting for players.")

        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(
            userName, serviceId, connectionLifecycleCallback, options
        ).addOnFailureListener {
            onConnectionStatusChanged(false, "Failed to start hosting.")
        }
    }

    // --- 4. SPIEL SUCHEN ---
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            discoveredGamesMap[endpointId] = info.endpointName
            updateDiscoveredGamesList()
        }

        override fun onEndpointLost(endpointId: String) {
            discoveredGamesMap.remove(endpointId)
            updateDiscoveredGamesList()
        }
    }

    fun startDiscovering() {
        isHosting = false
        discoveredGamesMap.clear()
        updateDiscoveredGamesList()
        onConnectionStatusChanged(false, "Discovering games...")

        connectionsClient.stopDiscovery()

        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(
            serviceId, endpointDiscoveryCallback, options
        ).addOnFailureListener {
            onConnectionStatusChanged(false, "Failed to discover games.")
        }
    }

    // --- 5. SPIEL BEITRETEN ---
    fun joinGame(endpointId: String, userName: String) {
        connectionsClient.stopDiscovery()
        onConnectionStatusChanged(false, "Connecting to host...")

        connectionsClient.requestConnection(
            userName, endpointId, connectionLifecycleCallback
        ).addOnFailureListener {
            discoveredGamesMap.remove(endpointId)
            updateDiscoveredGamesList()
            onConnectionStatusChanged(false, "Game is no longer available.")
        }
    }

    // --- 6. ZUG / CODE SENDEN ---
    fun sendMove(moveIndex: Int) {
        opponentEndpointId?.let { endpointId ->
            val bytes = ByteBuffer.allocate(4).putInt(moveIndex).array()
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
        }
    }

    private fun updateDiscoveredGamesList() {
        val list = discoveredGamesMap.map { DiscoveredGame(it.key, it.value) }
        onGamesDiscovered(list)
    }

    fun cancel() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        // Alte Lobby-Liste knallhart leeren!
        discoveredGamesMap.clear()
        updateDiscoveredGamesList()
        onConnectionStatusChanged(false, "")
    }

    fun disconnect() {
        isDisconnectingLocally = true // Wir sagen der App: WIR gehen jetzt!
        connectionsClient.stopAllEndpoints()
        opponentEndpointId = null
        isHosting = false
        // Alte Lobby-Liste auch beim Disconnect leeren!
        discoveredGamesMap.clear()
        updateDiscoveredGamesList()
        onConnectionStatusChanged(false, "")
    }
}