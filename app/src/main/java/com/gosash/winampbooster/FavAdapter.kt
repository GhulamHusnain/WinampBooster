package com.gosash.winampbooster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavAdapter(
    private val onPlay: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<FavAdapter.VH>() {

    private val items = mutableListOf<String>()

    fun submit(list: List<String>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_fav, parent, false)
        ThemePrefs.applyToView(v)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = items[position]
        holder.url.text = url
        holder.play.setOnClickListener { onPlay(url) }
        holder.del.setOnClickListener { onDelete(url) }
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val url: TextView = v.findViewById(R.id.favUrl)
        val play: Button = v.findViewById(R.id.favPlay)
        val del: Button = v.findViewById(R.id.favDelete)
    }
}
