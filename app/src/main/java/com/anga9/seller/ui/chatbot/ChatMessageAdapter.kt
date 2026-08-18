package com.anga9.seller.ui.chatbot
import com.anga9.seller.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ChatMessageAdapter(
    private val onThumbsUp: (assistantMessageId: String) -> Unit,
    private val onThumbsDown: (assistantMessageId: String) -> Unit,
) : ListAdapter<ChatUiMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_ASSISTANT = 1
        private const val TYPE_SYSTEM = 2
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position).role) {
        MessageRole.USER -> TYPE_USER
        MessageRole.ASSISTANT -> TYPE_ASSISTANT
        MessageRole.SYSTEM -> TYPE_SYSTEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserMessageVH(inflater.inflate(R.layout.item_message_user, parent, false))
            TYPE_ASSISTANT -> AssistantMessageVH(inflater.inflate(R.layout.item_message_assistant, parent, false))
            else -> SystemMessageVH(inflater.inflate(R.layout.item_message_system, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserMessageVH -> holder.bind(message)
            is AssistantMessageVH -> holder.bind(message, onThumbsUp, onThumbsDown)
            is SystemMessageVH -> holder.bind(message)
        }
    }

    class UserMessageVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
        fun bind(message: ChatUiMessage) { tvMessage.text = message.text }
    }

    class AssistantMessageVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvAssistantMessage)
        private val tvHint: TextView = itemView.findViewById(R.id.tvInlineHint)
        private val tvCursor: TextView = itemView.findViewById(R.id.tvStreamingCursor)
        private val btnThumbsUp: ImageButton = itemView.findViewById(R.id.btnThumbsUp)
        private val btnThumbsDown: ImageButton = itemView.findViewById(R.id.btnThumbsDown)
        private val feedbackContainer: View = itemView.findViewById(R.id.feedbackContainer)

        fun bind(message: ChatUiMessage, onThumbsUp: (String) -> Unit, onThumbsDown: (String) -> Unit) {
            tvMessage.text = message.text
            tvCursor.visibility = if (message.isStreaming) View.VISIBLE else View.GONE
            if (message.inlineHint != null) { tvHint.text = message.inlineHint; tvHint.visibility = View.VISIBLE }
            else tvHint.visibility = View.GONE
            val msgId = message.assistantMessageId
            if (!message.isStreaming && msgId != null && msgId.isNotEmpty()) {
                feedbackContainer.visibility = View.VISIBLE
                when (message.feedbackGiven) {
                    1 -> { btnThumbsUp.alpha = 1f; btnThumbsDown.alpha = 0.3f }
                    -1 -> { btnThumbsUp.alpha = 0.3f; btnThumbsDown.alpha = 1f }
                    else -> { btnThumbsUp.alpha = 0.6f; btnThumbsDown.alpha = 0.6f }
                }
                btnThumbsUp.setOnClickListener { onThumbsUp(msgId) }
                btnThumbsDown.setOnClickListener { onThumbsDown(msgId) }
            } else feedbackContainer.visibility = View.GONE
        }
    }

    class SystemMessageVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvSystemMessage)
        fun bind(message: ChatUiMessage) { tvMessage.text = message.text }
    }
}

class ChatMessageDiffCallback : DiffUtil.ItemCallback<ChatUiMessage>() {
    override fun areItemsTheSame(oldItem: ChatUiMessage, newItem: ChatUiMessage) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ChatUiMessage, newItem: ChatUiMessage) = oldItem == newItem
}
