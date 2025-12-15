package com.example.application_gestion_rdv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
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
    private var capturedPhotoUri: Uri? = null
    private var selectedDocumentType: String = "mutuelle"

    private val documentTypes = listOf(
        "mutuelle" to "Mutuelle",
        "ordonnance" to "Ordonnance",
        "analyse" to "Analyse médicale",
        "radio" to "Radiographie",
        "autre" to "Autre"
    )

    // Launcher pour sélectionner un fichier
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            displaySelectedFile(it)
            binding.btnUpload.isEnabled = true
        }
    }

    // Launcher pour prendre une photo
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            capturedPhotoUri?.let { uri ->
                // Compresser et optimiser l'image
                val optimizedFile = compressAndRotateImage(uri)
                if (optimizedFile != null) {
                    selectedFileUri = Uri.fromFile(optimizedFile)
                    displaySelectedFile(selectedFileUri!!)
                    binding.btnUpload.isEnabled = true
                } else {
                    Toast.makeText(this, "Erreur de traitement de l'image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher pour permission stockage
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openFilePicker()
        } else {
            Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher pour permission caméra
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
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
            checkStoragePermissionAndPickFile()
        }

        // Bouton scanner document
        binding.btnScanDocument.setOnClickListener {
            checkCameraPermissionAndScan()
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

    private fun checkStoragePermissionAndPickFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openFilePicker()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openFilePicker()
                }
                else -> {
                    requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                showScanOptions()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(this)
                    .setTitle("Permission caméra")
                    .setMessage("L'application a besoin d'accéder à la caméra pour scanner vos documents.")
                    .setPositiveButton("OK") { _, _ ->
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showScanOptions() {
        AlertDialog.Builder(this)
            .setTitle("Scanner un document")
            .setMessage("Prenez une photo claire de votre document avec un bon éclairage.")
            .setPositiveButton("Prendre une photo") { _, _ ->
                openCamera()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun openFilePicker() {
        pickFileLauncher.launch("*/*")
    }

    private fun openCamera() {
        try {
            val photoFile = File.createTempFile(
                "document_${System.currentTimeMillis()}",
                ".jpg",
                cacheDir
            )

            capturedPhotoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )

            takePictureLauncher.launch(capturedPhotoUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compressAndRotateImage(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Lire l'orientation EXIF
            val exifInputStream = contentResolver.openInputStream(uri)
            val exif = ExifInterface(exifInputStream!!)
            exifInputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            // Rotation selon l'orientation
            val rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(originalBitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(originalBitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(originalBitmap, 270f)
                else -> originalBitmap
            }

            // Compression (max 1920x1920, qualité 85%)
            val maxSize = 1920
            val scale = minOf(
                maxSize.toFloat() / rotatedBitmap.width,
                maxSize.toFloat() / rotatedBitmap.height,
                1f
            )

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    rotatedBitmap,
                    (rotatedBitmap.width * scale).toInt(),
                    (rotatedBitmap.height * scale).toInt(),
                    true
                )
            } else {
                rotatedBitmap
            }

            // Sauvegarder
            val outputFile = File(cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outputFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            // Libérer mémoire
            if (rotatedBitmap != originalBitmap) rotatedBitmap.recycle()
            if (scaledBitmap != rotatedBitmap) scaledBitmap.recycle()
            originalBitmap.recycle()

            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun displaySelectedFile(uri: Uri) {
        val fileName = getFileName(uri)
        val fileSize = getFileSize(uri)
        binding.tvSelectedFile.text = "📄 $fileName (${formatFileSize(fileSize)})"
    }

    private fun getFileName(uri: Uri): String {
        var name = "document_${System.currentTimeMillis()}.jpg"
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

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = it.getLong(sizeIndex)
                }
            }
        }
        return size
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    private fun uploadDocument() {
        val fileUri = selectedFileUri ?: return

        binding.progressBarUpload.visibility = View.VISIBLE
        binding.btnUpload.isEnabled = false
        binding.btnSelectFile.isEnabled = false
        binding.btnScanDocument.isEnabled = false

        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(fileUri)
                val file = File(cacheDir, getFileName(fileUri))

                inputStream?.use { input ->
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
                binding.btnScanDocument.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@DocumentsActivity,
                        "✅ Document téléchargé avec succès!",
                        Toast.LENGTH_LONG
                    ).show()

                    selectedFileUri = null
                    capturedPhotoUri = null
                    binding.tvSelectedFile.text = "Aucun fichier sélectionné"
                    binding.btnUpload.isEnabled = false

                    loadDocuments()
                    file.delete()
                } else {
                    Toast.makeText(
                        this@DocumentsActivity,
                        response.body()?.message ?: "Erreur d'upload",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                binding.progressBarUpload.visibility = View.GONE
                binding.btnUpload.isEnabled = true
                binding.btnSelectFile.isEnabled = true
                binding.btnScanDocument.isEnabled = true

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