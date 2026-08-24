package com.tv.applelauncher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tv.applelauncher.R
import com.tv.applelauncher.models.ContentItem
import com.tv.applelauncher.models.Shelf

class ShelfAdapter(
    private val shelves: List<Shelf>,
    private val onItemFocus: (ContentItem) -> Unit
) : RecyclerView.Adapter<ShelfAdapter.ShelfVH>() {

    class ShelfVH(view: View) : RecyclerView.ViewHolder(view) {
        val title: android.widget.TextView = view.findViewById(R.id.shelf_title)
        val items: RecyclerView = view.findViewById(R.id.shelf_items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ShelfVH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_shelf, parent, false))

    override fun onBindViewHolder(holder: ShelfVH, position: Int) {
        val shelf = shelves[position]
        holder.title.text = shelf.title
        holder.items.layoutManager =
            LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.items.adapter = ContentAdapter(shelf.items, shelf.style) { onItemFocus(it) }
    }

    override fun getItemCount() = shelves.size
}