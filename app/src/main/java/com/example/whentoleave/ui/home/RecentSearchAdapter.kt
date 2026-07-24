package com.example.whentoleave.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.whentoleave.R
import com.example.whentoleave.util.RecentSearchManager

class RecentSearchAdapter(
    private val onItemClick: (RecentSearchManager.RecentSearch) -> Unit
) : RecyclerView.Adapter<RecentSearchAdapter.ViewHolder>() {

    private var items: List<RecentSearchManager.RecentSearch> = emptyList()

    fun submitList(list: List<RecentSearchManager.RecentSearch>) {
        items = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStart: TextView = itemView.findViewById(R.id.tv_recent_start)
        val tvArrowEnd: TextView = itemView.findViewById(R.id.tv_recent_arrow_end)
        val btnGo: ImageView = itemView.findViewById(R.id.btn_recent_go)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvStart.text = item.startAddress
        holder.tvArrowEnd.text = "→ ${item.endAddress}"
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnGo.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}
