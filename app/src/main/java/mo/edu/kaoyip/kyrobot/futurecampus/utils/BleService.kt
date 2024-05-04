package mo.edu.kaoyip.kyrobot.futurecampus.utils

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.text.TextUtils
import java.util.UUID


@SuppressLint("MissingPermission")
class BleService : Service() {

    private val TAG = BleService::class.java.simpleName
    private var mBluetoothGatt: BluetoothGatt? = null

    // 蓝牙连接状态
    private var mConnectionState = 0
    // 蓝牙连接已断开
    private val STATE_DISCONNECTED = 0
    // 蓝牙正在连接
    private val STATE_CONNECTING = 1
    // 蓝牙已连接
    private val STATE_CONNECTED = 2

    // 蓝牙已连接
    companion object {
        const val ACTION_GATT_CONNECTED = "mo.edu.kaoyip.kyrobot.futurecampus.utils.ACTION_GATT_CONNECTED"
        // 蓝牙已断开
        const val ACTION_GATT_DISCONNECTED = "mo.edu.kaoyip.kyrobot.futurecampus.utils.ACTION_GATT_DISCONNECTED"
        // 发现GATT服务
        const val ACTION_GATT_SERVICES_DISCOVERED = "mo.edu.kaoyip.kyrobot.futurecampus.utils.ACTION_GATT_SERVICES_DISCOVERED"
        // 收到蓝牙数据
        const val ACTION_DATA_AVAILABLE = "mo.edu.kaoyip.kyrobot.futurecampus.utils.ACTION_DATA_AVAILABLE"
        // 连接失败
        const val ACTION_CONNECTING_FAIL = "mo.edu.kaoyip.kyrobot.futurecampus.utils.ACTION_CONNECTING_FAIL"
        // 蓝牙数据
        const val EXTRA_DATA = "mo.edu.kaoyip.kyrobot.futurecampus.utils.EXTRA_DATA"
    }

    // 服务标识
    private val SERVICE_UUID = UUID.fromString("0000ace0-0000-1000-8000-00805f9b34fb")
    // 特征标识（读取数据）
    private val CHARACTERISTIC_READ_UUID = UUID.fromString("0000ace0-0001-1000-8000-00805f9b34fb")
    // 特征标识（发送数据）
    private val CHARACTERISTIC_WRITE_UUID = UUID.fromString("0000ace0-0003-1000-8000-00805f9b34fb")
    // 描述标识
    private val DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // 服务相关
    private val mBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BleService {
            return this@BleService
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        release()
        return super.onUnbind(intent)
    }

    /**
     * 蓝牙操作回调
     * 蓝牙连接状态才会回调
     */
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 蓝牙已连接
                mConnectionState = STATE_CONNECTED
                sendBleBroadcast(ACTION_GATT_CONNECTED)
                // 搜索GATT服务
                mBluetoothGatt?.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 蓝牙已断开连接
                mConnectionState = STATE_DISCONNECTED
                sendBleBroadcast(ACTION_GATT_DISCONNECTED)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            // 发现GATT服务
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setBleNotification()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            // 收到数据
            sendBleBroadcast(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    /**
     * 发送通知
     *
     * @param action 广播Action
     */
    private fun sendBleBroadcast(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    /**
     * 发送通知
     *
     * @param action 广播Action
     * @param characteristic 特征值
     */
    private fun sendBleBroadcast(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent: Intent = Intent(action)
        if (CHARACTERISTIC_READ_UUID == characteristic.uuid) {
            intent.putExtra(EXTRA_DATA, characteristic.value)
        }
        sendBroadcast(intent)
    }

    /**
     * 蓝牙连接
     *
     * @param bluetoothAdapter BluetoothAdapter
     * @param address          设备mac地址
     * @return true：成功 false：
     */
    fun connect(bluetoothAdapter: BluetoothAdapter?, address: String?): Boolean {
        if (bluetoothAdapter == null || TextUtils.isEmpty(address)) {
            return false
        }
        val device = bluetoothAdapter.getRemoteDevice(address) ?: return false
        println(device)
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        mConnectionState = STATE_CONNECTING
        return true
    }

    /**
     * 断开蓝牙设备连接
     */
    fun disconnect() {
        mBluetoothGatt?.disconnect()
    }

    /**
     * 释放资源
     */
    fun release() {
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    /**
     * 设置特征通知
     */
    private fun setBleNotification() {
        val gattService = mBluetoothGatt?.getService(SERVICE_UUID)
        val characteristic = gattService?.getCharacteristic(CHARACTERISTIC_READ_UUID)
        mBluetoothGatt?.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic?.getDescriptor(DESCRIPTOR_UUID)
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        mBluetoothGatt?.writeDescriptor(descriptor)
    }

    /**
     * 发送数据
     *
     * @param data 发送的数据
     */
    fun sendData(data: ByteArray) {
        val gattService = mBluetoothGatt?.getService(SERVICE_UUID)
        val characteristic = gattService?.getCharacteristic(CHARACTERISTIC_WRITE_UUID)
        characteristic?.value = data
        mBluetoothGatt?.writeCharacteristic(characteristic)
    }
}