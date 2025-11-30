package com.example.application_gestion_rdv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.application_gestion_rdv.R
import com.example.application_gestion_rdv.models.MedicalDocument
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class DocumentsAdapter(
    private var documents: List<MedicalDocument>,
    private val onDelete: (MedicalDocument) -> Unit
) : RecyclerView.Adapter<DocumentsAdapter.DocumentViewHolder>() {

    class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFileIcon: TextView = itemView.findViewById(R.id.tvFileIcon)
        val tvDocumentType: TextView = itemView.findViewById(R.id.tvDocumentType)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvFileSize: TextView = itemView.findViewById(R.id.tvFileSize)
        val tvUploadDate: TextView = itemView.findViewById(R.id.tvUploadDate)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documents[position]

        // Icône selon le type de fichier
        holder.tvFileIcon.text = when {
            document.filename.endsWith(".pdf") -> "📄"
            document.filename.endsWith(".jpg") || document.filename.endsWith(".jpeg") ||
                    document.filename.endsWith(".png") -> "🖼️"
            document.filename.endsWith(".doc") || document.filename.endsWith(".docx") -> "📝"
            else -> "📎"
        }

        // Type de document
        holder.tvDocumentType.text = when (document.file_type) {
            "mutuelle" -> "Mutuelle"
            "ordonnance" -> "Ordonnance"
            "analyse" -> "Analyse médicale"
            "radio" -> "Radiographie"
            "autre" -> "Autre"
            else -> document.file_type.capitalize(Locale.getDefault())
        }

        // Nom du fichier
        holder.tvFileName.text = document.original_filename ?: document.filename

        // Taille du fichier
        holder.tvFileSize.text = formatFileSize(document.file_size)

        // Date d'upload
        holder.tvUploadDate.text = formatDate(document.upload_date)

        // Action supprimer
        holder.btnDelete.setOnClickListener {
            onDelete(document)
        }
    }

    override fun getItemCount(): Int = documents.size

    fun updateDocuments(newDocuments: List<MedicalDocument>) {
        documents = newDocuments
        notifyDataSetChanged()
    }

    private fun formatFileSize(bytes: Int): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${(bytes / 1024.0).roundToInt()} KB"
            else -> "${(bytes / (1024.0 * 1024.0)).roundToInt()} MB"
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString.substring(0, 10) // Fallback
        }
    }
}