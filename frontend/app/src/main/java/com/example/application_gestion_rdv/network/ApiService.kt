package com.example.application_gestion_rdv.network
import com.example.application_gestion_rdv.models.LoginRequest
import com.example.application_gestion_rdv.models.LoginResponse
import com.example.application_gestion_rdv.models.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("users/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("users/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>
}