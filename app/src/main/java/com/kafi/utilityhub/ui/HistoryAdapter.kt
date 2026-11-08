package com.kafi.utilityhub.ui

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kafi.utilityhub.R
import com.kafi.utilityhub.data.HistoryEntity
import java.util.Date

class HistoryAdapter(private val onDelete: (HistoryEntity) -> Unit) :
    ListAdapter<HistoryEntity, HistoryAdapter.VH>(DIFF) {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvTitle)
        val subtitle: TextView = v.findViewById(R.id.tvSubtitle)
        val time: TextView = v.findViewById(R.id.tvTime)
        val delete: Button = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.title.text = item.title
        holder.subtitle.text = item.content.take(140)
        holder.time.text = DateFormat.format("dd MMM yyyy, HH:mm", Date(item.timestamp))
        holder.delete.setOnClickListener { onDelete(item) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HistoryEntity>() {
            override fun areItemsTheSame(a: HistoryEntity, b: HistoryEntity) = a.id == b.id
            override fun areContentsTheSame(a: HistoryEntity, b: HistoryEntity) = a == b
        }
    }
}