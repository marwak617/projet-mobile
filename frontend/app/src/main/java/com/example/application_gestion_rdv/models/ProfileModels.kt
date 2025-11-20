package com.example.application_gestion_rdv.models

// Requête de mise à jour du profil
data class UpdateProfileRequest(
    val name: String,
    val phone: String?,
    val region: String?,
    val address: String?
)

// Réponse du profil
data class ProfileResponse(
    val success: Boolean,
    val message: String? = null,
    val user: UserProfile?
)

// Profil utilisateur complet
data class UserProfile(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val region: String?,
    val address: String?,
    val role: String?
)

// Requête de changement de mot de passe
data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String
)

// Réponse changement de mot de passe
data class ChangePasswordResponse(
    val success: Boolean,
    val message: String
)