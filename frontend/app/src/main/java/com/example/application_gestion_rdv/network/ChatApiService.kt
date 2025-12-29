package com.example.application_gestion_rdv.network

import com.example.application_gestion_rdv.models.Message
import com.example.application_gestion_rdv.models.Conversation
import com.example.application_gestion_rdv.models.CreateConversationResponse
import com.example.application_gestion_rdv.models.StatusResponse
import com.example.application_gestion_rdv.models.UploadResponse
import com.example.application_gestion_rdv.models.DeleteResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApiService {
    @FormUrlEncoded
    @POST("chat/conversations/create")
    suspend fun createOrGetConversation(
        @Field("patient_id") patientId: Int,
        @Field("medecin_id") medecinId: Int
    ): Response<CreateConversationResponse>

    @GET("chat/conversations/{conversation_id}/messages")
    suspend fun getMessages(
        @Path("conversation_id") conversationId: Int,
        @Query("user_id") userId: Int,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<Message>>

    @POST("chat/conversations/{conversation_id}/read")
    suspend fun markAsRead(
        @Path("conversation_id") conversationId: Int,
        @Query("user_id") userId: Int
    ): Response<StatusResponse>


    // Upload de fichier
    @Multipart
    @POST("chat/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("conversation_id") conversationId: Int,
        @Part("sender_id") senderId: Int
    ): Response<UploadResponse>

    // Télécharger un fichier
    @GET("chat/download/{filename}")
    @Streaming
    suspend fun downloadFile(
        @Path("filename") filename: String
    ): Response<ResponseBody>


    // Supprimer un message avec pièce jointe
    @DELETE("chat/messages/{message_id}/attachment")
    suspend fun deleteAttachment(
        @Path("message_id") messageId: Int,
        @Query("user_id") userId: Int
    ): Response<DeleteResponse>

    //Récupérer toutes les conversations d'un utilisateur

    @GET("chat/conversations")
    suspend fun getUserConversations(
        @Query("user_id") userId: Int
    ): Response<List<Conversation>>

}

