package com.example.application_gestion_rdv.models

// Document médical
data class MedicalDocument(
    val filename: String,
    val original_filename: String? = null,
    val file_type: String,
    val upload_date: String,
    val file_size: Int
)

// Réponse upload
data class UploadDocumentResponse(
    val success: Boolean,
    val message: String,
    val document: MedicalDocument?
)

// Réponse liste documents
data class DocumentsListResponse(
    val success: Boolean,
    val count: Int,
    val documents: List<MedicalDocument>
)

// Réponse suppression
data class DeleteDocumentResponse(
    val success: Boolean,
    val message: String
)