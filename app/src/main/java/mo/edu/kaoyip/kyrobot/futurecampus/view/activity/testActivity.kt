package mo.edu.kaoyip.kyrobot.futurecampus.view.activity

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import mo.edu.kaoyip.kyrobot.futurecampus.R
import java.io.IOException
import java.util.*


class testActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)


// 获取默认的蓝牙适配器
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

// 检查设备是否支持蓝牙
        if (bluetoothAdapter == null) {
            // 设备不支持蓝牙
            println(123)
            return
        }

// 检查蓝牙是否已经启用
        if (!bluetoothAdapter.isEnabled) {
            // 如果蓝牙未启用，则可以请求用户启用蓝牙
            // 你可以在这里添加适当的逻辑来请求用户启用蓝牙
            println(2)
            return
        }

        // 根据指定的MAC地址获取BluetoothDevice对象
        val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice("00:18:E4:40:00:06")

        // 检查设备是否存在
        if (device == null) {
            // 设备不存在
            println(3)
            return
        }

        // 连接到蓝牙设备
        /*try {
            println(4)
            val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)

            socket.connect()

            // 连接成功，可以在这里进行蓝牙通信操作

            // 关闭连接
            println("连接成功")

//            socket.close()
        } catch (e: IOException) {
            e.printStackTrace()
            // 连接失败，处理异常情况
        }*/

        // 开始扫描设备
        // 开始扫描设备
        bluetoothAdapter.startDiscovery()
// BroadcastReceiver接收扫描到的设备
// BroadcastReceiver接收扫描到的设备
        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device!!.getName() == "HC-06" || device!!.getName() == "HC-05") {
                        println("找到了")
                        // 连接到HC-06
                        val connectThread = ConnectThread(device, bluetoothAdapter)
                        connectThread.start()
                        // 停止扫描
                        bluetoothAdapter.cancelDiscovery()
                    }
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)


    }


    inner class ConnectThread(device: BluetoothDevice,bluetoothAdaptera: BluetoothAdapter) : Thread() {
        private val socket: BluetoothSocket?
        private val device: BluetoothDevice
        private var bluetoothAdapter: BluetoothAdapter? = null
        init {
            bluetoothAdapter = bluetoothAdaptera
            var tempSocket: BluetoothSocket? = null
            this.device = device
            try {
                tempSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            } catch (e: IOException) {
                e.printStackTrace()
            }
            socket = tempSocket
        }

        override fun run() {
            bluetoothAdapter!!.cancelDiscovery()
            try {
                socket!!.connect()
            } catch (connectException: IOException) {
                try {
                    socket!!.close()
                } catch (closeException: IOException) {
                    closeException.printStackTrace()
                }
                return
            }

            Thread{
                /*while (true) {
                    try {
                        if(socket != null) {
                            try {
                                val outputStream = socket!!.outputStream
                                outputStream.write(123)
                            } catch (e: IOException) {
                                //e.printStackTrace()
                            } catch(e: NullPointerException){
                                //e.printStackTrace()
                            }
                        }
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }*/
            }.start()

            // 在连接成功后，可以使用InputStream和OutputStream进行数据的接收和发送
            // 例如，通过InputStream接收数据
            try {
                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)
                var bytes: Int
                while (inputStream.read(buffer).also { bytes = it } != -1) {
                    val receivedData = String(buffer, 0, bytes)
                    println(receivedData)
                    // 处理接收到的数据

                    val outputStream = socket!!.outputStream
                    outputStream.write(2)
                    outputStream.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }


        }

        fun cancel() {
            try {
                socket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


}

