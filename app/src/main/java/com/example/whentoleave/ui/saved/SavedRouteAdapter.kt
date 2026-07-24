package com.example.whentoleave.ui.saved

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.whentoleave.data.local.SavedRoute
import com.example.whentoleave.databinding.ItemSavedRouteBinding

class SavedRouteAdapter(
    private val onGoClick: (SavedRoute) -> Unit,
    private val onLongClick: (SavedRoute) -> Unit
) : ListAdapter<SavedRoute, SavedRouteAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemSavedRouteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(route: SavedRoute) {
            binding.tvLabel.text = route.label
            binding.tvStart.text = "출발 ${route.startAddress}"
            binding.tvEnd.text   = "도착 ${route.endAddress}"
            binding.btnGo.setOnClickListener { onGoClick(route) }
            binding.root.setOnLongClickListener { onLongClick(route); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemSavedRouteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SavedRoute>() {
            override fun areItemsTheSame(a: SavedRoute, b: SavedRoute) = a.id == b.id
            override fun areContentsTheSame(a: SavedRoute, b: SavedRoute) = a == b
        }
    }
}