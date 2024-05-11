package mo.edu.kaoyip.kyrobot.futurecampus.utils

import android.annotation.SuppressLint
import java.util.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import java.io.IOException

@SuppressLint("MissingPermission")
class BluetoothManager(private val deviceAddress: String, private val onDataReceived: (value: Int) -> Unit) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val data: MutableList<Int> = mutableListOf()
    private var sendThread: Thread? = null
    private var receiveThread: Thread? = null
    private var socket: BluetoothSocket? = null

    private val handler = Handler(Looper.getMainLooper())

    private var isMoMove:Boolean = true
    private var isSLMove:Boolean = true
    private var temValse:Int = 0

    fun send(value: Int) { // stop 72 79 maga改，如果收到伺服马达那就停止步进电机，如果收到步进电机那就停伺服马达
        if (value == -10086){
            synchronized(data) {
                data.clear()
            }
            return
        }
        if (temValse == value){
            return
        }
        temValse = value


        if (value == 80){
            synchronized(data) {
                data.clear()
                data.add(value)

            }
            isMoMove = false
            isSLMove = false
            return
        }

        if (value == 72){
            if (!isMoMove){
                return
            }
            isMoMove = false
            synchronized(data) {
                data.clear()
                data.add(value)
            }
            return
        }
        if (value == 79){
            if (!isSLMove){
                return
            }
            isSLMove = false
            synchronized(data) {
                data.clear()
                data.add(value)

            }
            return
        }
        if (value == 70 || value == 71){
            isMoMove = true
        }
        if (value == 77 || value == 78){
            isSLMove = true
        }

        synchronized(data) {
            data.add(value)
        }

    }

    fun startSending() {
        if (sendThread == null) {
            sendThread = Thread {
                socket = createBluetoothSocket(deviceAddress)

                try {
                    socket?.connect()

                    val outputStream = socket?.outputStream

                    while (true) {
                        var value: Int? = null
                        synchronized(data) {
                            if (data.isNotEmpty()) {
                                value = data.removeAt(0)
                            }
                        }

                        value?.let {
                            val dataStr = it.toString()
                            outputStream?.write(dataStr.toByteArray())
                            outputStream?.flush()
                        }

                        // 调整等待时间，以控制数据发送的速率
                        Thread.sleep(100)
                    }
                } catch (e: IOException) {
                    // e.printStackTrace()
                } catch (e: InterruptedException) {
                    // e.printStackTrace()
                } finally {
                    socket?.close()
                }
            }

            sendThread?.start()
        }
    }


    fun startReceiving() {
        if (receiveThread == null) {
            receiveThread = Thread {
                socket = createBluetoothSocket(deviceAddress)

                try {
                    socket?.connect()

                    val inputStream = socket?.inputStream
                    val buffer = ByteArray(1024)
                    var bytesRead: Int

                    while (true) {
                        bytesRead = inputStream?.read(buffer) ?: -1
                        if (bytesRead != -1) {
//                            val dataStr = String(buffer, 0, bytesRead)
//                            val value = dataStr.toIntOrNull()
                            val value = buffer[0].toInt()
                            value.let {
                                handler.post { onDataReceived(it) }
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    socket?.close()
                }
            }

            receiveThread?.start()
        }
    }

    fun stopSending() {
        sendThread?.interrupt()
        sendThread = null
        socket?.close()
    }

    fun stopReceiving() {
        receiveThread?.interrupt()
        receiveThread = null
        socket?.close()
    }

    private fun createBluetoothSocket(deviceAddress: String): BluetoothSocket? {
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        return device?.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
    }
}