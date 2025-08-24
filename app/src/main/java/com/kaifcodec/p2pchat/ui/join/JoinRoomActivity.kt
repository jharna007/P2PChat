package com.kaifcodec.p2pchat.ui.join

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentIntegrator
import com.journeyapps.barcodescanner.ScanOptions
import com.kaifcodec.p2pchat.R
import com.kaifcodec.p2pchat.databinding.ActivityJoinRoomBinding
import com.kaifcodec.p2pchat.ui.chat.ChatActivity
import com.kaifcodec.p2pchat.utils.isValidRoomCode

class JoinRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJoinRoomBinding

    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val roomCode = result.contents
            if (roomCode.isValidRoomCode()) {
                binding.etRoomCode.setText(roomCode)
                joinRoom(roomCode)
            } else {
                Snackbar.make(binding.root, "Invalid QR code format", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Join Room"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupClickListeners() {
        binding.btnJoinRoom.setOnClickListener {
            val roomCode = binding.etRoomCode.text.toString().trim().uppercase()
            if (roomCode.isValidRoomCode()) {
                joinRoom(roomCode)
            } else {
                binding.tilRoomCode.error = "Invalid room code format"
            }
        }

        binding.btnScanQr.setOnClickListener {
            startQrScanner()
        }

        // Clear error when user starts typing
        binding.etRoomCode.setOnFocusChangeListener { _, _ ->
            binding.tilRoomCode.error = null
        }
    }

    private fun startQrScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan room QR code")
            setBeepEnabled(true)
            setBarcodeImageEnabled(true)
            setOrientationLocked(false)
        }
        qrScanLauncher.launch(options)
    }

    private fun joinRoom(roomCode: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_ROOM_CODE, roomCode)
        }
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
