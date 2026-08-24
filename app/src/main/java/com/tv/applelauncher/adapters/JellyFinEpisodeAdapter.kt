package com.tv.applelauncher.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tv.applelauncher.PlayerActivity
import com.tv.applelauncher.R
import com.tv.applelauncher.streaming.JellyfinApi
import com.tv.applelauncher.streaming.JellyfinSession
import com.tv.applelauncher.streaming.JfItem

class JellyfinEpisodeAdapter(private val episodes: List<JfItem>) :
    RecyclerView.Adapter<JellyfinEpisodeAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.ep_thumb)
        val number: TextView = view.findViewById(R.id.ep_number)
        val title: TextView = view.findViewById(R.id.ep_title)
        val duration: TextView = view.findViewById(R.id.ep_duration)
        val desc: TextView = view.findViewById(R.id.ep_desc)
        val progress: View = view.findViewById(R.id.ep_progress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ep = episodes[position]
        holder.number.text = String.format("%02d", ep.IndexNumber ?: position + 1)
        holder.title.text = ep.Name
        holder.duration.text = "${(ep.RunTimeTicks ?: 0) / 600_000_000} min"
        holder.desc.text = ep.Overview ?: ""

        Glide.with(holder.itemView.context)
            .load(JellyfinApi.imageUrl(JellyfinSession.serverUrl, ep, "Primary", 400))
            .placeholder(R.drawable.art_2)
            .centerCrop().into(holder.thumb)

        // Resume progress bar
        ep.UserData?.PlayedPercentage?.takeIf { it > 0 && it < 95 }?.let { pct ->
            holder.progress.visibility = View.VISIBLE
            holder.progress.post {
                holder.progress.layoutParams.width =
                    (holder.thumb.width * pct / 100).toInt().coerceAtLeast(4)
                holder.progress.requestLayout()
            }
        } ?: run { holder.progress.visibility = View.GONE }

        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.animate().scaleX(if (hasFocus) 1.03f else 1f)
                .scaleY(if (hasFocus) 1.03f else 1f).setDuration(180).start()
        }

        holder.itemView.setOnClickListener {
            it.context.startActivity(Intent(it.context, PlayerActivity::class.java).apply {
                putExtra("title", "ep.SeriesName—E{ep.SeriesName} — Eep.SeriesName—E{ep.IndexNumber}")
                putExtra("jellyfin_item_id", ep.Id)
                putExtra("stream_url", JellyfinApi.streamUrl(
                    JellyfinSession.serverUrl, ep.Id, JellyfinSession.accessToken))
                putExtra("resume_ticks", ep.UserData?.PlaybackPositionTicks ?: 0L)
            })
        }
    }

    override fun getItemCount() = episodes.size
}