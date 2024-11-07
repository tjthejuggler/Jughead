package com.example.jughead.ui.gesturemapping

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jughead.databinding.ItemGestureBinding
import com.example.jughead.data.GestureMapping

class GestureMappingAdapter(
    private val onAssignCommandClicked: (GestureMapping) -> Unit
) : RecyclerView.Adapter<GestureMappingAdapter.GestureViewHolder>() {

    private var gestureMappings: List<GestureMapping> = emptyList()

    fun submitList(list: List<GestureMapping>) {
        gestureMappings = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestureViewHolder {
        val binding = ItemGestureBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GestureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GestureViewHolder, position: Int) {
        holder.bind(gestureMappings[position])
    }

    override fun getItemCount(): Int = gestureMappings.size

    inner class GestureViewHolder(private val binding: ItemGestureBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(gestureMapping: GestureMapping) {
            binding.gestureName.text = gestureMapping.gestureName
            binding.assignedCommand.text = gestureMapping.commandName ?: "No Command Assigned"
            binding.assignCommandButton.setOnClickListener {
                onAssignCommandClicked(gestureMapping)
            }
        }
    }
}
