package com.Bobr.mill.data.network

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.*
import java.nio.ByteBuffer

data class DiscoveredGame(val endpointId: String, val hostName: String)

class NearbyConnectionsManager(
    private val context: Context,
    private val onConnectionStatusChanged: (Boolean, String) -> Unit,
    private val onMoveReceived: (Int) -> Unit,
    private val onRoleAssigned: (Boolean) -> Unit,
    private val onGamesDiscovered: (List<DiscoveredGame>) -> Unit
) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_STAR
    private val serviceId = "com.Bobr.mill.SERVICE_ID"

    private var hostPlaysWhite = true
    private var isHosting = false
    private var opponentEndpointId: String? = null

    private val discoveredGamesMap = mutableMapOf<String, String>()

    // --- NEU FÜR POLLING ---
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pollingJob: Job? = null

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
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                pollingJob?.cancel() // Polling stoppen bei Erfolg
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
                onConnectionStatusChanged(false, "Connection failed.")
            }
        }

        override fun onDisconnected(endpointId: String) {
            opponentEndpointId = null
            onConnectionStatusChanged(false, "Opponent disconnected.")
        }
    }

    // --- 3. SPIEL HOSTEN ---
    fun startHosting(userName: String, isWhite: Boolean) {
        pollingJob?.cancel() // Sicherstellen, dass Polling beim Hosten aus ist
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

    // --- 4. SPIEL SUCHEN (MIT POLLING) ---
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
        onConnectionStatusChanged(false, "Discovering games...")

        // Polling-Schleife: Alle 5 Sekunden Liste leeren und Suche neu starten
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                discoveredGamesMap.clear()
                updateDiscoveredGamesList()

                connectionsClient.stopDiscovery()
                val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
                connectionsClient.startDiscovery(
                    serviceId, endpointDiscoveryCallback, options
                ).addOnFailureListener {
                    onConnectionStatusChanged(false, "Failed to discover games.")
                }

                delay(5000) // 5 Sekunden warten, bevor die Liste aktualisiert wird
            }
        }
    }

    // --- 5. SPIEL BEITRETEN ---
    fun joinGame(endpointId: String, userName: String) {
        pollingJob?.cancel() // WICHTIG: Suche stoppen, während wir verbinden!
        connectionsClient.stopDiscovery()
        onConnectionStatusChanged(false, "Connecting to host...")

        connectionsClient.requestConnection(
            userName, endpointId, connectionLifecycleCallback
        ).addOnFailureListener {
            onConnectionStatusChanged(false, "Failed to send connection request.")
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
        pollingJob?.cancel()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        onConnectionStatusChanged(false, "")
    }

    fun disconnect() {
        pollingJob?.cancel()
        connectionsClient.stopAllEndpoints()
        opponentEndpointId = null
        isHosting = false
    }
}