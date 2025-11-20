package com.example.application_gestion_rdv

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.application_gestion_rdv.adapters.DoctorsAdapter
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityDoctorsListBinding
import com.example.application_gestion_rdv.models.Doctor
import kotlinx.coroutines.launch

class DoctorsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorsListBinding
    private lateinit var adapter: DoctorsAdapter
    private var allDoctors: List<Doctor> = emptyList()
    private var currentFilteredDoctors: List<Doctor> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentSpecialty: String = "Toutes les spécialités"

    private val specialties = listOf(
        "Toutes les spécialités",
        "Généraliste",
        "Cardiologue",
        "Dentiste",
        "Dermatologue",
        "Pédiatre",
        "Gynécologue",
        "Ophtalmologue",
        "ORL",
        "Psychiatre",
        "Radiologue"
    )

    companion object {
        private const val TAG = "DoctorsListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupSearch()
        setupSpecialtyFilter()
        loadDoctors()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = DoctorsAdapter(emptyList()) { doctor ->
            onDoctorSelected(doctor)
        }

        binding.recyclerViewDoctors.apply {
            layoutManager = LinearLayoutManager(this@DoctorsListActivity)
            adapter = this@DoctorsListActivity.adapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s.toString().trim()
                applyFilters()
            }
        })
    }

    private fun setupSpecialtyFilter() {
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            specialties
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSpecialty.adapter = spinnerAdapter

        binding.spinnerSpecialty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSpecialty = specialties[position]
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyFilters() {
        var filteredList = allDoctors

        // Filtrer par spécialité
        if (currentSpecialty != "Toutes les spécialités") {
            filteredList = filteredList.filter {
                it.specialty?.equals(currentSpecialty, ignoreCase = true) == true
            }
        }

        // Filtrer par recherche
        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter { doctor ->
                doctor.name.contains(currentSearchQuery, ignoreCase = true) ||
                        doctor.specialty?.contains(currentSearchQuery, ignoreCase = true) == true ||
                        doctor.region?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }

        currentFilteredDoctors = filteredList
        adapter.updateDoctors(filteredList)
        updateEmptyState(filteredList.isEmpty())

        Log.d(TAG, "Filters applied - Total: ${allDoctors.size}, Filtered: ${filteredList.size}")
    }

    private fun loadDoctors() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading doctors...")
                val response = RetrofitClient.apiService.getDoctors()

                Log.d(TAG, "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d(TAG, "Response body: $body")

                    if (body?.success == true) {
                        allDoctors = body.doctors ?: emptyList()
                        currentFilteredDoctors = allDoctors

                        Log.d(TAG, "Doctors loaded: ${allDoctors.size}")

                        adapter.updateDoctors(allDoctors)
                        updateEmptyState(allDoctors.isEmpty())
                        showLoading(false)

                    } else {
                        Log.e(TAG, "API returned success=false")
                        showError("Erreur lors du chargement des médecins")
                    }
                } else {
                    Log.e(TAG, "Response not successful: ${response.errorBody()?.string()}")
                    showError("Erreur serveur: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception loading doctors", e)
                showError("Erreur de connexion: ${e.message}")
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewDoctors.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.tvEmptyMessage.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        updateEmptyState(true)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewDoctors.visibility = View.GONE
            binding.tvEmptyMessage.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = if (allDoctors.isEmpty()) {
                "Aucun médecin disponible"
            } else {
                "Aucun résultat pour votre recherche"
            }
        } else {
            binding.recyclerViewDoctors.visibility = View.VISIBLE
            binding.tvEmptyMessage.visibility = View.GONE
        }
    }

    private fun onDoctorSelected(doctor: Doctor) {
        Toast.makeText(
            this,
            "Prendre RDV avec Dr. ${doctor.name}",
            Toast.LENGTH_SHORT
        ).show()

        // TODO: Naviguer vers l'écran de prise de RDV
        // val intent = Intent(this, BookAppointmentActivity::class.java)
        // intent.putExtra("DOCTOR_ID", doctor.id)
        // intent.putExtra("DOCTOR_NAME", doctor.name)
        // intent.putExtra("DOCTOR_SPECIALTY", doctor.specialty)
        // startActivity(intent)
    }
}