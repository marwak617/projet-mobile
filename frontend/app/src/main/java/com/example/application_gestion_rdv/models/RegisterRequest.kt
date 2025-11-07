package com.example.application_gestion_rdv.models

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val region: String? = null
)
