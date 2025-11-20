package com.example.application_gestion_rdv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.application_gestion_rdv.R
import com.example.application_gestion_rdv.models.Doctor

class DoctorsAdapter(
    private var doctors: List<Doctor>,
    private val onBookAppointment: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorsAdapter.DoctorViewHolder>() {

    class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDoctorName: TextView = itemView.findViewById(R.id.tvDoctorName)
        val tvSpecialty: TextView = itemView.findViewById(R.id.tvSpecialty)
        val tvRegion: TextView = itemView.findViewById(R.id.tvRegion)
        val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        val btnBookAppointment: Button = itemView.findViewById(R.id.btnBookAppointment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doctor = doctors[position]

        holder.tvDoctorName.text = "Dr. ${doctor.name}"
        holder.tvSpecialty.text = doctor.specialty ?: "Généraliste"

        // Afficher la région si disponible
        if (doctor.region.isNullOrEmpty()) {
            holder.tvRegion.visibility = View.GONE
        } else {
            holder.tvRegion.visibility = View.VISIBLE
            holder.tvRegion.text = "📍 ${doctor.region}"
        }

        // Afficher le téléphone si disponible
        if (doctor.phone.isNullOrEmpty()) {
            holder.tvPhone.visibility = View.GONE
        } else {
            holder.tvPhone.visibility = View.VISIBLE
            holder.tvPhone.text = "📞 ${doctor.phone}"
        }

        // Action du bouton RDV
        holder.btnBookAppointment.setOnClickListener {
            onBookAppointment(doctor)
        }

        // Click sur toute la carte
        holder.itemView.setOnClickListener {
            onBookAppointment(doctor)
        }
    }

    override fun getItemCount(): Int = doctors.size

    // Mettre à jour la liste
    fun updateDoctors(newDoctors: List<Doctor>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }

    // Filtrer par recherche
    fun filter(query: String, allDoctors: List<Doctor>) {
        val filteredList = if (query.isEmpty()) {
            allDoctors
        } else {
            allDoctors.filter { doctor ->
                doctor.name.contains(query, ignoreCase = true) ||
                        doctor.specialty?.contains(query, ignoreCase = true) == true ||
                        doctor.region?.contains(query, ignoreCase = true) == true
            }
        }
        updateDoctors(filteredList)
    }
}