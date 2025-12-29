package com.example.application_gestion_rdv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.io.File
import android.util.Log
import com.example.application_gestion_rdv.adapters.ChatAdapter
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityChatBinding
import com.example.application_gestion_rdv.models.Message
import com.example.application_gestion_rdv.utils.UploadState


class ChatActivity : AppCompatActivity() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter
    private lateinit var fileUploadManager: FileUploadManager
    private lateinit var binding: ActivityChatBinding



    private var conversationId = -1
    private var userId = -1

    // Launchers
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.data?.let(::uploadFile)
            }
        }

    private val documentPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.data?.let(::uploadFile)
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showAttachmentOptions()
            else Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)



        setupToolbar()
        initializeChat()
    }



    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat avec Dr. Test (Mode Test)"
    }

    private fun initializeChat() {
        val apiService = RetrofitClient.chatApiService
        fileUploadManager = FileUploadManager(this, apiService)

        val factory = ChatViewModelFactory(userId, conversationId, apiService)
        viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.messages.collect { messages ->
                        adapter.submitList(messages)
                        if (messages.isNotEmpty()) {
                            binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                        }
                    }
                }

                launch {
                    viewModel.error.collect {
                        it?.let { msg ->
                            Toast.makeText(this@ChatActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.editTextMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.editTextMessage.text?.clear()
            }
        }

        binding.btnAttachment.setOnClickListener {
            checkPermissionAndShowOptions()
        }
    }

    private fun checkPermissionAndShowOptions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(permission)
        } else showAttachmentOptions()
    }

    private fun showAttachmentOptions() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_attachment, null)

        view.findViewById<View>(R.id.optionImage).setOnClickListener {
            FilePickerHelper(this).pickImage(imagePickerLauncher)
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.optionDocument).setOnClickListener {
            FilePickerHelper(this).pickDocument(documentPickerLauncher)
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun uploadFile(uri: Uri) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Upload")
            .setMessage("Envoi en cours...")
            .setCancelable(false)
            .create()

        lifecycleScope.launch {
            fileUploadManager.uploadFile(uri, conversationId, userId)
                .collect { state ->
                    when (state) {
                        is UploadState.Preparing -> progressDialog.show()
                        is UploadState.Uploading ->
                            progressDialog.setMessage("Progression ${state.progress}%")
                        is UploadState.Success -> {
                            if (progressDialog.isShowing) progressDialog.dismiss()
                            viewModel.refreshMessages()
                        }
                        is UploadState.Error -> {
                            if (progressDialog.isShowing) progressDialog.dismiss()
                            Toast.makeText(this@ChatActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(
            currentUserId = userId,
            onImageClick = { openImageFullscreen(it.fileUrl) },
            onDocumentClick = { downloadAndOpenDocument(it) },
            onLongClick = { showMessageOptions(it) }
        )

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = this@ChatActivity.adapter
        }
    }

    private fun openImageFullscreen(url: String?) {
        url ?: return
        startActivity(
            Intent(this, ImageViewerActivity::class.java)
                .putExtra("image_url", url)
        )
    }

    private fun downloadAndOpenDocument(message: Message) {
        lifecycleScope.launch {
            val file = message.fileUrl?.substringAfterLast("/")?.let {
                fileUploadManager.downloadFile(it)
            }
            file?.let(::openFile)
        }
    }

    private fun openFile(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, contentResolver.getType(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Aucune application pour ouvrir ce fichier", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMessageOptions(message: Message) {
        if (message.senderId != userId) return

        AlertDialog.Builder(this)
            .setItems(arrayOf("Supprimer")) { _, _ ->
                Toast.makeText(this, "À implémenter", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}