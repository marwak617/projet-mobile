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

        Log.d("REGISTER", "📱 RegisterActivity créée")
        Log.d("REGISTER", "🌐 BASE_URL: ${RetrofitClient.BASE_URL}")

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val region = binding.etRegion.text.toString().trim()

            Log.d("REGISTER", "🔘 Bouton cliqué - Name: $name, Email: $email, Region: $region")

            if (validateInput(name, email, password)) {
                performRegister(name, email, password, region.ifEmpty { null })
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            Log.d("REGISTER", "↩️ Retour vers LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        if (name.isEmpty()) {
            binding.etName.error = "Nom requis"
            Log.d("REGISTER", "❌ Validation: nom vide")
            return false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email requis"
            Log.d("REGISTER", "❌ Validation: email vide")
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Mot de passe requis"
            Log.d("REGISTER", "❌ Validation: password vide")
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = "Minimum 6 caractères"
            Log.d("REGISTER", "❌ Validation: password trop court (${password.length} caractères)")
            return false
        }

        Log.d("REGISTER", "✅ Validation OK")
        return true
    }

    private fun performRegister(name: String, email: String, password: String, region: String?) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        Log.d("REGISTER", "🚀 Début de performRegister")
        Log.d("REGISTER", "📤 Name: $name")
        Log.d("REGISTER", "📤 Email: $email")
        Log.d("REGISTER", "📤 Region: $region")
        Log.d("REGISTER", "📤 URL complète: ${RetrofitClient.BASE_URL}users/register")

        lifecycleScope.launch {
            try {
                Log.d("REGISTER", "⏳ Envoi de la requête...")

                val response = RetrofitClient.apiService.register(
                    RegisterRequest(name, email, password, region)
                )

                Log.d("REGISTER", "📥 Réponse reçue!")
                Log.d("REGISTER", "📊 HTTP Code: ${response.code()}")
                Log.d("REGISTER", "📊 isSuccessful: ${response.isSuccessful}")

                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true

                if (response.isSuccessful) {
                    val body = response.body()

                    // Logs détaillés du body
                    Log.d("REGISTER", "📦 Body reçu: $body")
                    Log.d("REGISTER", "📦 success: ${body?.success}")
                    Log.d("REGISTER", "📦 message: ${body?.message}")
                    Log.d("REGISTER", "📦 token: ${body?.token?.take(20)}...")
                    Log.d("REGISTER", "📦 user: ${body?.user}")

                    if (body?.success == true) {
                        Log.d("REGISTER", "✅ SUCCESS = TRUE")
                        val user = body.user
                        Log.d("REGISTER", "👤 User name: ${user?.name}")
                        Log.d("REGISTER", "👤 User email: ${user?.email}")
                        Log.d("REGISTER", "👤 User role: ${user?.role}")

                        Toast.makeText(
                            this@RegisterActivity,
                            "Compte créé avec succès! Bienvenue ${user?.name}!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Rediriger vers LoginActivity
                        Log.d("REGISTER", "🔄 Redirection vers LoginActivity")
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()

                    } else {
                        Log.e("REGISTER", "❌ SUCCESS = FALSE")
                        Log.e("REGISTER", "❌ Message d'erreur: ${body?.message}")

                        Toast.makeText(
                            this@RegisterActivity,
                            body?.message ?: "Échec de l'inscription",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("REGISTER", "❌ HTTP Error")
                    Log.e("REGISTER", "❌ Code: ${response.code()}")
                    Log.e("REGISTER", "❌ Message: ${response.message()}")
                    Log.e("REGISTER", "❌ Error Body: $errorBody")

                    Toast.makeText(
                        this@RegisterActivity,
                        "Erreur serveur: ${response.code()} - ${errorBody ?: response.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true

                Log.e("REGISTER", "💥 EXCEPTION attrapée!")
                Log.e("REGISTER", "💥 Type: ${e.javaClass.simpleName}")
                Log.e("REGISTER", "💥 Message: ${e.message}")
                Log.e("REGISTER", "💥 Cause: ${e.cause}")
                e.printStackTrace()

                Toast.makeText(
                    this@RegisterActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}