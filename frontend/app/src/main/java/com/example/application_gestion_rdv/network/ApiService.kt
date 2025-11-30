package com.example.application_gestion_rdv.network

import com.example.application_gestion_rdv.models.DoctorsResponse
import com.example.application_gestion_rdv.models.LoginRequest
import com.example.application_gestion_rdv.models.LoginResponse
import com.example.application_gestion_rdv.models.RegisterRequest
import com.example.application_gestion_rdv.models.ProfileResponse
import com.example.application_gestion_rdv.models.UpdateProfileRequest
import com.example.application_gestion_rdv.models.ChangePasswordRequest
import com.example.application_gestion_rdv.models.ChangePasswordResponse
import com.example.application_gestion_rdv.models.DocumentsListResponse
import com.example.application_gestion_rdv.models.DeleteDocumentResponse
import com.example.application_gestion_rdv.models.UploadDocumentResponse

import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.Part

interface ApiService {
    @POST("users/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("users/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @GET("/doctors")
    suspend fun getDoctors(@Query("specialty") specialty: String? = null): Response<DoctorsResponse>

    @GET("users/profile/{user_id}")
    suspend fun getProfile(@Path("user_id") userId: Int): Response<ProfileResponse>

    @PUT("users/profile/{user_id}")
    suspend fun updateProfile(
        @Path("user_id") userId: Int,
        @Body request: UpdateProfileRequest
    ): Response<ProfileResponse>

    @PUT("users/change-password/{user_id}")
    suspend fun changePassword(
        @Path("user_id") userId: Int,
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>
    @Multipart
    @POST("users/upload-document/{user_id}")
    suspend fun uploadDocument(
        @Path("user_id") userId: Int,
        @Part file: MultipartBody.Part,
        @Part("document_type") documentType: RequestBody
    ): Response<UploadDocumentResponse>

    @GET("users/documents/{user_id}")
    suspend fun getUserDocuments(@Path("user_id") userId: Int): Response<DocumentsListResponse>

    @DELETE("users/document/{user_id}/{filename}")
    suspend fun deleteDocument(
        @Path("user_id") userId: Int,
        @Path("filename") filename: String
    ): Response<DeleteDocumentResponse>
}