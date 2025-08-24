package com.kaifcodec.p2pchat.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentIntegrator
import com.journeyapps.barcodescanner.ScanOptions
import com.kaifcodec.p2pchat.databinding.ActivityMainBinding
import com.kaifcodec.p2pchat.ui.chat.ChatActivity
import com.kaifcodec.p2pchat.ui.join.JoinRoomActivity
import com.kaifcodec.p2pchat.ui.viewmodels.ChatViewModel
import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.showToast

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val chatViewModel: ChatViewModel by viewModels()

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val roomCode = result.contents
            joinRoom(roomCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.apply {
            btnCreateRoom.setOnClickListener {
                createNewRoom()
            }

            btnJoinRoom.setOnClickListener {
                showJoinRoomDialog()
            }

            btnScanQr.setOnClickListener {
                scanQRCode()
            }

            btnJoinRoomManual.setOnClickListener {
                startActivity(Intent(this@MainActivity, JoinRoomActivity::class.java))
            }
        }
    }

    private fun observeViewModel() {
        chatViewModel.roomId.observe(this) { roomId ->
            if (roomId.isNotEmpty()) {
                startChatActivity(roomId, true)
            }
        }

        chatViewModel.error.observe(this) { error ->
            if (error.isNotEmpty()) {
                showToast(error, Toast.LENGTH_LONG)
            }
        }
    }

    private fun createNewRoom() {
        binding.btnCreateRoom.isEnabled = false
        val roomId = chatViewModel.createRoom()

        // Show room code to user
        MaterialAlertDialogBuilder(this)
            .setTitle("Room Created")
            .setMessage("Your room code is: $roomId

Share this code with others to let them join your room.")
            .setPositiveButton("Continue") { _, _ ->
                // Room creation is handled by ViewModel observer
            }
            .setNegativeButton("Copy Code") { _, _ ->
                copyToClipboard(roomId)
            }
            .setCancelable(false)
            .show()
    }

    private fun showJoinRoomDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Enter room code"
        input.filters = arrayOf(android.text.InputFilter.LengthFilter(Constants.ROOM_CODE_LENGTH))
        input.inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS

        MaterialAlertDialogBuilder(this)
            .setTitle("Join Room")
            .setMessage("Enter the 6-character room code:")
            .setView(input)
            .setPositiveButton("Join") { _, _ ->
                val roomCode = input.text.toString().trim().uppercase()
                if (roomCode.length == Constants.ROOM_CODE_LENGTH) {
                    joinRoom(roomCode)
                } else {
                    showToast("Please enter a valid 6-character room code")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun joinRoom(roomCode: String) {
        if (roomCode.length == Constants.ROOM_CODE_LENGTH) {
            startChatActivity(roomCode, false)
        } else {
            showToast("Invalid room code")
        }
    }

    private fun scanQRCode() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan QR code to join room")
            setCameraId(0)
            setBeepEnabled(false)
            setBarcodeImageEnabled(true)
            setOrientationLocked(false)
        }
        barcodeLauncher.launch(options)
    }

    private fun startChatActivity(roomId: String, isCaller: Boolean) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(Constants.EXTRA_ROOM_ID, roomId)
            putExtra(Constants.EXTRA_IS_CALLER, isCaller)
        }
        startActivity(intent)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Room Code", text)
        clipboard.setPrimaryClip(clip)
        showToast("Room code copied to clipboard")
    }

    override fun onResume() {
        super.onResume()
        // Re-enable create room button
        binding.btnCreateRoom.isEnabled = true
    }
}