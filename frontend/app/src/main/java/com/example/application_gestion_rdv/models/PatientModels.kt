package com.example.application_gestion_rdv.models

data class Patient(
    val id: Int,
    val name: String,
    val email: String,
    val appointmentCount: Int
)