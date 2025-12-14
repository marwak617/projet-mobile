package com.example.application_gestion_rdv

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.application_gestion_rdv.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)

        setupUI()
        setupClickListeners()
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })
    }

    private fun setupUI() {
        // Récupérer les données de l'utilisateur depuis l'Intent
        val userName = intent.getStringExtra("USER_NAME") ?: "Patient"
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        // Afficher le nom de l'utilisateur
        binding.tvUserName.text = userName

        // Simuler un prochain rendez-vous (pour le moment)
        showNextAppointment(hasAppointment = false)
    }

    private fun showNextAppointment(hasAppointment: Boolean) {
        if (hasAppointment) {
            binding.cardNextAppointment.visibility = android.view.View.VISIBLE
            // Les données seront remplies depuis l'API
        } else {
            binding.cardNextAppointment.visibility = android.view.View.GONE
        }
    }

    private fun setupClickListeners() {
        // Action: Prendre un RDV
        binding.cardBookAppointment.setOnClickListener {
            Toast.makeText(this, "Fonctionnalité à venir : Prendre un RDV", Toast.LENGTH_SHORT).show()
            // TODO: Naviguer vers BookAppointmentActivity
            // startActivity(Intent(this, BookAppointmentActivity::class.java))
        }

        // Action: Mon profil
        binding.cardMyProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        // Action: Voir détails du prochain RDV
        binding.btnViewDetails.setOnClickListener {
            Toast.makeText(this, "Détails du rendez-vous", Toast.LENGTH_SHORT).show()
            // TODO: Afficher les détails
        }

        // Action: Déconnexion
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Action: Photo de profil
        binding.ivProfile.setOnClickListener {
            Toast.makeText(this, "Fonctionnalité à venir : Modifier la photo", Toast.LENGTH_SHORT).show()
        }

        // Action: Prendre un RDV (via liste des médecins)
        binding.cardBookAppointment.setOnClickListener {
            val intent = Intent(this, DoctorsListActivity::class.java)
            intent.putExtra("PATIENT_ID", userId)
            startActivity(intent)
        }

        // Action: Mes rendez-vous
        binding.cardMyAppointments.setOnClickListener {
            val intent = Intent(this, MyAppointmentsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        // Action: Trouver un médecin
        binding.cardFindDoctor.setOnClickListener {
            val intent = Intent(this, DoctorsListActivity::class.java)
            intent.putExtra("PATIENT_ID", userId)
            startActivity(intent)
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Quitter l'application")
            .setMessage("Voulez-vous vraiment quitter ?")
            .setPositiveButton("Oui") { _, _ ->
                finishAffinity() // Fermer complètement l'app
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
        // TODO: Supprimer le token stocké, nettoyer les préférences

        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show()

        // Retourner à l'écran de login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}