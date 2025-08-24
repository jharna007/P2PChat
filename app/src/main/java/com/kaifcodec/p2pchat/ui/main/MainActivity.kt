package com.kaifcodec.p2pchat.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kaifcodec.p2pchat.P2PChatApplication
import com.kaifcodec.p2pchat.R
import com.kaifcodec.p2pchat.databinding.ActivityMainBinding
import com.kaifcodec.p2pchat.ui.chat.ChatActivity
import com.kaifcodec.p2pchat.ui.join.JoinRoomActivity
import com.kaifcodec.p2pchat.ui.viewmodels.ChatViewModel
import com.kaifcodec.p2pchat.utils.copyToClipboard
import com.kaifcodec.p2pchat.utils.showToast
import com.kaifcodec.p2pchat.webrtc.WebRTCClient
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var recentRoomsAdapter: RecentRoomsAdapter

    private val viewModel: ChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as P2PChatApplication
                val webRTCClient = WebRTCClient(this@MainActivity)
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(app.repository, webRTCClient) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "P2PChat"
    }

    private fun setupRecyclerView() {
        recentRoomsAdapter = RecentRoomsAdapter { roomId ->
            joinRoom(roomId)
        }

        binding.rvRecentRooms.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = recentRoomsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateRoom.setOnClickListener {
            createRoom()
        }

        binding.btnJoinRoom.setOnClickListener {
            startActivity(Intent(this, JoinRoomActivity::class.java))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.recentRooms.collect { rooms ->
                recentRoomsAdapter.submitList(rooms)
                binding.tvNoRecentRooms.visibility = if (rooms.isEmpty()) {
                    binding.rvRecentRooms.visibility = android.view.View.GONE
                    android.view.View.VISIBLE
                } else {
                    binding.rvRecentRooms.visibility = android.view.View.VISIBLE
                    android.view.View.GONE
                }
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
            }
        }
    }

    private fun createRoom() {
        val roomCode = viewModel.createRoom()

        // Show room code dialog
        MaterialAlertDialogBuilder(this)
            .setTitle("Room Created")
            .setMessage("Room Code: $roomCode

Share this code with others to join the chat.")
            .setPositiveButton("Copy Code") { _, _ ->
                copyToClipboard(roomCode, "Room Code")
            }
            .setNeutralButton("Join Now") { _, _ ->
                joinRoom(roomCode)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun joinRoom(roomCode: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_ROOM_CODE, roomCode)
        }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("About P2PChat")
            .setMessage("P2PChat is a peer-to-peer messaging app using WebRTC for direct communication between devices.

Version 1.0
Built with Android + Kotlin + WebRTC + Firebase")
            .setPositiveButton("OK", null)
            .show()
    }
}

// Simple adapter for recent rooms
class RecentRoomsAdapter(
    private val onRoomClick: (String) -> Unit
) : androidx.recyclerview.widget.ListAdapter<String, RecentRoomsAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val textView: android.widget.TextView = itemView.findViewById(android.R.id.text1)

        fun bind(roomId: String) {
            textView.text = "Room: $roomId"
            itemView.setOnClickListener { onRoomClick(roomId) }
        }
    }
}
