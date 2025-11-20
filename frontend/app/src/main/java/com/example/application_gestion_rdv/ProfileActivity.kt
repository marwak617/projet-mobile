package com.example.application_gestion_rdv

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityProfileBinding
import com.example.application_gestion_rdv.models.ChangePasswordRequest
import com.example.application_gestion_rdv.models.UpdateProfileRequest
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)

        if (userId == -1) {
            Toast.makeText(this, "Erreur: ID utilisateur manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        loadProfile()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun loadProfile() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getProfile(userId)

                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body()?.success == true) {
                    val user = response.body()?.user

                    binding.tvUserEmail.text = user?.email
                    binding.etName.setText(user?.name)
                    binding.etPhone.setText(user?.phone ?: "")
                    binding.etRegion.setText(user?.region ?: "")
                    binding.etAddress.setText(user?.address ?: "")

                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Erreur de chargement du profil",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@ProfileActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val region = binding.etRegion.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (name.isEmpty()) {
            binding.etName.error = "Le nom est requis"
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSaveProfile.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = UpdateProfileRequest(
                    name = name,
                    phone = phone.ifEmpty { null },
                    region = region.ifEmpty { null },
                    address = address.ifEmpty { null }
                )

                val response = RetrofitClient.apiService.updateProfile(userId, request)

                binding.progressBar.visibility = View.GONE
                binding.btnSaveProfile.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Profil mis à jour avec succès!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        response.body()?.message ?: "Erreur de mise à jour",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnSaveProfile.isEnabled = true

                Toast.makeText(
                    this@ProfileActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etCurrentPassword = dialogView.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(this)
            .setTitle("Changer le mot de passe")
            .setView(dialogView)
            .setPositiveButton("Changer") { _, _ ->
                val currentPassword = etCurrentPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun validatePasswordChange(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "Mot de passe actuel requis", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Nouveau mot de passe requis", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                val request = ChangePasswordRequest(currentPassword, newPassword)
                val response = RetrofitClient.apiService.changePassword(userId, request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Mot de passe modifié avec succès!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        response.body()?.message ?: "Erreur de modification",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}