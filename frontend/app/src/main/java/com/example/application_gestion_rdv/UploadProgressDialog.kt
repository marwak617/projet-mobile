package com.example.application_gestion_rdv

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.application_gestion_rdv.utils.UploadState

class UploadProgressDialog(context: Context) {

    private val dialog: AlertDialog
    private val progressBar: ProgressBar
    private val statusText: TextView

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_upload_progress, null)
        progressBar = view.findViewById(R.id.progressBar)
        statusText = view.findViewById(R.id.statusText)

        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    fun setStatus(status: String) {
        statusText.text = status
    }

    fun setProgress(progress: Int) {
        progressBar.progress = progress
    }

    fun updateState(state: UploadState) {
        when (state) {
            is UploadState.Preparing -> {
                setStatus("Préparation...")
                progressBar.isIndeterminate = true
            }
            is UploadState.Uploading -> {
                setStatus("Envoi en cours...")
                progressBar.isIndeterminate = false
                setProgress(state.progress)
            }
            else -> {}
        }
    }
}