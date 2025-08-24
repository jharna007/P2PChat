package com.kaifcodec.p2pchat.ui.chat

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kaifcodec.p2pchat.P2PChatApplication
import com.kaifcodec.p2pchat.R
import com.kaifcodec.p2pchat.databinding.ActivityChatBinding
import com.kaifcodec.p2pchat.models.ConnectionState
import com.kaifcodec.p2pchat.ui.adapters.MessageAdapter
import com.kaifcodec.p2pchat.ui.viewmodels.ChatViewModel
import com.kaifcodec.p2pchat.utils.copyToClipboard
import com.kaifcodec.p2pchat.utils.isValidMessage
import com.kaifcodec.p2pchat.webrtc.WebRTCClient
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter
    private var roomCode: String? = null

    private val viewModel: ChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as P2PChatApplication
                val webRTCClient = WebRTCClient(this@ChatActivity)
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(app.repository, webRTCClient) as T
            }
        }
    }

    companion object {
        const val EXTRA_ROOM_CODE = "extra_room_code"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomCode = intent.getStringExtra(EXTRA_ROOM_CODE)
        if (roomCode == null) {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Join the room
        viewModel.joinRoom(roomCode!!)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Room: $roomCode"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabSend.setOnClickListener {
            sendMessage()
        }

        binding.etMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                messageAdapter.submitList(messages) {
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.smoothScrollToPosition(messages.size - 1)
                    }
                }

                // Show/hide empty state
                binding.tvEmptyMessages.visibility = if (messages.isEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                updateConnectionStatus(state)
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Disable input during loading
                binding.etMessage.isEnabled = !isLoading
                binding.fabSend.isEnabled = !isLoading
            }
        }
    }

    private fun updateConnectionStatus(state: ConnectionState) {
        val (statusText, statusColor) = when (state) {
            is ConnectionState.Disconnected -> "Disconnected" to getColor(R.color.status_disconnected)
            is ConnectionState.Connecting -> "Connecting..." to getColor(R.color.status_connecting)
            is ConnectionState.Connected -> "Connected" to getColor(R.color.status_connected)
            is ConnectionState.Reconnecting -> "Reconnecting..." to getColor(R.color.status_connecting)
            is ConnectionState.Failed -> "Connection Failed" to getColor(R.color.status_failed)
        }

        binding.tvConnectionStatus.apply {
            text = statusText
            setTextColor(statusColor)
        }

        // Enable/disable input based on connection state
        val isConnected = state is ConnectionState.Connected
        binding.etMessage.isEnabled = isConnected
        binding.fabSend.isEnabled = isConnected && binding.etMessage.text.toString().isValidMessage()
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isValidMessage()) {
            viewModel.sendMessage(messageText)
            binding.etMessage.text?.clear()
        } else {
            Snackbar.make(binding.root, "Please enter a valid message", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_copy_room_code -> {
                roomCode?.let { copyToClipboard(it, "Room Code") }
                true
            }
            R.id.action_leave_room -> {
                viewModel.leaveRoom()
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.leaveRoom()
    }

    override fun onBackPressed() {
        viewModel.leaveRoom()
        super.onBackPressed()
    }
}
