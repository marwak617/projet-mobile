package com.example.application_gestion_rdv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityRegisterBinding
import com.example.application_gestion_rdv.models.RegisterRequest
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val region = binding.etRegion.text.toString().trim()

            if (validateInput(name, email, password)) {
                performRegister(name, email, password, region.ifEmpty { null })
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        if (name.isEmpty()) {
            binding.etName.error = "Nom requis"
            return false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email requis"
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Mot de passe requis"
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = "Minimum 6 caractères"
            return false
        }

        return true
    }

    private fun performRegister(name: String, email: String, password: String, region: String?) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.register(
                    RegisterRequest(name, email, password, region)
                )

                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@RegisterActivity, "Compte créé avec succès!", Toast.LENGTH_LONG).show()

                    // Rediriger vers LoginActivity
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        response.body()?.message ?: "Échec de l'inscription",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true

                Log.e("REGISTER", "Erreur: ${e.message}")
                Toast.makeText(this@RegisterActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}