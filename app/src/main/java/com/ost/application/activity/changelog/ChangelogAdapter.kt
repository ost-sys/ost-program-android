package com.ost.application.activity.changelog

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ost.application.R
import dev.oneuiproject.oneui.widget.CardItemView

class ChangelogAdapter(private val items: List<ChangelogItem>) :
    RecyclerView.Adapter<ChangelogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardItemView: CardItemView = view.findViewById(R.id.cardItemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.changelog_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.cardItemView.title = item.version
        holder.cardItemView.summary = item.body

        holder.itemView.setOnClickListener {
            item.downloadUrl?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = items.size
}
