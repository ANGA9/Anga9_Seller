package com.anga9.seller.ui.support

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.anga9.seller.R

/**
 * Horizontal attachment preview strip adapter.
 * Used in CreateTicketActivity and TicketDetailActivity reply box.
 */
class AttachmentPreviewAdapter(
    private val items: MutableList<Uri>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<AttachmentPreviewAdapter.PreviewVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attachment_preview, parent, false)
        return PreviewVH(view)
    }

    override fun onBindViewHolder(holder: PreviewVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PreviewVH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivThumb: ImageView      = view.findViewById(R.id.ivPreviewThumb)
        private val layoutFileChip: View    = view.findViewById(R.id.layoutFileChip)
        private val ivFileTypeIcon: ImageView = view.findViewById(R.id.ivFileTypeIcon)
        private val tvFileExt: TextView     = view.findViewById(R.id.tvFileExt)
        private val btnRemove: ImageView    = view.findViewById(R.id.btnRemove)

        fun bind(uri: Uri) {
            val context  = itemView.context
            val mimeType = context.contentResolver.getType(uri) ?: ""

            if (mimeType.startsWith("image/")) {
                ivThumb.visibility = View.VISIBLE
                layoutFileChip.visibility = View.GONE
                ivThumb.load(uri) { crossfade(true); placeholder(R.drawable.input_background) }
            } else {
                ivThumb.visibility = View.GONE
                layoutFileChip.visibility = View.VISIBLE
                tvFileExt.text = getExtension(mimeType, uri)
                ivFileTypeIcon.setImageResource(R.drawable.ic_attachment)
            }

            btnRemove.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_ID.toInt()) onRemove(pos)
            }
        }

        private fun getExtension(mimeType: String, uri: Uri): String {
            val path = uri.path ?: ""
            val dotIdx = path.lastIndexOf('.')
            if (dotIdx >= 0 && dotIdx < path.length - 1)
                return ".${path.substring(dotIdx + 1).uppercase().take(4)}"
            return when {
                mimeType.contains("pdf")   -> ".PDF"
                mimeType.contains("word")  -> ".DOC"
                mimeType.contains("excel") -> ".XLS"
                mimeType.contains("video") -> ".VID"
                mimeType.contains("audio") -> ".AUD"
                mimeType.contains("text")  -> ".TXT"
                else -> ".FILE"
            }
        }
    }
}
