package com.example.application_gestion_rdv.models
import com.google.gson.annotations.SerializedName
data class Conversation(
    val id: Int,

    @SerializedName("patient_id")
    val patientId: Int,

    @SerializedName("medecin_id")
    val medecinId: Int,

    @SerializedName("patient_name")
    val patientName: String,

    @SerializedName("medecin_name")
    val medecinName: String,

    @SerializedName("last_message")
    val lastMessage: String?,

    @SerializedName("last_message_at")
    val lastMessageAt: String?,

    @SerializedName("unread_count")
    val unreadCount: Int
) {
    // Propriété calculée pour l'affichage
    val doctorName: String
        get() = medecinName


    val doctorId: Int
        get() = medecinId

    val lastMessageTime: String?
        get() = lastMessageAt
}
data class UploadResponse(
    val success: Boolean,
    val message: Message,

    @SerializedName("file_info")
    val fileInfo: FileInfo
)

data class FileInfo(
    @SerializedName("filename")
    val filename: String,

    @SerializedName("original_name")
    val originalName: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("size")
    val size: Int
)

data class StatusResponse(
    @SerializedName("status")
    val status: String
)


data class DeleteResponse(
    @SerializedName("success")
    val success: Boolean
)


// Model de réponse
data class CreateConversationResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("conversation_id")
    val conversation_id: Int,

    @SerializedName("message")
    val message: String
)