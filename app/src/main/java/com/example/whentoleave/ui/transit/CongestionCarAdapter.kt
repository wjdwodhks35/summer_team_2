package com.example.whentoleave.ui.transit

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.whentoleave.R

class CongestionCarAdapter(private var congestionList: List<Int>) :
    RecyclerView.Adapter<CongestionCarAdapter.CarViewHolder>() {

    inner class CarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bar: View = view.findViewById(R.id.bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_congestion_car, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val level = congestionList[position]
        holder.bar.setBackgroundColor(
            when {
                level < 40 -> Color.parseColor("#3DDC97") // 여유 (mint)
                level < 70 -> Color.parseColor("#FFB627") // 보통 (amber)
                else       -> Color.parseColor("#FF6B57") // 혼잡 (coral)
            }
        )
    }

    override fun getItemCount() = congestionList.size

    fun update(newList: List<Int>) {
        congestionList = newList
        notifyDataSetChanged()
    }
}
