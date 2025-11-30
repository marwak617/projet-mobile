package com.example.application_gestion_rdv

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.application_gestion_rdv.adapters.DocumentsAdapter
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityDocumentsBinding
import com.example.application_gestion_rdv.models.MedicalDocument
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class DocumentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentsBinding
    private lateinit var adapter: DocumentsAdapter
    private var userId: Int = -1
    private var selectedFileUri: Uri? = null
    private var selectedDocumentType: String = "mutuelle"

    private val documentTypes = listOf(
        "mutuelle" to "Mutuelle",
        "ordonnance" to "Ordonnance",
        "analyse" to "Analyse médicale",
        "radio" to "Radiographie",
        "autre" to "Autre"
    )

    // Launcher pour sélectionner un fichier (simplifié - fonctionne avec tous les providers)
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            displaySelectedFile(it)
            binding.btnUpload.isEnabled = true
        }
    }

    // Launcher pour demander les permissions (Android 12 et inférieur)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openFilePicker()
        } else {
            Toast.makeText(
                this,
                "Permission refusée. Vous pouvez tout de même sélectionner des fichiers.",
                Toast.LENGTH_LONG
            ).show()
            // Même sans permission, le file picker peut fonctionner
            openFilePicker()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)

        if (userId == -1) {
            Toast.makeText(this, "Erreur: ID utilisateur manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        loadDocuments()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Setup spinner
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            documentTypes.map { it.second }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDocumentType.adapter = spinnerAdapter

        binding.spinnerDocumentType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDocumentType = documentTypes[position].first
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Bouton sélectionner fichier
        binding.btnSelectFile.setOnClickListener {
            checkPermissionAndPickFile()
        }

        // Bouton upload
        binding.btnUpload.setOnClickListener {
            uploadDocument()
        }
    }

    private fun setupRecyclerView() {
        adapter = DocumentsAdapter(emptyList()) { document ->
            showDeleteConfirmation(document)
        }

        binding.recyclerViewDocuments.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewDocuments.adapter = adapter
    }

    private fun checkPermissionAndPickFile() {
        // Android 13+ n'a plus besoin de READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openFilePicker()
        } else {
            // Android 12 et inférieur
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openFilePicker()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    AlertDialog.Builder(this)
                        .setTitle("Permission requise")
                        .setMessage("Cette application a besoin d'accéder à vos fichiers pour les télécharger.")
                        .setPositiveButton("OK") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        .setNegativeButton("Continuer sans permission") { _, _ ->
                            openFilePicker()
                        }
                        .show()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun openFilePicker() {
        // Le file picker standard permet d'accéder à :
        // - Fichiers locaux
        // - Google Drive (si l'app Drive est installée)
        // - Autres providers de contenu
        pickFileLauncher.launch("*/*")
    }

    private fun displaySelectedFile(uri: Uri) {
        val fileName = getFileName(uri)
        binding.tvSelectedFile.text = "Fichier sélectionné : $fileName"
    }

    private fun getFileName(uri: Uri): String {
        var name = "fichier"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun uploadDocument() {
        val fileUri = selectedFileUri ?: return

        binding.progressBarUpload.visibility = View.VISIBLE
        binding.btnUpload.isEnabled = false
        binding.btnSelectFile.isEnabled = false

        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(fileUri)
                if (inputStream == null) {
                    throw Exception("Impossible de lire le fichier")
                }

                val file = File(cacheDir, getFileName(fileUri))

                inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val documentTypeBody = selectedDocumentType.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.apiService.uploadDocument(
                    userId,
                    filePart,
                    documentTypeBody
                )

                binding.progressBarUpload.visibility = View.GONE
                binding.btnUpload.isEnabled = true
                binding.btnSelectFile.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@DocumentsActivity,
                        "Document téléchargé avec succès!",
                        Toast.LENGTH_LONG
                    ).show()

                    selectedFileUri = null
                    binding.tvSelectedFile.text = "Aucun fichier sélectionné"
                    binding.btnUpload.isEnabled = false

                    loadDocuments()
                } else {
                    Toast.makeText(
                        this@DocumentsActivity,
                        response.body()?.message ?: "Erreur d'upload",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Nettoyage du fichier temporaire
                file.delete()

            } catch (e: Exception) {
                binding.progressBarUpload.visibility = View.GONE
                binding.btnUpload.isEnabled = true
                binding.btnSelectFile.isEnabled = true

                Toast.makeText(
                    this@DocumentsActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }
    }

    private fun loadDocuments() {
        binding.progressBarList.visibility = View.VISIBLE
        binding.recyclerViewDocuments.visibility = View.GONE
        binding.tvEmptyMessage.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getUserDocuments(userId)

                binding.progressBarList.visibility = View.GONE

                if (response.isSuccessful && response.body()?.success == true) {
                    val documents = response.body()?.documents ?: emptyList()

                    if (documents.isEmpty()) {
                        binding.tvEmptyMessage.visibility = View.VISIBLE
                    } else {
                        binding.recyclerViewDocuments.visibility = View.VISIBLE
                        adapter.updateDocuments(documents)
                    }
                } else {
                    Toast.makeText(
                        this@DocumentsActivity,
                        "Erreur de chargement",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.tvEmptyMessage.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                binding.progressBarList.visibility = View.GONE
                binding.tvEmptyMessage.visibility = View.VISIBLE

                Toast.makeText(
                    this@DocumentsActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDeleteConfirmation(document: MedicalDocument) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le document")
            .setMessage("Êtes-vous sûr de vouloir supprimer ce document ?")
            .setPositiveButton("Supprimer") { _, _ ->
                deleteDocument(document)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteDocument(document: MedicalDocument) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteDocument(
                    userId,
                    document.filename
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@DocumentsActivity,
                        "Document supprimé",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadDocuments()
                } else {
                    Toast.makeText(
                        this@DocumentsActivity,
                        response.body()?.message ?: "Erreur de suppression",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@DocumentsActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}