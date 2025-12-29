package com.example.application_gestion_rdv.utils

import com.example.application_gestion_rdv.models.UploadResponse

sealed class UploadState {
    object Preparing : UploadState()
    data class Uploading(val progress: Int) : UploadState()
    data class Success(val response: UploadResponse) : UploadState()
    data class Error(val message: String) : UploadState()
}