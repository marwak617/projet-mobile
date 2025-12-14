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
import com.example.application_gestion_rdv.models.Appointment
import com.example.application_gestion_rdv.models.AppointmentCreate
import com.example.application_gestion_rdv.models.AppointmentCreateResponse
import com.example.application_gestion_rdv.models.AppointmentStatusResponse
import com.example.application_gestion_rdv.models.AppointmentsListResponse
import com.example.application_gestion_rdv.models.AvailabilityResponse

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

    @POST("appointments/create")
    suspend fun createAppointment(
        @Query("patient_id") patientId: Int,
        @Body appointment: AppointmentCreate
    ): Response<AppointmentCreateResponse>

    @GET("appointments/patient/{patient_id}")
    suspend fun getPatientAppointments(
        @Path("patient_id") patientId: Int,
        @Query("status") status: String? = null
    ): Response<AppointmentsListResponse>

    @GET("appointments/doctor/{doctor_id}")
    suspend fun getDoctorAppointments(
        @Path("doctor_id") doctorId: Int,
        @Query("status") status: String? = null
    ): Response<AppointmentsListResponse>

    @PUT("appointments/{appointment_id}/status")
    suspend fun updateAppointmentStatus(
        @Path("appointment_id") appointmentId: Int,
        @Query("status") status: String,
        @Query("user_id") userId: Int
    ): Response<AppointmentStatusResponse>

    @DELETE("appointments/{appointment_id}")
    suspend fun deleteAppointment(
        @Path("appointment_id") appointmentId: Int,
        @Query("user_id") userId: Int
    ): Response<AppointmentStatusResponse>

    @GET("appointments/doctor/{doctor_id}/availability")
    suspend fun getDoctorAvailability(
        @Path("doctor_id") doctorId: Int,
        @Query("date") date: String
    ): Response<AvailabilityResponse>
}