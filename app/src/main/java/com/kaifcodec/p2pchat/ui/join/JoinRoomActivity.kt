package com.kaifcodec.p2pchat.ui.join

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.kaifcodec.p2pchat.databinding.ActivityJoinRoomBinding
import com.kaifcodec.p2pchat.ui.chat.ChatActivity
import com.kaifcodec.p2pchat.utils.Constants
import com.kaifcodec.p2pchat.utils.showToast

class JoinRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJoinRoomBinding

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val roomCode = result.contents
            if (roomCode.length == Constants.ROOM_CODE_LENGTH) {
                binding.editTextRoomCode.setText(roomCode)
                joinRoom(roomCode)
            } else {
                showToast("Invalid QR code")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Join Room"
        }

        // Setup room code input
        binding.editTextRoomCode.addTextChangedListener { text ->
            val roomCode = text.toString().trim()
            binding.buttonJoin.isEnabled = roomCode.length == Constants.ROOM_CODE_LENGTH

            // Auto-format input to uppercase
            if (text.toString() != roomCode.uppercase()) {
                binding.editTextRoomCode.setText(roomCode.uppercase())
                binding.editTextRoomCode.setSelection(roomCode.length)
            }
        }

        binding.buttonJoin.setOnClickListener {
            val roomCode = binding.editTextRoomCode.text.toString().trim()
            joinRoom(roomCode)
        }

        binding.buttonScanQr.setOnClickListener {
            scanQRCode()
        }

        // Initially disable join button
        binding.buttonJoin.isEnabled = false
    }

    private fun joinRoom(roomCode: String) {
        if (roomCode.length == Constants.ROOM_CODE_LENGTH) {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra(Constants.EXTRA_ROOM_ID, roomCode)
                putExtra(Constants.EXTRA_IS_CALLER, false)
            }
            startActivity(intent)
            finish()
        } else {
            showToast("Please enter a valid 6-character room code")
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}