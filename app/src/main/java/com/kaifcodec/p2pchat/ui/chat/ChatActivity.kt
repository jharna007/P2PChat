package com.kaifcodec.p2pchat.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.kaifcodec.p2pchat.R
import com.kaifcodec.p2pchat.databinding.ActivityChatBinding
import com.kaifcodec.p2pchat.models.ConnectionState
import com.kaifcodec.p2pchat.ui.adapters.MessageAdapter
import com.kaifcodec.p2pchat.ui.viewmodels.ChatViewModel
import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.showToast

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter

    private var roomId: String = ""
    private var isCaller: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentData()
        setupUI()
        observeViewModel()
        joinRoom()
    }

    private fun getIntentData() {
        roomId = intent.getStringExtra(Constants.EXTRA_ROOM_ID) ?: ""
        isCaller = intent.getBooleanExtra(Constants.EXTRA_IS_CALLER, false)

        if (roomId.isEmpty()) {
            showToast("Invalid room ID")
            finish()
            return
        }
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Room: $roomId"
        }

        // Setup RecyclerView
        messageAdapter = MessageAdapter(chatViewModel.getCurrentUserId())
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = messageAdapter
        }

        // Setup message input
        binding.editTextMessage.addTextChangedListener { text ->
            binding.buttonSend.isEnabled = !text.isNullOrBlank()
        }

        binding.buttonSend.setOnClickListener {
            sendMessage()
        }

        // Initially disable send button
        binding.buttonSend.isEnabled = false

        updateConnectionStatus(ConnectionState.DISCONNECTED)
    }

    private fun observeViewModel() {
        chatViewModel.messages.observe(this) { messages ->
            messageAdapter.submitList(messages) {
                // Scroll to bottom when new messages arrive
                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                }
            }
        }

        chatViewModel.connectionState.observe(this) { state ->
            updateConnectionStatus(state)
        }

        chatViewModel.isConnected.observe(this) { connected ->
            binding.buttonSend.isEnabled = connected && binding.editTextMessage.text?.isNotBlank() == true

            if (connected) {
                showToast("Connected! You can now chat.")
            }
        }

        chatViewModel.error.observe(this) { error ->
            if (error.isNotEmpty()) {
                showToast(error, Toast.LENGTH_LONG)
            }
        }
    }

    private fun joinRoom() {
        if (!isCaller) {
            chatViewModel.joinRoom(roomId)
        }
        // If caller, room is already created by MainActivity
    }

    private fun sendMessage() {
        val messageText = binding.editTextMessage.text.toString().trim()
        if (messageText.isNotEmpty() && messageText.length <= Constants.MAX_MESSAGE_LENGTH) {
            chatViewModel.sendMessage(messageText)
            binding.editTextMessage.text?.clear()
        } else if (messageText.length > Constants.MAX_MESSAGE_LENGTH) {
            showToast("Message too long. Maximum ${Constants.MAX_MESSAGE_LENGTH} characters.")
        }
    }

    private fun updateConnectionStatus(state: ConnectionState) {
        val (statusText, statusColor) = when (state) {
            ConnectionState.DISCONNECTED -> "Disconnected" to getColor(R.color.status_disconnected)
            ConnectionState.CONNECTING -> "Connecting..." to getColor(R.color.status_connecting)
            ConnectionState.CONNECTED -> "Connected" to getColor(R.color.status_connected)
            ConnectionState.RECONNECTING -> "Reconnecting..." to getColor(R.color.status_connecting)
            ConnectionState.FAILED -> "Connection Failed" to getColor(R.color.status_failed)
        }

        binding.textConnectionStatus.text = statusText
        binding.textConnectionStatus.setTextColor(statusColor)
    }

    private fun generateQRCode(text: String): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun showQRCode() {
        val qrBitmap = generateQRCode(roomId)
        if (qrBitmap != null) {
            val imageView = android.widget.ImageView(this).apply {
                setImageBitmap(qrBitmap)
                setPadding(32, 32, 32, 32)
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Room QR Code")
                .setMessage("Others can scan this QR code to join the room")
                .setView(imageView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Share Code") { _, _ ->
                    shareRoomCode()
                }
                .show()
        } else {
            showToast("Failed to generate QR code")
        }
    }

    private fun shareRoomCode() {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "Join my P2P chat room with code: $roomId")
            putExtra(android.content.Intent.EXTRA_SUBJECT, "P2P Chat Room Invitation")
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Room Code"))
    }

    private fun copyRoomCode() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Room Code", roomId)
        clipboard.setPrimaryClip(clip)
        showToast("Room code copied to clipboard")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_show_qr -> {
                showQRCode()
                true
            }
            R.id.action_copy_code -> {
                copyRoomCode()
                true
            }
            R.id.action_share_code -> {
                shareRoomCode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Leave Room")
            .setMessage("Are you sure you want to leave the chat room?")
            .setPositiveButton("Leave") { _, _ ->
                chatViewModel.leaveRoom()
                super.onBackPressed()
            }
            .setNegativeButton("Stay", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        chatViewModel.leaveRoom()
    }
}