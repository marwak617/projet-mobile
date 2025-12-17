package com.example.application_gestion_rdv.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.application_gestion_rdv.R
import com.example.application_gestion_rdv.models.Appointment
import java.text.SimpleDateFormat
import java.util.*

class DoctorAppointmentsAdapter(
    private var appointments: List<Appointment>,
    private val onConfirm: (Appointment) -> Unit,
    private val onReject: (Appointment) -> Unit
) : RecyclerView.Adapter<DoctorAppointmentsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        val tvPatientEmail: TextView = itemView.findViewById(R.id.tvPatientEmail)
        val tvReason: TextView = itemView.findViewById(R.id.tvReason)
        val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)
        val layoutActions: LinearLayout = itemView.findViewById(R.id.layoutActions)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
        val btnConfirm: Button = itemView.findViewById(R.id.btnConfirm)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]

        // Afficher le statut avec emoji et couleur
        val (statusText, statusColor) = when (appointment.status) {
            "pending" -> "⏳ En attente" to "#FF9800"
            "confirmed" -> "✅ Confirmé" to "#4CAF50"
            "rejected" -> "❌ Refusé" to "#F44336"
            "cancelled" -> "🚫 Annulé" to "#9E9E9E"
            "completed" -> "✔️ Terminé" to "#607D8B"
            else -> "❓ ${appointment.status}" to "#999999"
        }

        holder.tvStatus.text = statusText
        holder.tvStatus.setTextColor(Color.parseColor(statusColor))

        // Formater la date
        holder.tvDate.text = formatDate(appointment.appointment_date)

        // Informations du patient
        holder.tvPatientName.text = appointment.patient_name
        holder.tvPatientEmail.text = appointment.patient_email ?: "Email non fourni"
        holder.tvReason.text = "Motif: ${appointment.reason}"

        // Notes (si présentes)
        if (!appointment.notes.isNullOrEmpty()) {
            holder.tvNotes.visibility = View.VISIBLE
            holder.tvNotes.text = "Notes: ${appointment.notes}"
        } else {
            holder.tvNotes.visibility = View.GONE
        }

        // Gérer la visibilité des boutons selon le statut
        when (appointment.status) {
            "pending" -> {
                holder.layoutActions.visibility = View.VISIBLE
                holder.btnConfirm.visibility = View.VISIBLE
                holder.btnReject.visibility = View.VISIBLE
            }
            else -> {
                holder.layoutActions.visibility = View.GONE
            }
        }

        // Actions
        holder.btnConfirm.setOnClickListener {
            onConfirm(appointment)
        }

        holder.btnReject.setOnClickListener {
            onReject(appointment)
        }
    }

    override fun getItemCount(): Int = appointments.size

    fun updateAppointments(newAppointments: List<Appointment>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("fr", "FR"))
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
}