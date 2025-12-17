package com.example.application_gestion_rdv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.application_gestion_rdv.R
import com.example.application_gestion_rdv.models.Patient

class PatientsAdapter(
    private var patients: List<Patient>,
    private val onClick: (Patient) -> Unit
) : RecyclerView.Adapter<PatientsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        val tvPatientEmail: TextView = itemView.findViewById(R.id.tvPatientEmail)
        val tvAppointmentCount: TextView = itemView.findViewById(R.id.tvAppointmentCount)
        val tvBadge: TextView = itemView.findViewById(R.id.tvBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patient = patients[position]

        holder.tvPatientName.text = patient.name
        holder.tvPatientEmail.text = patient.email

        val consultationText = if (patient.appointmentCount == 1) {
            "1 consultation"
        } else {
            "${patient.appointmentCount} consultations"
        }
        holder.tvAppointmentCount.text = consultationText

        // Badge pour patients fidèles (5+ consultations)
        if (patient.appointmentCount >= 5) {
            holder.tvBadge.visibility = View.VISIBLE
        } else {
            holder.tvBadge.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onClick(patient)
        }
    }

    override fun getItemCount(): Int = patients.size

    fun updatePatients(newPatients: List<Patient>) {
        patients = newPatients
        notifyDataSetChanged()
    }

    fun filter(query: String, allPatients: List<Patient>) {
        val filteredList = if (query.isEmpty()) {
            allPatients
        } else {
            allPatients.filter { patient ->
                patient.name.contains(query, ignoreCase = true) ||
                        patient.email.contains(query, ignoreCase = true)
            }
        }
        updatePatients(filteredList)
    }
}