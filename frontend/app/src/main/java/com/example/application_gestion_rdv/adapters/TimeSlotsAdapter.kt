package com.example.application_gestion_rdv.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.application_gestion_rdv.R
import com.example.application_gestion_rdv.models.TimeSlot

class TimeSlotsAdapter(
    private var timeSlots: List<TimeSlot>,
    private val onSlotSelected: (TimeSlot) -> Unit
) : RecyclerView.Adapter<TimeSlotsAdapter.TimeSlotViewHolder>() {

    private var selectedPosition = -1

    class TimeSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardTimeSlot: CardView = itemView.findViewById(R.id.cardTimeSlot)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_slot, parent, false)
        return TimeSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val slot = timeSlots[position]

        holder.tvTime.text = slot.time

        // Gérer l'apparence selon disponibilité et sélection
        when {
            !slot.available -> {
                // Créneau occupé
                holder.cardTimeSlot.setCardBackgroundColor(Color.parseColor("#CCCCCC"))
                holder.tvTime.setTextColor(Color.parseColor("#999999"))
                holder.cardTimeSlot.isEnabled = false
            }
            position == selectedPosition -> {
                // Créneau sélectionné
                holder.cardTimeSlot.setCardBackgroundColor(Color.parseColor("#4CAF50"))
                holder.tvTime.setTextColor(Color.WHITE)
                holder.cardTimeSlot.isEnabled = true
            }
            else -> {
                // Créneau disponible
                holder.cardTimeSlot.setCardBackgroundColor(Color.WHITE)
                holder.tvTime.setTextColor(Color.parseColor("#333333"))
                holder.cardTimeSlot.isEnabled = true
            }
        }

        // Click listener
        holder.cardTimeSlot.setOnClickListener {
            if (slot.available) {
                val previousPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onSlotSelected(slot)
            }
        }
    }

    override fun getItemCount(): Int = timeSlots.size

    fun updateSlots(newSlots: List<TimeSlot>) {
        timeSlots = newSlots
        selectedPosition = -1
        notifyDataSetChanged()
    }
}