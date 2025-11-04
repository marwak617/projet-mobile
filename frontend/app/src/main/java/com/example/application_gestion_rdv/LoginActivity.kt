package com.example.application_gestion_rdv

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityLoginBinding
import com.example.application_gestion_rdv.models.LoginRequest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("LOGIN", "📱 LoginActivity créée")
        Log.d("LOGIN", "🌐 BASE_URL: ${RetrofitClient.BASE_URL}")

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            Log.d("LOGIN", "🔘 Bouton cliqué - Email: $email")

            if (validateInput(email, password)) {
                performLogin(email, password)
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email requis"
            Log.d("LOGIN", "❌ Validation: email vide")
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Mot de passe requis"
            Log.d("LOGIN", "❌ Validation: password vide")
            return false
        }

        Log.d("LOGIN", "✅ Validation OK")
        return true
    }

    private fun performLogin(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        Log.d("LOGIN", "🚀 Début de performLogin")
        Log.d("LOGIN", "📤 Email: $email")
        Log.d("LOGIN", "📤 URL complète: ${RetrofitClient.BASE_URL}users/login")

        lifecycleScope.launch {
            try {
                Log.d("LOGIN", "⏳ Envoi de la requête...")

                val response = RetrofitClient.apiService.login(LoginRequest(email, password))

                Log.d("LOGIN", "📥 Réponse reçue!")
                Log.d("LOGIN", "📊 HTTP Code: ${response.code()}")
                Log.d("LOGIN", "📊 isSuccessful: ${response.isSuccessful}")

                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                if (response.isSuccessful) {
                    val body = response.body()

                    // Logs détaillés du body
                    Log.d("LOGIN", "📦 Body reçu: $body")
                    Log.d("LOGIN", "📦 success: ${body?.success}")
                    Log.d("LOGIN", "📦 message: ${body?.message}")
                    Log.d("LOGIN", "📦 token: ${body?.token?.take(20)}...") // Premiers 20 caractères
                    Log.d("LOGIN", "📦 user: ${body?.user}")

                    if (body?.success == true) {
                        Log.d("LOGIN", "✅ SUCCESS = TRUE")
                        val user = body.user
                        Log.d("LOGIN", "👤 User name: ${user?.name}")
                        Log.d("LOGIN", "👤 User email: ${user?.email}")

                        Toast.makeText(
                            this@LoginActivity,
                            "Bienvenue ${user?.name}!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Log.e("LOGIN", "❌ SUCCESS = FALSE")
                        Log.e("LOGIN", "❌ Message d'erreur: ${body?.message}")

                        Toast.makeText(
                            this@LoginActivity,
                            body?.message ?: "Échec de connexion",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("LOGIN", "❌ HTTP Error")
                    Log.e("LOGIN", "❌ Code: ${response.code()}")
                    Log.e("LOGIN", "❌ Message: ${response.message()}")
                    Log.e("LOGIN", "❌ Error Body: $errorBody")

                    Toast.makeText(
                        this@LoginActivity,
                        "Erreur serveur: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                Log.e("LOGIN", "💥 EXCEPTION attrapée!")
                Log.e("LOGIN", "💥 Type: ${e.javaClass.simpleName}")
                Log.e("LOGIN", "💥 Message: ${e.message}")
                Log.e("LOGIN", "💥 Cause: ${e.cause}")
                e.printStackTrace()

                Toast.makeText(
                    this@LoginActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}