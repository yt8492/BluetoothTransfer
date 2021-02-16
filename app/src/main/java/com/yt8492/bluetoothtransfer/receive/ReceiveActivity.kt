package com.yt8492.bluetoothtransfer.receive

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.yt8492.bluetoothtransfer.Constants
import com.yt8492.bluetoothtransfer.R
import com.yt8492.bluetoothtransfer.databinding.ActivityReceiveBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class ReceiveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiveBinding
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_receive)
        binding.lifecycleOwner = this
        binding.discoverableButton.setOnClickListener {
            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(discoverableIntent)
        }
        binding.receiveButton.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                val dataBuf = withContext(Dispatchers.IO) {
                    val serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("hoge", Constants.APP_UUID)
                    val socket = serverSocket.accept()
                    val inputStream = socket.inputStream
                    val buf = ByteArray(4)
                    inputStream.read(buf)
                    val size = ByteBuffer.wrap(buf).int
                    Log.d("hogehoge", "size: $size")
                    val fileBuf = ByteArray(size)
                    var offset = 0
                    while (true) {
                        val readSize = inputStream.read(fileBuf, offset, size - offset)
                        if (readSize == 0) {
                            break
                        }
                        offset += readSize
                    }
                    socket.close()
                    fileBuf
                }
                val bitmap = BitmapFactory.decodeByteArray(dataBuf, 0, dataBuf.size)
                binding.receivedImageView.setImageBitmap(bitmap)
            }
        }
    }
}
