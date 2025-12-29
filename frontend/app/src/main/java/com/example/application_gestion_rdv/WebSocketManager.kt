package com.example.application_gestion_rdv

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONObject
import com.example.application_gestion_rdv.models.Message
import java.util.concurrent.TimeUnit

class WebSocketManager(private val userId: Int) {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) 
        .build()

    private val _messages = MutableStateFlow<Message?>(null)
    val messages: StateFlow<Message?> = _messages

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var reconnectJob: Job? = null
    private var shouldReconnect = true
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    companion object {
        private const val TAG = "WebSocketManager"
    }

    fun connect(baseUrl: String) {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING) {
            Log.w(TAG, "Already connected or connecting")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        shouldReconnect = true
        reconnectAttempts = 0

        // ✅ Convertir http/https en ws/wss
        val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
        val fullUrl = "$wsUrl/chat/ws/$userId"

        Log.d(TAG, "Connecting to WebSocket: $fullUrl")

        val request = Request.Builder()
            .url(fullUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempts = 0
                Log.d(TAG, "✅ WebSocket Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d(TAG, "📩 Received: $text")
                    val json = JSONObject(text)
                    val type = json.getString("type")

                    when (type) {
                        "new_message" -> {
                            val messageJson = json.getJSONObject("message")
                            val message = Message(
                                id = messageJson.getInt("id"),
                                conversationId = messageJson.getInt("conversation_id"),
                                senderId = messageJson.getInt("sender_id"),
                                senderName = messageJson.optString("sender_name", null),
                                content = messageJson.getString("content"),
                                messageType = messageJson.getString("message_type"),
                                fileUrl = messageJson.optString("file_url", null)
                                    .takeIf { it.isNotEmpty() },
                                createdAt = messageJson.getString("created_at"),
                                isRead = messageJson.getBoolean("is_read")
                            )
                            _messages.value = message
                            Log.d(TAG, "✅ Message parsed successfully")
                        }
                        "error" -> {
                            val errorMessage = json.getString("message")
                            _error.value = errorMessage
                            Log.e(TAG, "❌ Server error: $errorMessage")
                        }
                        else -> {
                            Log.w(TAG, "⚠️ Unknown message type: $type")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing message", e)
                    _error.value = "Erreur de parsing: ${e.message}"
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.e(TAG, "❌ WebSocket Error: ${t.message}", t)
                _error.value = "Connexion échouée: ${t.message}"

                // ✅ Tentative de reconnexion automatique
                if (shouldReconnect && reconnectAttempts < maxReconnectAttempts) {
                    scheduleReconnect(baseUrl)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.d(TAG, "🔌 WebSocket Closed: $reason (code: $code)")

                // ✅ Reconnexion si fermeture anormale
                if (shouldReconnect && code != 1000 && reconnectAttempts < maxReconnectAttempts) {
                    scheduleReconnect(baseUrl)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "🔌 WebSocket Closing: $reason (code: $code)")
            }
        })
    }

    private fun scheduleReconnect(baseUrl: String) {
        reconnectAttempts++
        val delay = minOf(2000L * reconnectAttempts, 30000L) // Max 30s

        Log.d(TAG, "🔄 Reconnecting in ${delay}ms (attempt $reconnectAttempts/$maxReconnectAttempts)")

        reconnectJob?.cancel()
        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            delay(delay)
            if (shouldReconnect) {
                connect(baseUrl)
            }
        }
    }

    fun sendMessage(conversationId: Int, content: String, messageType: String = "text"): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.e(TAG, "❌ Cannot send message: WebSocket not connected")
            _error.value = "Non connecté au serveur"
            return false
        }

        return try {
            val json = JSONObject().apply {
                put("conversation_id", conversationId)
                put("content", content)
                put("message_type", messageType)
            }

            val sent = webSocket?.send(json.toString()) ?: false
            if (sent) {
                Log.d(TAG, "📤 Message sent: $json")
            } else {
                Log.e(TAG, "❌ Failed to send message")
                _error.value = "Échec de l'envoi"
            }
            sent
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending message", e)
            _error.value = "Erreur d'envoi: ${e.message}"
            false
        }
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        reconnectJob = null

        webSocket?.close(1000, "User disconnect")
        webSocket = null

        _connectionState.value = ConnectionState.DISCONNECTED
        Log.d(TAG, "🔌 WebSocket disconnected by user")
    }

    fun clearError() {
        _error.value = null
    }

    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED
    }
}

enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    CONNECTING
}