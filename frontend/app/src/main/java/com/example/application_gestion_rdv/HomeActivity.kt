package com.example.application_gestion_rdv

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.application_gestion_rdv.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Récupérer les données de l'utilisateur depuis l'Intent
        val userName = intent.getStringExtra("USER_NAME") ?: "Patient"
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        // Afficher le nom de l'utilisateur
        binding.tvUserName.text = userName

        // Simuler un prochain rendez-vous (pour le moment)
        // TODO: Récupérer depuis l'API
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

        // Action: Mes rendez-vous
        binding.cardMyAppointments.setOnClickListener {
            Toast.makeText(this, "Fonctionnalité à venir : Mes rendez-vous", Toast.LENGTH_SHORT).show()
            // TODO: Naviguer vers MyAppointmentsActivity
            // startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }

        // Action: Trouver un médecin
        binding.cardFindDoctor.setOnClickListener {
            Toast.makeText(this, "Fonctionnalité à venir : Rechercher un médecin", Toast.LENGTH_SHORT).show()
            // TODO: Naviguer vers FindDoctorActivity
            // startActivity(Intent(this, FindDoctorActivity::class.java))
        }

        // Action: Mon profil
        binding.cardMyProfile.setOnClickListener {
            Toast.makeText(this, "Fonctionnalité à venir : Mon profil", Toast.LENGTH_SHORT).show()
            // TODO: Naviguer vers ProfileActivity
            // startActivity(Intent(this, ProfileActivity::class.java))
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

    // Empêcher le retour arrière vers Login après connexion
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Quitter l'application")
            .setMessage("Voulez-vous vraiment quitter ?")
            .setPositiveButton("Oui") { _, _ ->
                finishAffinity() // Fermer complètement l'app
            }
            .setNegativeButton("Non", null)
            .show()
    }
}