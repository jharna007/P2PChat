package com.kaifcodec.p2pchat.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kaifcodec.p2pchat.databinding.ItemMessageReceivedBinding
import com.kaifcodec.p2pchat.databinding.ItemMessageSentBinding
import com.kaifcodec.p2pchat.models.ChatMessage
import com.kaifcodec.p2pchat.models.DeliveryStatus
import com.kaifcodec.p2pchat.utils.toTimeFormat

class MessageAdapter(private val currentUserId: String) : 
    ListAdapter<ChatMessage, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ReceivedMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                textMessageContent.text = message.content
                textMessageTime.text = message.timestamp.toTimeFormat()

                // Show delivery status
                val statusText = when (message.deliveryStatus) {
                    DeliveryStatus.SENDING -> "Sending..."
                    DeliveryStatus.SENT -> "Sent"
                    DeliveryStatus.DELIVERED -> "Delivered"
                    DeliveryStatus.FAILED -> "Failed"
                }
                textMessageStatus.text = statusText

                // Set status color
                val statusColor = when (message.deliveryStatus) {
                    DeliveryStatus.SENDING -> android.graphics.Color.GRAY
                    DeliveryStatus.SENT -> android.graphics.Color.BLUE
                    DeliveryStatus.DELIVERED -> android.graphics.Color.GREEN
                    DeliveryStatus.FAILED -> android.graphics.Color.RED
                }
                textMessageStatus.setTextColor(statusColor)
            }
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                textMessageContent.text = message.content
                textMessageTime.text = message.timestamp.toTimeFormat()
                textSenderName.text = message.senderName
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}