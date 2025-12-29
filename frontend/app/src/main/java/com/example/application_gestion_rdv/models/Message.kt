package com.example.application_gestion_rdv.models
import com.google.gson.annotations.SerializedName

data class Message(
    val id: Int,

    @SerializedName("conversation_id")
    val conversationId: Int,

    @SerializedName("sender_id")
    val senderId: Int,

    val content: String,

    @SerializedName("message_type")
    val messageType: String,

    @SerializedName("file_url")
    val fileUrl: String?,

    @SerializedName("is_read")
    val isRead: Boolean,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("sender_name")
    val senderName: String?
)

