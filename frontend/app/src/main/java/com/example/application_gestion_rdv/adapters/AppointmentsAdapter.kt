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

class AppointmentsAdapter(
    private var appointments: List<Appointment>,
    private val onCancel: (Appointment) -> Unit,
    private val onDetails: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentsAdapter.AppointmentViewHolder>() {

    class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvDoctorName: TextView = itemView.findViewById(R.id.tvDoctorName)
        val tvSpecialty: TextView = itemView.findViewById(R.id.tvSpecialty)
        val tvReason: TextView = itemView.findViewById(R.id.tvReason)
        val layoutActions: LinearLayout = itemView.findViewById(R.id.layoutActions)
        val btnCancel: Button = itemView.findViewById(R.id.btnCancel)
        val btnDetails: Button = itemView.findViewById(R.id.btnDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
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

        // Informations du médecin
        holder.tvDoctorName.text = "Dr. ${appointment.doctor_name}"
        holder.tvSpecialty.text = appointment.doctor_specialty ?: "Généraliste"
        holder.tvReason.text = "Motif: ${appointment.reason}"

        // Gérer la visibilité des boutons selon le statut
        when (appointment.status) {
            "pending", "confirmed" -> {
                holder.layoutActions.visibility = View.VISIBLE
                holder.btnCancel.visibility = View.VISIBLE
                holder.btnCancel.text = "Annuler"
            }
            "rejected", "cancelled", "completed" -> {
                holder.layoutActions.visibility = View.VISIBLE
                holder.btnCancel.visibility = View.GONE
            }
            else -> {
                holder.layoutActions.visibility = View.GONE
            }
        }

        // Actions
        holder.btnCancel.setOnClickListener {
            onCancel(appointment)
        }

        holder.btnDetails.setOnClickListener {
            onDetails(appointment)
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
            val outputFormat = SimpleDateFormat("dd MMM, HH:mm", Locale("fr", "FR"))
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
}