package com.example.application_gestion_rdv.models

data class Doctor(
    val id: Int,
    val name: String,
    val email: String,
    val specialty: String?,
    val region: String?,
    val phone: String?,
    val address: String?
)

data class DoctorsResponse(
    val success: Boolean,
    val count: Int,
    val doctors: List<Doctor>
)