package com.example.application_gestion_rdv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.application_gestion_rdv.adapters.ConversationAdapter
import com.example.application_gestion_rdv.api.RetrofitClient
import com.example.application_gestion_rdv.databinding.ActivityConversationListBinding
import com.example.application_gestion_rdv.models.Conversation
import kotlinx.coroutines.launch
import com.example.application_gestion_rdv.utils.IntentKeys

class ConversationListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationListBinding
    private lateinit var adapter: ConversationAdapter

    // Valeurs fixes pour le mode test
    private val userId: Int = 12
    private val allowedConversationId: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        loadConversations()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Mes conversations"
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupFab() {
        // Désactiver le FAB en mode test pour empêcher la création de nouvelles conversations
        binding.fabNewChat.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter { conversation ->
            openChat(conversation)
        }

        binding.recyclerViewConversations.apply {
            layoutManager = LinearLayoutManager(this@ConversationListActivity)
            adapter = this@ConversationListActivity.adapter
        }
    }

    private fun loadConversations() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.chatApiService.getUserConversations(userId)

                if (response.isSuccessful) {
                    val allConversations = response.body() ?: emptyList()

                    // Filtrer pour ne garder que la conversation autorisée (ID = 1)
                    val filteredConversations = allConversations.filter {
                        it.id == allowedConversationId
                    }

                    if (filteredConversations.isEmpty()) {
                        showEmptyState()
                    } else {
                        showConversations(filteredConversations)
                    }
                } else {
                    showError("Erreur de chargement: ${response.code()}")
                }

            } catch (e: Exception) {
                showError("Erreur réseau: ${e.message}")
                e.printStackTrace()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showEmptyState() {
        binding.tvEmptyState.apply {
            visibility = View.VISIBLE
            text = "Aucune conversation disponible\n(Mode test: conversation ID $allowedConversationId)"
        }
        binding.recyclerViewConversations.visibility = View.GONE
    }

    private fun showConversations(conversations: List<Conversation>) {
        binding.tvEmptyState.visibility = View.GONE
        binding.recyclerViewConversations.visibility = View.VISIBLE
        adapter.submitList(conversations)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun openChat(conversation: Conversation) {


        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("conversation_id", conversation.id)
            putExtra(IntentKeys.USER_ID, userId)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Recharger les conversations à chaque retour sur l'écran
        loadConversations()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}