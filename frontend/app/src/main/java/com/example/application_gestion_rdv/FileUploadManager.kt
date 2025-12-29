package com.example.application_gestion_rdv

import android.content.Context
import android.net.Uri
import com.example.application_gestion_rdv.network.ChatApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import com.example.application_gestion_rdv.utils.UploadState

class FileUploadManager(
    private val context: Context,
    private val apiService: ChatApiService
) {

    private val filePickerHelper = FilePickerHelper(context)

    suspend fun uploadFile(
        uri: Uri,
        conversationId: Int,
        senderId: Int
    ): Flow<UploadState> = flow {
        try {
            emit(UploadState.Preparing)

            // Vérifier la taille
            val sizeInMB = filePickerHelper.getFileSizeInMB(uri)
            if (sizeInMB > 10) {
                emit(UploadState.Error("Fichier trop volumineux (max 10MB)"))
                return@flow
            }

            // Convertir URI en File
            val file = withContext(Dispatchers.IO) {
                filePickerHelper.getFileFromUri(uri)
            }

            if (file == null) {
                emit(UploadState.Error("Impossible de lire le fichier"))
                return@flow
            }

            emit(UploadState.Uploading(0))

            // Préparer la requête multipart
            val mimeType = filePickerHelper.getMimeType(uri) ?: "application/octet-stream"
            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestBody
            )

            // Upload
            val response = withContext(Dispatchers.IO) {
                apiService.uploadFile(multipartBody, conversationId, senderId)
            }

            // Nettoyer le fichier temporaire
            file.delete()

            if (response.isSuccessful && response.body() != null) {
                emit(UploadState.Success(response.body()!!))
            } else {
                emit(UploadState.Error("Erreur upload: ${response.code()}"))
            }

        } catch (e: Exception) {
            emit(UploadState.Error("Erreur: ${e.message}"))
        }
    }

    suspend fun downloadFile(filename: String): File? {
        return try {
            withContext(Dispatchers.IO) {
                val response = apiService.downloadFile(filename)
                if (response.isSuccessful && response.body() != null) {
                    val file = File(context.cacheDir, filename)
                    file.writeBytes(response.body()!!.bytes())
                    file
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}