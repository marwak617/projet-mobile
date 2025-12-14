package com.example.application_gestion_rdv

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.application_gestion_rdv.adapters.AppointmentsAdapter
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityMyAppointmentsBinding
import com.example.application_gestion_rdv.models.Appointment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MyAppointmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyAppointmentsBinding
    private lateinit var adapter: AppointmentsAdapter
    private var userId: Int = -1
    private var allAppointments: List<Appointment> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)

        if (userId == -1) {
            Toast.makeText(this, "Erreur: ID utilisateur manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        loadAppointments()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Filtres
        binding.btnFilterAll.setOnClickListener {
            filterAppointments(null)
        }

        binding.btnFilterPending.setOnClickListener {
            filterAppointments("pending")
        }

        binding.btnFilterConfirmed.setOnClickListener {
            filterAppointments("confirmed")
        }

        binding.btnFilterCompleted.setOnClickListener {
            filterAppointments("completed")
        }

        binding.btnFilterCancelled.setOnClickListener {
            filterAppointments("cancelled")
        }
    }

    private fun setupRecyclerView() {
        adapter = AppointmentsAdapter(
            emptyList(),
            onCancel = { appointment -> showCancelDialog(appointment) },
            onDetails = { appointment -> showDetailsDialog(appointment) }
        )

        binding.recyclerViewAppointments.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAppointments.adapter = adapter
    }

    private fun loadAppointments(status: String? = null) {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewAppointments.visibility = View.GONE
        binding.tvEmptyMessage.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPatientAppointments(userId, status)

                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body()?.success == true) {
                    allAppointments = response.body()?.appointments ?: emptyList()

                    if (allAppointments.isEmpty()) {
                        binding.tvEmptyMessage.visibility = View.VISIBLE
                    } else {
                        binding.recyclerViewAppointments.visibility = View.VISIBLE
                        adapter.updateAppointments(allAppointments)
                    }
                } else {
                    Toast.makeText(
                        this@MyAppointmentsActivity,
                        "Erreur de chargement",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.tvEmptyMessage.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyMessage.visibility = View.VISIBLE

                Toast.makeText(
                    this@MyAppointmentsActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun filterAppointments(status: String?) {
        val filtered = if (status == null) {
            allAppointments
        } else {
            allAppointments.filter { it.status == status }
        }

        if (filtered.isEmpty()) {
            binding.recyclerViewAppointments.visibility = View.GONE
            binding.tvEmptyMessage.visibility = View.VISIBLE
        } else {
            binding.recyclerViewAppointments.visibility = View.VISIBLE
            binding.tvEmptyMessage.visibility = View.GONE
            adapter.updateAppointments(filtered)
        }
    }

    private fun showCancelDialog(appointment: Appointment) {
        AlertDialog.Builder(this)
            .setTitle("Annuler le rendez-vous")
            .setMessage("Êtes-vous sûr de vouloir annuler ce rendez-vous avec Dr. ${appointment.doctor_name} ?")
            .setPositiveButton("Oui, annuler") { _, _ ->
                cancelAppointment(appointment)
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun cancelAppointment(appointment: Appointment) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updateAppointmentStatus(
                    appointment.id,
                    "cancelled",
                    userId
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@MyAppointmentsActivity,
                        "Rendez-vous annulé",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadAppointments()
                } else {
                    Toast.makeText(
                        this@MyAppointmentsActivity,
                        response.body()?.message ?: "Erreur d'annulation",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@MyAppointmentsActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDetailsDialog(appointment: Appointment) {
        val message = buildString {
            append("📅 Date: ${formatDate(appointment.appointment_date)}\n\n")
            append("👨‍⚕️ Médecin: Dr. ${appointment.doctor_name}\n")
            append("🏥 Spécialité: ${appointment.doctor_specialty}\n\n")
            append("📋 Motif: ${appointment.reason}\n")
            if (!appointment.notes.isNullOrEmpty()) {
                append("\n📝 Notes: ${appointment.notes}")
            }
            append("\n\n📊 Statut: ${getStatusText(appointment.status)}")
        }

        AlertDialog.Builder(this)
            .setTitle("Détails du rendez-vous")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE dd MMMM yyyy 'à' HH:mm", Locale("fr", "FR"))
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "pending" -> "⏳ En attente de confirmation"
            "confirmed" -> "✅ Confirmé"
            "rejected" -> "❌ Refusé par le médecin"
            "cancelled" -> "🚫 Annulé"
            "completed" -> "✔️ Terminé"
            else -> status
        }
    }
}