package com.yt8492.bluetoothtransfer.send

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.yt8492.bluetoothtransfer.Constants
import com.yt8492.bluetoothtransfer.R
import com.yt8492.bluetoothtransfer.databinding.ActivitySendBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer

class SendActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendBinding

    private val device by lazy {
        intent.getParcelableExtra<BluetoothDevice>(KEY_DEVICE)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val device = device ?: return@registerForActivityResult
        lifecycleScope.launchWhenStarted {
            withContext(Dispatchers.IO) {
                device.createRfcommSocketToServiceRecord(Constants.APP_UUID).use { socket ->
                    if (socket.isConnected) return@use
                    socket.connect()
                    val outputStream = socket.outputStream
                    val inputStream = contentResolver.openInputStream(uri)!!
                    val size = inputStream.available()
                    outputStream.write(ByteBuffer.allocate(4).putInt(size).array())
                    for (i in 0 until size) {
                        try {
                            outputStream.write(inputStream.read())
                        } catch (e: IOException) {
                            Log.d("hogehoge", "i: $i")
                            throw e
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_send)
        binding.lifecycleOwner = this
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE).takeIf {
            it != PackageManager.PERMISSION_GRANTED
        }?.let {
            val launcher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (!granted) {
                        AlertDialog.Builder(this)
                            .setMessage("ストレージへのアクセスを許可してください")
                            .setPositiveButton("OK") { _, _ ->
                                finish()
                            }
                            .create()
                            .show()
                    }
                }
            launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        binding.sendImageButton.setOnClickListener {
            pickImage()
        }
    }

    private fun pickImage() {
        lifecycleScope.launchWhenStarted {
            launcher.launch("image/*")
        }
    }

    companion object {
        const val KEY_DEVICE = "device"
    }
}