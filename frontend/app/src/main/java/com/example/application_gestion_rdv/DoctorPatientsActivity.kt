package com.example.application_gestion_rdv

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.application_gestion_rdv.adapters.PatientsAdapter
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityDoctorPatientsBinding
import com.example.application_gestion_rdv.models.Patient
import kotlinx.coroutines.launch

class DoctorPatientsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorPatientsBinding
    private lateinit var adapter: PatientsAdapter
    private var doctorId: Int = -1
    private var allPatients: List<Patient> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorPatientsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        doctorId = intent.getIntExtra("DOCTOR_ID", -1)

        if (doctorId == -1) {
            Toast.makeText(this, "Erreur: ID médecin manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        setupSearch()
        loadPatients()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = PatientsAdapter(emptyList()) { patient ->
            showPatientDetails(patient)
        }

        binding.recyclerViewPatients.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPatients.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                adapter.filter(query, allPatients)
            }
        })
    }

    private fun loadPatients() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewPatients.visibility = View.GONE
        binding.tvEmptyMessage.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Récupérer tous les RDV du médecin
                val response = RetrofitClient.apiService.getDoctorAppointments(doctorId)

                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body()?.success == true) {
                    val appointments = response.body()?.appointments ?: emptyList()

                    // Grouper par patient et compter les RDV
                    val patientsMap = appointments
                        .groupBy { it.patient_id }
                        .map { (patientId, patientAppointments) ->
                            val firstAppointment = patientAppointments.first()
                            Patient(
                                id = patientId,
                                name = firstAppointment.patient_name,
                                email = firstAppointment.patient_email ?: "Email non fourni",
                                appointmentCount = patientAppointments.size
                            )
                        }
                        .sortedByDescending { it.appointmentCount }

                    allPatients = patientsMap

                    if (allPatients.isEmpty()) {
                        binding.tvEmptyMessage.visibility = View.VISIBLE
                    } else {
                        binding.recyclerViewPatients.visibility = View.VISIBLE
                        adapter.updatePatients(allPatients)
                    }
                } else {
                    Toast.makeText(
                        this@DoctorPatientsActivity,
                        "Erreur de chargement",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.tvEmptyMessage.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyMessage.visibility = View.VISIBLE

                Toast.makeText(
                    this@DoctorPatientsActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showPatientDetails(patient: Patient) {
        val message = buildString {
            append("👤 Nom: ${patient.name}\n\n")
            append("📧 Email: ${patient.email}\n\n")
            append("📅 Nombre de consultations: ${patient.appointmentCount}\n\n")

            if (patient.appointmentCount >= 5) {
                append("🏆 Patient fidèle")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Informations patient")
            .setMessage(message)
            .setPositiveButton("Voir historique") { _, _ ->
                // TODO: Implémenter historique patient
                Toast.makeText(
                    this,
                    "Fonctionnalité à venir: Historique de ${patient.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Fermer", null)
            .show()
    }
}