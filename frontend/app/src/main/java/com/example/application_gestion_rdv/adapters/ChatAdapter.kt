package com.example.application_gestion_rdv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.application_gestion_rdv.models.Message
import com.example.application_gestion_rdv.R
import java.text.SimpleDateFormat
import java.util.*


class ChatAdapter(
    private val currentUserId: Int,
    private val onImageClick: (Message) -> Unit,
    private val onDocumentClick: (Message) -> Unit,
    private val onLongClick: (Message) -> Unit
) : ListAdapter<Message, ChatAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutReceived: View = itemView.findViewById(R.id.layoutReceived)
        private val layoutSent: View = itemView.findViewById(R.id.layoutSent)

        // Vues pour messages reçus
        private val textMessageReceived: TextView = itemView.findViewById(R.id.textMessageReceived)
        private val textTimeReceived: TextView = itemView.findViewById(R.id.textTimeReceived)
        private val imageReceived: ImageView = itemView.findViewById(R.id.imageReceived)
        private val documentReceived: View = itemView.findViewById(R.id.documentReceived)
        private val documentNameReceived: TextView = itemView.findViewById(R.id.documentNameReceived)

        // Vues pour messages envoyés
        private val textMessageSent: TextView = itemView.findViewById(R.id.textMessageSent)
        private val textTimeSent: TextView = itemView.findViewById(R.id.textTimeSent)
        private val imageSent: ImageView = itemView.findViewById(R.id.imageSent)
        private val documentSent: View = itemView.findViewById(R.id.documentSent)
        private val documentNameSent: TextView = itemView.findViewById(R.id.documentNameSent)

        fun bind(message: Message) {
            val isSent = message.senderId == currentUserId

            layoutReceived.visibility = if (isSent) View.GONE else View.VISIBLE
            layoutSent.visibility = if (isSent) View.VISIBLE else View.GONE

            when (message.messageType) {
                "text" -> bindTextMessage(message, isSent)
                "image" -> bindImageMessage(message, isSent)
                "document" -> bindDocumentMessage(message, isSent)
            }
            // Long click pour options
            itemView.setOnLongClickListener {
                onLongClick(message)
                true
            }
        }

        private fun bindTextMessage(message: Message, isSent: Boolean) {
            if (isSent) {
                textMessageSent.visibility = View.VISIBLE
                textMessageSent.text = message.content
                textTimeSent.text = formatTime(message.createdAt)
                imageSent.visibility = View.GONE
                documentSent.visibility = View.GONE
            } else {
                textMessageReceived.visibility = View.VISIBLE
                textMessageReceived.text = message.content
                textTimeReceived.text = formatTime(message.createdAt)
                imageReceived.visibility = View.GONE
                documentReceived.visibility = View.GONE
            }
        }

        private fun bindImageMessage(message: Message, isSent: Boolean) {
            if (isSent) {
                textMessageSent.visibility = View.GONE
                imageSent.visibility = View.VISIBLE
                documentSent.visibility = View.GONE

                Glide.with(itemView.context)
                    .load(message.fileUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .into(imageSent)

                imageSent.setOnClickListener {
                    onImageClick(message)
                }

                textTimeSent.text = formatTime(message.createdAt)
            } else {
                textMessageReceived.visibility = View.GONE
                imageReceived.visibility = View.VISIBLE
                documentReceived.visibility = View.GONE

                Glide.with(itemView.context)
                    .load(message.fileUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .into(imageReceived)

                imageReceived.setOnClickListener {
                    onImageClick(message)
                }

                textTimeReceived.text = formatTime(message.createdAt)
            }
        }

        private fun bindDocumentMessage(message: Message, isSent: Boolean) {
            if (isSent) {
                textMessageSent.visibility = View.GONE
                imageSent.visibility = View.GONE
                documentSent.visibility = View.VISIBLE
                documentNameSent.text = message.content

                documentSent.setOnClickListener {
                    onDocumentClick(message)
                }

                textTimeSent.text = formatTime(message.createdAt)
            } else {
                textMessageReceived.visibility = View.GONE
                imageReceived.visibility = View.GONE
                documentReceived.visibility = View.VISIBLE
                documentNameReceived.text = message.content

                documentReceived.setOnClickListener {
                    onDocumentClick(message)
                }

                textTimeReceived.text = formatTime(message.createdAt)
            }
        }

        private fun formatTime(timestamp: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = sdf.parse(timestamp)
                val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                timestamp
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}