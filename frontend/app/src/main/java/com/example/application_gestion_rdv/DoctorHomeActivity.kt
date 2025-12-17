package com.example.application_gestion_rdv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityDoctorHomeBinding
import kotlinx.coroutines.launch

class DoctorHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorHomeBinding
    private var doctorId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        doctorId = intent.getIntExtra("USER_ID", -1)

        if (doctorId == -1) {
            Toast.makeText(this, "Erreur: ID médecin manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupClickListeners()
        setupBackPressHandler()
        loadStatistics()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })
    }

    private fun setupUI() {
        val doctorName = intent.getStringExtra("USER_NAME") ?: "Médecin"
        val specialty = intent.getStringExtra("USER_SPECIALTY") ?: ""

        binding.tvDoctorName.text = "Dr. $doctorName"
        binding.tvSpecialty.text = specialty
    }

    private fun setupClickListeners() {
        // RDV en attente
        binding.cardPendingAppointments.setOnClickListener {
            val intent = Intent(this, DoctorAppointmentsActivity::class.java)
            intent.putExtra("DOCTOR_ID", doctorId)
            intent.putExtra("FILTER_STATUS", "pending")
            startActivity(intent)
        }

        // Tous les RDV
        binding.cardAllAppointments.setOnClickListener {
            val intent = Intent(this, DoctorAppointmentsActivity::class.java)
            intent.putExtra("DOCTOR_ID", doctorId)
            startActivity(intent)
        }

        // Mes patients
        binding.cardMyPatients.setOnClickListener {
            val intent = Intent(this, DoctorPatientsActivity::class.java)
            intent.putExtra("DOCTOR_ID", doctorId)
            startActivity(intent)
        }

        // Mon profil
        binding.cardMyProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("USER_ID", doctorId)
            startActivity(intent)
        }

        // Photo de profil
        binding.ivProfile.setOnClickListener {
            Toast.makeText(this, "Fonctionnalité à venir", Toast.LENGTH_SHORT).show()
        }

        // Déconnexion
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                // Charger tous les RDV
                val response = RetrofitClient.apiService.getDoctorAppointments(doctorId)

                if (response.isSuccessful && response.body()?.success == true) {
                    val appointments = response.body()?.appointments ?: emptyList()

                    // Compter les patients uniques
                    val uniquePatients = appointments.map { it.patient_id }.distinct().size
                    binding.tvTotalPatients.text = uniquePatients.toString()

                    // Compter les RDV en attente
                    val pendingCount = appointments.count { it.status == "pending" }
                    binding.tvPendingCount.text = pendingCount.toString()

                    // Afficher info RDV aujourd'hui
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date())

                    val todayAppointments = appointments.filter {
                        it.appointment_date.startsWith(today) &&
                                it.status == "confirmed"
                    }

                    if (todayAppointments.isNotEmpty()) {
                        binding.cardTodayAppointments.visibility = View.VISIBLE
                        binding.tvTodayInfo.text = "${todayAppointments.size} rendez-vous confirmé(s) aujourd'hui"
                    }
                }

            } catch (e: Exception) {
                // Erreur silencieuse, l'utilisateur peut continuer
            }
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Quitter l'application")
            .setMessage("Voulez-vous vraiment quitter ?")
            .setPositiveButton("Oui") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Déconnexion")
            .setMessage("Voulez-vous vraiment vous déconnecter ?")
            .setPositiveButton("Oui") { _, _ ->
                logout()
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun logout() {
        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Recharger les statistiques quand on revient sur l'écran
        loadStatistics()
    }
}