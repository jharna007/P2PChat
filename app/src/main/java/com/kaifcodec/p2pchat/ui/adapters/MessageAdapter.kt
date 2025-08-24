package com.kaifcodec.p2pchat.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kaifcodec.p2pchat.R
import com.kaifcodec.p2pchat.models.ChatMessage
import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.formatTimestamp

class MessageAdapter : ListAdapter<ChatMessage, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageContent: TextView = itemView.findViewById(R.id.tv_message_content)
        private val messageTime: TextView = itemView.findViewById(R.id.tv_message_time)
        private val deliveryStatus: TextView = itemView.findViewById(R.id.tv_delivery_status)

        fun bind(message: ChatMessage) {
            messageContent.text = message.content
            messageTime.text = message.timestamp.formatTimestamp()

            // Set delivery status for sent messages only
            if (message.isFromMe) {
                deliveryStatus.visibility = View.VISIBLE
                deliveryStatus.text = when (message.deliveryState) {
                    Constants.MESSAGE_STATE_SENDING -> "Sending..."
                    Constants.MESSAGE_STATE_SENT -> "Sent"
                    Constants.MESSAGE_STATE_DELIVERED -> "Delivered"
                    Constants.MESSAGE_STATE_FAILED -> "Failed"
                    else -> ""
                }
            } else {
                deliveryStatus.visibility = View.GONE
            }

            // Apply different styling for sent vs received messages
            val layoutParams = itemView.layoutParams as ViewGroup.MarginLayoutParams
            if (message.isFromMe) {
                // Align to right for sent messages
                layoutParams.leftMargin = itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_large)
                layoutParams.rightMargin = itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_small)
                itemView.setBackgroundResource(R.drawable.bg_message_sent)
            } else {
                // Align to left for received messages
                layoutParams.leftMargin = itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_small)
                layoutParams.rightMargin = itemView.context.resources.getDimensionPixelSize(R.dimen.message_margin_large)
                itemView.setBackgroundResource(R.drawable.bg_message_received)
            }
            itemView.layoutParams = layoutParams
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
