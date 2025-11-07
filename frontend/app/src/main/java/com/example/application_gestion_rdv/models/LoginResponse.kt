package com.example.application_gestion_rdv.models

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val user: User?
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String?,
    val region: String?
)