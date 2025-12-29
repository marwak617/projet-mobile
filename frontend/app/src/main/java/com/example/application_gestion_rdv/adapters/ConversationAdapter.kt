package com.example.application_gestion_rdv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.application_gestion_rdv.R
import com.example.application_gestion_rdv.models.Conversation
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val onConversationClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ViewHolder>(ConversationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvContactName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvUnreadBadge: TextView = itemView.findViewById(R.id.tvUnreadBadge)

        fun bind(conversation: Conversation) {
            // Afficher le nom de l'autre personne
            tvName.text = conversation.medecinName // ou patientName selon le rôle

            // Dernier message
            tvLastMessage.text = conversation.lastMessage ?: "Aucun message"

            // Temps
            tvTime.text = formatTime(conversation.lastMessageAt)

            // Badge messages non lus
            if (conversation.unreadCount > 0) {
                tvUnreadBadge.visibility = View.VISIBLE
                tvUnreadBadge.text = conversation.unreadCount.toString()
            } else {
                tvUnreadBadge.visibility = View.GONE
            }

            // Click
            itemView.setOnClickListener {
                onConversationClick(conversation)
            }
        }

        private fun formatTime(timestamp: String?): String {
            if (timestamp == null) return ""

            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = sdf.parse(timestamp)
                val now = Date()

                val diff = now.time - (date?.time ?: 0)
                val hours = diff / (1000 * 60 * 60)

                when {
                    hours < 1 -> "À l'instant"
                    hours < 24 -> "${hours}h"
                    else -> {
                        val days = hours / 24
                        "${days}j"
                    }
                }
            } catch (e: Exception) {
                ""
            }
        }
    }

    class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
}