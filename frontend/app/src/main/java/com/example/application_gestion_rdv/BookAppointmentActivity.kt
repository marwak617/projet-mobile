package com.example.application_gestion_rdv

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.application_gestion_rdv.adapters.TimeSlotsAdapter
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityBookAppointmentBinding
import com.example.application_gestion_rdv.models.AppointmentCreate
import com.example.application_gestion_rdv.models.TimeSlot
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookAppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookAppointmentBinding
    private lateinit var timeSlotsAdapter: TimeSlotsAdapter
    private var doctorId: Int = -1
    private var patientId: Int = -1
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Récupérer les données
        doctorId = intent.getIntExtra("DOCTOR_ID", -1)
        patientId = intent.getIntExtra("PATIENT_ID", -1)
        val doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Médecin"
        val doctorSpecialty = intent.getStringExtra("DOCTOR_SPECIALTY") ?: ""

        if (doctorId == -1 || patientId == -1) {
            Toast.makeText(this, "Erreur: Données manquantes", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Afficher les infos du médecin
        binding.tvDoctorName.text = "Dr. $doctorName"
        binding.tvDoctorSpecialty.text = doctorSpecialty

        setupUI()
        setupRecyclerView()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnConfirm.setOnClickListener {
            bookAppointment()
        }
    }

    private fun setupRecyclerView() {
        timeSlotsAdapter = TimeSlotsAdapter(emptyList()) { slot ->
            selectedTime = slot.time
            binding.btnConfirm.isEnabled = true
        }

        binding.recyclerViewTimeSlots.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerViewTimeSlots.adapter = timeSlotsAdapter
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Formater la date
                val cal = Calendar.getInstance()
                cal.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDate = dateFormat.format(cal.time)

                val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                binding.btnSelectDate.text = displayFormat.format(cal.time)

                // Charger les créneaux disponibles
                loadAvailableSlots()
            },
            year, month, day
        )

        // Ne pas permettre de sélectionner une date passée
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun loadAvailableSlots() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewTimeSlots.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getDoctorAvailability(
                    doctorId,
                    selectedDate
                )

                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body()?.success == true) {
                    val slots = response.body()?.slots ?: emptyList()

                    binding.tvTimeSlotLabel.visibility = View.VISIBLE
                    binding.recyclerViewTimeSlots.visibility = View.VISIBLE

                    timeSlotsAdapter.updateSlots(slots)
                } else {
                    Toast.makeText(
                        this@BookAppointmentActivity,
                        "Erreur de chargement des créneaux",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@BookAppointmentActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bookAppointment() {
        val reason = binding.etReason.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        if (reason.isEmpty()) {
            binding.etReason.error = "Motif requis"
            return
        }

        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner une date et une heure", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnConfirm.isEnabled = false

        lifecycleScope.launch {
            try {
                // Créer la date complète au format ISO
                val appointmentDateTime = "${selectedDate}T${selectedTime}:00"

                val appointmentCreate = AppointmentCreate(
                    doctor_id = doctorId,
                    appointment_date = appointmentDateTime,
                    reason = reason,
                    notes = notes.ifEmpty { null }
                )

                val response = RetrofitClient.apiService.createAppointment(
                    patientId,
                    appointmentCreate
                )

                binding.progressBar.visibility = View.GONE
                binding.btnConfirm.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@BookAppointmentActivity,
                        "Rendez-vous créé avec succès!",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@BookAppointmentActivity,
                        response.body()?.message ?: "Erreur de création",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnConfirm.isEnabled = true

                Toast.makeText(
                    this@BookAppointmentActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}