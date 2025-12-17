package com.example.application_gestion_rdv.models

// Créer un rendez-vous
data class AppointmentCreate(
    val doctor_id: Int,
    val appointment_date: String,  // Format ISO: "2025-11-15T14:30:00"
    val reason: String,
    val notes: String? = null
)

// Rendez-vous

data class Appointment(
    val id: Int,
    val patient_id: Int,
    val patient_name: String,
    val patient_email: String?,  // ← AJOUTÉ
    val doctor_id: Int,
    val doctor_name: String,
    val doctor_specialty: String?,
    val appointment_date: String,
    val status: String,  // pending, confirmed, rejected, cancelled, completed
    val reason: String,
    val notes: String?,
    val created_at: String?
)

// Réponse création
data class AppointmentCreateResponse(
    val success: Boolean,
    val message: String,
    val appointment_id: Int?
)

// Réponse liste
data class AppointmentsListResponse(
    val success: Boolean,
    val count: Int,
    val appointments: List<Appointment>
)

// Réponse mise à jour statut
data class AppointmentStatusResponse(
    val success: Boolean,
    val message: String
)

// Créneaux horaires
data class TimeSlot(
    val time: String,
    val available: Boolean
)

data class AvailabilityResponse(
    val success: Boolean,
    val date: String,
    val slots: List<TimeSlot>
)