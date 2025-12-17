package com.example.application_gestion_rdv

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.application_gestion_rdv.adapters.DoctorAppointmentsAdapter
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityDoctorAppointmentsBinding
import com.example.application_gestion_rdv.models.Appointment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DoctorAppointmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorAppointmentsBinding
    private lateinit var adapter: DoctorAppointmentsAdapter
    private var doctorId: Int = -1
    private var allAppointments: List<Appointment> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        doctorId = intent.getIntExtra("DOCTOR_ID", -1)

        if (doctorId == -1) {
            Toast.makeText(this, "Erreur: ID médecin manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val filterStatus = intent.getStringExtra("FILTER_STATUS")

        setupUI(filterStatus)
        setupRecyclerView()
        loadAppointments(filterStatus)
    }

    private fun setupUI(initialFilter: String?) {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Titre selon le filtre initial
        binding.tvTitle.text = when (initialFilter) {
            "pending" -> "RDV en attente"
            "confirmed" -> "RDV confirmés"
            else -> "Mes Rendez-vous"
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

        binding.btnFilterToday.setOnClickListener {
            filterTodayAppointments()
        }
    }

    private fun setupRecyclerView() {
        adapter = DoctorAppointmentsAdapter(
            emptyList(),
            onConfirm = { appointment -> showConfirmDialog(appointment) },
            onReject = { appointment -> showRejectDialog(appointment) }
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
                val response = RetrofitClient.apiService.getDoctorAppointments(doctorId, status)

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
                        this@DoctorAppointmentsActivity,
                        "Erreur de chargement",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.tvEmptyMessage.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyMessage.visibility = View.VISIBLE

                Toast.makeText(
                    this@DoctorAppointmentsActivity,
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

    private fun filterTodayAppointments() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val filtered = allAppointments.filter {
            it.appointment_date.startsWith(today)
        }

        if (filtered.isEmpty()) {
            binding.recyclerViewAppointments.visibility = View.GONE
            binding.tvEmptyMessage.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = "Aucun rendez-vous aujourd'hui"
        } else {
            binding.recyclerViewAppointments.visibility = View.VISIBLE
            binding.tvEmptyMessage.visibility = View.GONE
            adapter.updateAppointments(filtered)
        }
    }

    private fun showConfirmDialog(appointment: Appointment) {
        AlertDialog.Builder(this)
            .setTitle("Confirmer le rendez-vous")
            .setMessage("Voulez-vous confirmer ce rendez-vous avec ${appointment.patient_name} ?")
            .setPositiveButton("✅ Confirmer") { _, _ ->
                updateAppointmentStatus(appointment, "confirmed")
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showRejectDialog(appointment: Appointment) {
        AlertDialog.Builder(this)
            .setTitle("Refuser le rendez-vous")
            .setMessage("Êtes-vous sûr de vouloir refuser ce rendez-vous ?")
            .setPositiveButton("❌ Refuser") { _, _ ->
                updateAppointmentStatus(appointment, "rejected")
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun updateAppointmentStatus(appointment: Appointment, newStatus: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updateAppointmentStatus(
                    appointment.id,
                    newStatus,
                    doctorId
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val message = when (newStatus) {
                        "confirmed" -> "✅ Rendez-vous confirmé"
                        "rejected" -> "❌ Rendez-vous refusé"
                        else -> "Statut mis à jour"
                    }

                    Toast.makeText(
                        this@DoctorAppointmentsActivity,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()

                    // Recharger la liste
                    loadAppointments()
                } else {
                    Toast.makeText(
                        this@DoctorAppointmentsActivity,
                        response.body()?.message ?: "Erreur de mise à jour",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@DoctorAppointmentsActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}