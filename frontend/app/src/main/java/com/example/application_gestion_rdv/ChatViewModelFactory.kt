package com.example.application_gestion_rdv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.application_gestion_rdv.network.ChatApiService

class ChatViewModelFactory(
    private val userId: Int,
    private val conversationId: Int,
    private val apiService: ChatApiService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(userId, conversationId, apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}