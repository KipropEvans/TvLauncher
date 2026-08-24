package com.tv.applelauncher.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tv.applelauncher.DetailActivity
import com.tv.applelauncher.R
import com.tv.applelauncher.data.ParentalControls
import com.tv.applelauncher.models.ContentItem

class ContentAdapter(
    private val items: List<ContentItem>,
    private val style: Int,   // 0=WIDE, 1=POSTER, 2=SQUARE
    private val onFocus: (ContentItem) -> Unit
) : RecyclerView.Adapter<ContentAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: View = view.findViewById(R.id.card)
        val image: ImageView = view.findViewById(R.id.content_image)
        val title: TextView = view.findViewById(R.id.content_title)
        val progressBar: View = view.findViewById(R.id.progress_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_content, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // Size by shelf style
        val (w, h) = when (style) {
            0 -> 320 to 180   // WIDE
            2 -> 180 to 180   // SQUARE
            else -> 180 to 270 // POSTER
        }
        holder.card.layoutParams = holder.card.layoutParams.apply {
            width = dp(holder.itemView.context, w); height = dp(holder.itemView.context, h)
        }

        Glide.with(holder.itemView.context)
            .load(item.posterUrl ?: item.backdropUrl)
            .placeholder(R.drawable.art_1)
            .centerCrop().into(holder.image)

        holder.title.text = item.title

        // Apple TV behavior: label hidden until focused
        holder.title.alpha = 0f

        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) 1.08f else 1f)
                .scaleY(if (hasFocus) 1.08f else 1f)
                .setDuration(200).start()
            holder.title.animate().alpha(if (hasFocus) 1f else 0f).setDuration(180).start()
            if (hasFocus) onFocus(item)
        }

        holder.itemView.setOnClickListener {
            val ctx = it.context
            if (item.isApp && item.launchIntent != null) {
                if (ParentalControls.isAppBlocked(ctx, item.launchIntent.`package`)) {
                    requestPinThen(ctx) { ctx.startActivity(item.launchIntent) }
                } else ctx.startActivity(item.launchIntent)
            } else if (!ParentalControls.isAllowed(ctx, item.ratingLevel)) {
                requestPinThen(ctx) { openDetail(ctx, item) }
            } else openDetail(ctx, item)
        }
    }

    private fun openDetail(ctx: Context, item: ContentItem) =
        ctx.startActivity(android.content.Intent(ctx, DetailActivity::class.java)
            .putExtra("item", item))

    private fun dp(ctx: Context, value: Int) =
        (value * ctx.resources.displayMetrics.density).toInt()

    override fun getItemCount() = items.size

    companion object {
        fun requestPinThen(ctx: Context, action: () -> Unit) {
            val input = EditText(ctx).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            AlertDialog.Builder(ctx)
                .setTitle("Parental Control")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    if (ParentalControls.verifyPin(ctx, input.text.toString())) action()
                    else Toast.makeText(ctx, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null).show()
        }
    }
}