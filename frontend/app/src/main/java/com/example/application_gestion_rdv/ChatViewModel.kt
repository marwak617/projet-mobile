package com.example.application_gestion_rdv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.application_gestion_rdv.models.Message
import com.example.application_gestion_rdv.network.ChatApiService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

class ChatViewModel(
    private val userId: Int,
    private var conversationId: Int,
    private val apiService: ChatApiService
) : ViewModel() {

    private val webSocketManager = WebSocketManager(userId)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        connectWebSocket()
        loadMessages()
        observeNewMessages()
    }

    private fun connectWebSocket() {
        // TODO: Remplacer par votre URL réelle
        // Exemple: ws://192.168.1.100:8000
        val wsUrl = "ws://192.168.1.99:8000"  //  Changez cette IP
        webSocketManager.connect(wsUrl)
    }

    private fun observeNewMessages() {
        viewModelScope.launch {
            webSocketManager.messages.collect { newMessage ->
                newMessage?.let {
                    if (it.conversationId == conversationId) {
                        // Ajouter le nouveau message à la liste
                        _messages.value = _messages.value + it

                        // Marquer comme lu si l'utilisateur n'est pas l'expéditeur
                        if (it.senderId != userId) {
                            markAsRead()
                        }
                    }
                }
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d("ChatViewModel", "Loading messages for conversation $conversationId, user $userId")

                val result = apiService.getMessages(conversationId, userId)

                if (result.isSuccessful && result.body() != null) {
                    _messages.value = result.body()!!.reversed() // Ordre chronologique
                    Log.d("ChatViewModel", "Loaded ${_messages.value.size} messages")
                } else {
                    val errorMsg = "Erreur ${result.code()}: ${result.message()}"
                    _error.value = errorMsg
                    Log.e("ChatViewModel", "Error loading messages: $errorMsg")
                    Log.e("ChatViewModel", "Error body: ${result.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                val errorMsg = "Erreur réseau: ${e.message}"
                _error.value = errorMsg
                Log.e("ChatViewModel", "Exception loading messages", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Sending message: $content")
                webSocketManager.sendMessage(conversationId, content)
            } catch (e: Exception) {
                val errorMsg = "Erreur d'envoi: ${e.message}"
                _error.value = errorMsg
                Log.e("ChatViewModel", "Error sending message", e)
            }
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            try {
                apiService.markAsRead(conversationId, userId)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error marking as read", e)
            }
        }
    }

    fun refreshMessages() {
        loadMessages()
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
        Log.d("ChatViewModel", "ViewModel cleared")
    }
}