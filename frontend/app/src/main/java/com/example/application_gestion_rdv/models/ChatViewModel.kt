package com.example.application_gestion_rdv.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.application_gestion_rdv.WebSocketManager
import com.example.application_gestion_rdv.network.ChatApiService

class ChatViewModel(
    private val userId: Int,
    private val conversationId: Int,
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
        // CHANGEZ cette URL avec votre IP
        webSocketManager.connect("ws://192.168.1.99:8000")
    }

    private fun observeNewMessages() {
        viewModelScope.launch {
            webSocketManager.messages.collect { newMessage ->
                newMessage?.let {
                    if (it.conversationId == conversationId) {
                        _messages.value = _messages.value + it

                        // Marquer comme lu si ce n'est pas notre message
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
                //Appel API avec gestion de réponse
                val response = apiService.getMessages(conversationId, userId)

                if (response.isSuccessful && response.body() != null) {
                    _messages.value = response.body()!!.reversed() // Ordre chronologique
                } else {
                    _error.value = "Erreur de chargement: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Erreur: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                webSocketManager.sendMessage(conversationId, content)
            } catch (e: Exception) {
                _error.value = "Erreur d'envoi: ${e.message}"
            }
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            try {
                //Appel API
                val response = apiService.markAsRead(conversationId, userId)

                if (!response.isSuccessful) {
                    println("Erreur markAsRead: ${response.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshMessages() {
        loadMessages()
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}