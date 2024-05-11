package mo.edu.kaoyip.kyrobot.futurecampus.view.activity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import mo.edu.kaoyip.kyrobot.futurecampus.R
import mo.edu.kaoyip.kyrobot.futurecampus.utils.BluetoothManager
import mo.edu.kaoyip.kyrobot.futurecampus.utils.network.NetworkMessage
import mo.edu.kaoyip.kyrobot.futurecampus.utils.network.ScanDeviceTool
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.UUID


@RequiresApi(Build.VERSION_CODES.O)
class UsedByStudentMainActivity : AppCompatActivity() {

    private var main_right_drawer_layout: RelativeLayout? = null //右侧滑动栏
    private var drawerbar: ActionBarDrawerToggle? = null
    private var main_root: DrawerLayout? = null

    private var mWebView_main: WebView? = null
    private var mImageView_Gpt_Icon: ImageView? = null
    private var mImageView_note_Icon: ImageView? = null

    private var isAutoControl: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_used_by_student)

        //初始化控件
        initView()
        initUpLoad()

    }

    @SuppressLint("SetJavaScriptEnabled", "RtlHardcoded")
    private fun initView() {
        main_root = findViewById<DrawerLayout>(R.id.root)
        //右边菜单
        main_right_drawer_layout =
            findViewById<RelativeLayout>(R.id.main_right_drawer_layout) // View

        //设置菜单内容之外其他区域的背景色
        main_root!!.setScrimColor(Color.TRANSPARENT)

        val mBtnOpenRightDrawerLayout = findViewById<Button>(R.id.btn_open_right_drawer_layout)
        mBtnOpenRightDrawerLayout.setOnClickListener{
            if (main_root!!.isDrawerOpen(main_right_drawer_layout!!)) {
                main_root!!.closeDrawer(main_right_drawer_layout!!)
            } else {
                main_root!!.openDrawer(main_right_drawer_layout!!)
            }
        }

        mWebView_main = findViewById<WebView>(R.id.web_view_main)
        mWebView_main!!.setWebViewClient(object : WebViewClient() {
            //使用webView打开网页，而不是Android默认浏览器，必须覆盖webView的WebViewClient对象
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                /*return super.shouldOverrideUrlLoading(view, url);*/
                view.loadUrl(url)

                // TODO: 上传url

                Thread{
                    val uploadUrl: String = "http://${MainActivity.mMastersIp!!.split(":")[0]}:14515/api/broUpload"

                    var client = OkHttpClient()
                    // Create a JSON object with the required fields
                    val json = JSONObject()
                    json.put("stdId", MainActivity.userID)
                    json.put("time", getCurrentTime())
                    json.put("url", url)

                    // Create the request body
                    val requestBody: RequestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
                    val request = Request.Builder()
                        .url(uploadUrl) // 设置请求的 URL
                        .post(requestBody)
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            // 请求失败的处理
                            e.printStackTrace()
                        }

                        override fun onResponse(call: Call, response: Response) {
                            // 请求成功的处理
                            val responseData = response.body?.string()
                            Log.d("ChatGptMainActivity", "onResponse: $responseData")
                        }
                    })

                }.start()

                return true
            }
        })
        mWebView_main!!.loadUrl("https://www.google.com/?hl=zh-TW")
        mWebView_main!!.settings.javaScriptEnabled = true


        mImageView_Gpt_Icon = findViewById<ImageView>(R.id.Iv_Gpt_Icon)
        mImageView_Gpt_Icon!!.setOnClickListener {
            // 设置Activity样式
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // 设置Activity位置和边界
            window.setGravity(Gravity.TOP or Gravity.LEFT)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )

            val intent = Intent(this, ChatGptMainActivity::class.java)
            startActivity(intent)

        }
        mImageView_note_Icon = findViewById<ImageView>(R.id.Iv_note_Icon)
        mImageView_note_Icon!!.setOnClickListener {


            val intent = Intent(this, NoteActivity::class.java)
            startActivity(intent)

        }

        var mtv_feedback = findViewById<TextView>(R.id.tv_feedback)
        mtv_feedback.setOnClickListener {
            // 设置Activity样式
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // 设置Activity位置和边界
            window.setGravity(Gravity.TOP or Gravity.LEFT)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )

            val intent = Intent(this, CommentBoxActivity::class.java)
            startActivity(intent)
        }

        var mSwitchControl = findViewById<Switch>(R.id.switchControlButton)

        var mLyOll_control_button = findViewById<LinearLayout>(R.id.ll_control_button)

        mSwitchControl.setOnCheckedChangeListener { _, isChecked ->
            // 在这里处理Switch状态变化的逻辑
            if (isChecked) {
                // Switch被选中
                // 执行相应的操作
                mLyOll_control_button.visibility = View.GONE
                isAutoControl = true
                bleManger?.send(-10086)
            } else {
                // Switch未被选中
                // 执行相应的操作
                mLyOll_control_button.visibility = View.VISIBLE
                isAutoControl = false
                bleManger?.send(-10086)
            }
        }

        // 上下左右按钮
        var mBtnUp = findViewById<Button>(R.id.btnUp)
        var mBtnDown = findViewById<Button>(R.id.btnDown)
        var mBtnLeft = findViewById<Button>(R.id.btnLeft)
        var mBtnRight = findViewById<Button>(R.id.btnRight)
        var mbtnreset = findViewById<Button>(R.id.btnreset)
        mbtnreset.setOnClickListener { bleManger?.send(80) }
        mBtnUp.setOnClickListener{
            // 向前
//            sendBle(4)
            Thread{
                //qian
//                if(socket != null) {
//                    return@Thread
//                }
//                //sendBle("G")
//                sendBle(77)
//                //ting
//                Thread.sleep(100)
//                // ting
//                sendBle(79)
                bleManger?.send(77)
                bleManger?.send(79)

            }.start()
        }
        mBtnDown.setOnClickListener{
            // 向前
//            sendBle(4)
            Thread{
                //qian
//                if(socket != null) {
//                    return@Thread
//                }
//                //sendBle("G")
//                sendBle(78)
//                //ting
//                Thread.sleep(100)
//                // ting
//                sendBle(79)
                bleManger?.send(78)
                bleManger?.send(79)

            }.start()
        }
        mBtnLeft.setOnClickListener{
            // 向前
//            sendBle(4)
            Thread{
                //qian
//                if(socket != null) {
//                    return@Thread
//                }
//                //sendBle("G")
//                sendBle(71)
//                //ting
//                Thread.sleep(100)
//                 sendBle(72)
                // ting
                bleManger?.send(71)
                bleManger?.send(72)

            }.start()
        }
        mBtnRight.setOnClickListener{
            // 向前
//            sendBle(4)
            Thread{
                //qian
                /*if(socket != null) {
                    return@Thread
                }
                //sendBle("G")
                sendBle(70)
                //ting
                Thread.sleep(100)
                // ting
                sendBle(72)*/
                bleManger?.send(70)
                bleManger?.send(72)

            }.start()
        }



        initDistenDectector()
        initBle()

        // TODO: 全局通知。。。

        //悬浮窗权限检查
        // DialogX.globalHoverWindow = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    this,
                    "使用 DialogX.globalHoverWindow 必须开启悬浮窗权限",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivity(intent)
                return
            }
        }

        Thread{
            /*
            import socket
            import os

            def send_text_data(text):
                # 连接服务器
                server_address = ('localhost', 5000)  # 服务器地址和端口号
                client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                client_socket.connect(server_address)

                # 发送数据类型标识（0表示文本）
                client_socket.sendall(b'\x00')

                # 发送文本数据
                client_socket.sendall(text.encode())

                # 关闭连接
                client_socket.close()

            def send_image_data(image_path):
                # 连接服务器
                server_address = ('localhost', 5000)  # 服务器地址和端口号
                client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                client_socket.connect(server_address)

                # 发送数据类型标识（1表示图片）
                client_socket.sendall(b'\x01')

                # 发送图片数据
                with open(image_path, 'rb') as file:
                    while True:
                        data = file.read(1024)
                        if not data:
                            break
                        client_socket.sendall(data)

                # 关闭连接
                client_socket.close()

            # 发送文本数据示例
            text_data = "Hello, Server!"
            send_text_data(text_data)

            # 发送图片数据示例
            image_path = "path/to/image.jpg"
            send_image_data(image_path)
            */



            try {
                var port = 14516
                val bufferSize = 8192 // 缓冲区大小
                val serverSocket = ServerSocket(port)
                println("服务器正在监听端口 $port...")

                while (true) {
                    val clientSocket = serverSocket.accept()
                    println("客户端已连接：${clientSocket.inetAddress.hostAddress}:${clientSocket.port}")

                    val inputStream = clientSocket.getInputStream()
                    val bufferedInputStream = BufferedInputStream(inputStream)

                    val dataType = bufferedInputStream.read() // 读取数据类型（0表示文本，1表示图片）

                    if (dataType == 0) {
                        // 文本数据
                        val inputStreamReader = InputStreamReader(bufferedInputStream)
                        val bufferedReader = BufferedReader(inputStreamReader)

                        val stringBuilder = StringBuilder()
                        var line: String?
                        while (bufferedReader.readLine().also { line = it } != null) {
                            stringBuilder.append(line).append("\n")
                        }

                        val receivedText = stringBuilder.toString()
                        println("接收到的文本：$receivedText")

                        // 处理接收到的文本数据
                        handleReceivedText(receivedText)

                        bufferedReader.close()
                        inputStreamReader.close()
                    } else if (dataType == 1) {
                        // 图片数据
                        val outputFile = File.createTempFile("received", ".jpg")
                        val fileOutputStream = FileOutputStream(outputFile)

                        val buffer = ByteArray(bufferSize)
                        var bytesRead: Int
                        while (bufferedInputStream.read(buffer).also { bytesRead = it } != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead)
                        }

                        fileOutputStream.close()
                        println("数据已接收并保存到文件：${outputFile.absolutePath}")

                        // 处理接收到的图片数据
                        handleReceivedImage(outputFile)
                    }

                    bufferedInputStream.close()
                    inputStream.close()
                    clientSocket.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

    }


    private fun handleReceivedText(text: String) {
        // 处理接收到的文本数据
        println("处理接收到的文本数据：$text")
    }

    private fun handleReceivedImage(imageFile: File) {
        // 处理接收到的图片数据
        println("处理接收到的图片数据：${imageFile.absolutePath}")
    }

    private var upLoadThread: Thread? = null
    private fun initUpLoad(){
        // 距离上传
        upLoadThread = Thread{
            while (true) {
                Thread{
                    if (distanceFace == 0.toDouble() || numberOfFace == 0){
                        return@Thread
                    }
                    Log.i("Use", "distanceFace: $distanceFace")
                    var client = OkHttpClient()
                    val uploadUrl: String = "http://${MainActivity.mMastersIp!!.split(":")[0]}:14515/api/disUpload"

                    // Create a JSON object with the required fields
                    val json = JSONObject()
                    json.put("stdId", MainActivity.userID)
                    json.put("distan", distanceFace)

                    // Create the request body
                    val requestBody: RequestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
                    val request = Request.Builder()
                        .url(uploadUrl) // 设置请求的 URL
                        .post(requestBody)
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            // 请求失败的处理
                            e.printStackTrace()
                        }

                        override fun onResponse(call: Call, response: Response) {
                            // 请求成功的处理
                            val responseData = response.body?.string()
                            Log.d("ChatGptMainActivity", "onResponse: $responseData")
                        }
                    })

                }.start()

                Thread.sleep(10 * 1000)
            }
        }
        upLoadThread!!.start()
    }


    // 蓝牙
    // 00:18:E4:40:00:06

    private var socket: BluetoothSocket? = null
    private var bleManger: BluetoothManager? = null
    @SuppressLint("MissingPermission")
    fun initBle(){

        bleManger = BluetoothManager("00:18:E4:40:00:06") {
            Thread{
                val uploadUrl: String = "http://${MainActivity.mMastersIp!!.split(":")[0]}:14515/api/temUpload"

                var client = OkHttpClient()
                // Create a JSON object with the required fields
                val json = JSONObject()
                json.put("stdId", MainActivity.userID)
                json.put("time", getCurrentTime())
                json.put("temperature", it)

                // Create the request body
                val requestBody: RequestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
                val request = Request.Builder()
                    .url(uploadUrl) // 设置请求的 URL
                    .post(requestBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // 请求失败的处理
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        // 请求成功的处理
                        val responseData = response.body?.string()
                        Log.d("ChatGptMainActivity", "onResponse: $responseData")
                    }
                })

            }.start()

        }

        bleManger!!.startSending()
        bleManger!!.startReceiving()


// 获取默认的蓝牙适配器
        /*val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

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
        registerReceiver(receiver, filter)*/
        /*Thread{
            while (true) {
                try {
                    if(socket != null) {
                        sendBle("G")
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }.start()*/
    }


    /*@SuppressLint("MissingPermission")
    inner class ConnectThread(device: BluetoothDevice, bluetoothAdaptera: BluetoothAdapter) : Thread() {
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

            // 在连接成功后，可以使用InputStream和OutputStream进行数据的接收和发送
            // 例如，通过InputStream接收数据
            try {
                val inputStream = socket!!.inputStream
                val buffer = ByteArray(1024)
                var bytes: Int
                while (inputStream.read(buffer).also { bytes = it } != -1) {
                    val receivedData = buffer[0].toInt()
//                    println(receivedData)
//                    println(receivedData.toInt())
//                    println(buffer)
                    // 处理接收到的数据、

                    // TODO: 在这里处理接收到的数据

                    Thread{
                        val uploadUrl: String = "http://${MainActivity.mMastersIp!!.split(":")[0]}:14515/api/temUpload"

                        var client = OkHttpClient()
                        // Create a JSON object with the required fields
                        val json = JSONObject()
                        json.put("stdId", MainActivity.userID)
                        json.put("time", getCurrentTime())
                        json.put("temperature", receivedData)

                        // Create the request body
                        val requestBody: RequestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
                        val request = Request.Builder()
                            .url(uploadUrl) // 设置请求的 URL
                            .post(requestBody)
                            .build()

                        client.newCall(request).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                // 请求失败的处理
                                e.printStackTrace()
                            }

                            override fun onResponse(call: Call, response: Response) {
                                // 请求成功的处理
                                val responseData = response.body?.string()
                                Log.d("ChatGptMainActivity", "onResponse: $responseData")
                            }
                        })

                    }.start()
                }
            } catch (e: IOException) {
                // e.printStackTrace()
            }
        }

        fun cancel() {
            try {
                socket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }*/

//    private var dataBleSend: Int = -1
//    private fun sendBle(data: Int){
//        // 例如，通过OutputStream发送数据
//        // 例如，通过OutputStream发送数据
//        if (dataBleSend != data){
//            dataBleSend = data
//        }else{
//            return
//        }
//        try {
//            val outputStream = socket!!.outputStream
//            outputStream.write(data)
//            outputStream.flush()
//        } catch (e: IOException) {
//            //e.printStackTrace()
//        } catch(e: NullPointerException){
//            //e.printStackTrace()
//        }
//    }

    private fun getCurrentTime(): String {
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return currentTime.format(formatter)
    }

    // 自动距离调整
    // OpenCV
    private var distanceFace: Double = 0.0
        set(value) {
            if (value < 10.0 || numberOfFace == 0) {
                // sendBle(79)
                return
            }

            if (!isAutoControl){ return }

            field = value
            
            if (value > 45){
                Log.i("平板","向前")
//                sendBle(77)
                bleManger?.send(77)

            }else if (value < 25) {
                Log.i("平板","向后")
//                sendBle(78)
                bleManger?.send(78)
            }else{
//                sendBle(79)
                bleManger?.send(79)
            }
        }

    private var faceCentrePoint: ArrayList<Double>? = null
        set(value){

            if (numberOfFace == 0) {
                // sendBle(72)
                return
            }
            if (!isAutoControl){ return }

            field = value
            var halfHeight = value!![2] / 2

            if (value[1] > halfHeight + 20) {
                Log.i("平板","向下")
//                mBleService!!.sendData("G".toByteArray())
//                sendBle(70)
                bleManger?.send(70)

            }
            else if (value[1] < halfHeight - 20) {
                Log.i("平板","向上")
//                mBleService!!.sendData("H".toByteArray())
//                sendBle(71)
                bleManger?.send(71)
            }
            else{
//                sendBle(72)
                bleManger?.send(72)
            }
        }

    private var cascadeClassifier: CascadeClassifier? = null

    private lateinit var cameraTextureView: SurfaceView
    private lateinit var cameraId: String
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var imageReader: ImageReader
    private var numberOfFace: Int = 0

    fun initDistenDectector() {

        cameraTextureView = findViewById(R.id.surface_view_camera)

        // initClassifier
        try {
            val inputStream = resources.openRawResource(R.raw.lbpcascade_frontalface)
            val cascadeDir = getDir("cascade", MODE_PRIVATE)
            val cascadeFile = File(cascadeDir, "lbpcascade_frontalface_improved.xml")
            val outputStream = FileOutputStream(cascadeFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            outputStream.close()
            cascadeClassifier = CascadeClassifier(cascadeFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // initCamera
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = getBackFacingCameraId() ?: run {
            Toast.makeText(this, "No back-facing camera found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        cameraTextureView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                setupCamera(cameraTextureView.width, cameraTextureView.height)
                openCamera()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // Handle surface size change
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // Handle surface destroyed
            }
        })
    }
    private fun setupCamera(width: Int, height: Int) {
        try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map: StreamConfigurationMap =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            val largestPreviewSize = Collections.max(
                listOf(*map.getOutputSizes(ImageFormat.YUV_420_888)),
                CompareSizesByArea()
            )
            val previewSize = getOptimalSize(
                map.getOutputSizes(SurfaceTexture::class.java),
                width,
                height,
                largestPreviewSize
            )
            imageReader = ImageReader.newInstance(
                previewSize.width,
                previewSize.height,
                ImageFormat.YUV_420_888,
                2
            )
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                processImage(image)
                image?.close()
            }, backgroundHandler)

            val rotation = windowManager.defaultDisplay.rotation
            val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            val swappedDimensions = areDimensionsSwapped(rotation, sensorOrientation)
            val displaySize = Size(width, height)

            // Configure the texture view
            val matrix = android.graphics.Matrix()
            val viewRect = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
            val bufferRect = android.graphics.RectF(
                0f, 0f, previewSize.height.toFloat(),
                previewSize.width.toFloat()
            )
            val centerX = viewRect.centerX()
            val centerY = viewRect.centerY()
            if (swappedDimensions) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
                matrix.setRectToRect(viewRect, bufferRect, android.graphics.Matrix.ScaleToFit.FILL)
                val scale = Math.max(
                    height.toFloat() / previewSize.height,
                    width.toFloat() / previewSize.width
                )
                matrix.postScale(scale, scale, centerX, centerY)
                matrix.postRotate(90 * (rotation - 2).toFloat(), centerX, centerY)
            } else {
                matrix.postRotate(90 * rotation.toFloat(), centerX, centerY)
            }
            // cameraTextureView.setTransform(matrix)

            // Open the camera
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
                return
            }
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice.close()
        }
    }

    private fun createPreviewSession() {
        try {



            // 相机打开成功后，创建相机会话
            val surfaces = listOf(imageReader.surface)
            cameraDevice.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    // 配置相机会话
                    try {
                        println("onConfigured")
                        val captureRequest =
                            session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        captureRequest.addTarget(imageReader.surface)
                        session.setRepeatingRequest(captureRequest.build(), null, null)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }


                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    // 配置相机会话失败
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun processImage(image: Image) {
        val yuvBytes = imageToByteArray(image)
        val matYuv = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        matYuv.put(0, 0, yuvBytes)
        val mirroredMatYuv = Mat()

        Core.flip(matYuv, mirroredMatYuv, 1)

        val matRgb = Mat(image.height, image.width, CvType.CV_8UC3)
        Imgproc.cvtColor(mirroredMatYuv, matRgb, Imgproc.COLOR_YUV2RGB_NV21)
        Imgproc.cvtColor(matRgb, matRgb, Imgproc.COLOR_BGR2RGB)
        // 创建一个与彩色图像大小相同的灰度图像Mat对象
        val matGray = Mat(image.height, image.width, CvType.CV_8UC1)

        // 将彩色图像转换为灰度图像
        Imgproc.cvtColor(mirroredMatYuv, matGray, Imgproc.COLOR_YUV2GRAY_NV21)

        val faces = MatOfRect()
        cascadeClassifier?.detectMultiScale(
            matGray,
            faces,
            1.1,
            10,
            2,
            org.opencv.core.Size(absoluteFaceSize.toDouble(), absoluteFaceSize.toDouble()),
            org.opencv.core.Size()
        )

        // Draw rectangles around detected faces
        for (rect in faces.toArray()) {
//            if (rect.width * rect.height < 300){
//                continue
//            }
            Imgproc.rectangle(
                matRgb,
                org.opencv.core.Point(rect.x.toDouble(), rect.y.toDouble()),
                org.opencv.core.Point(
                    (rect.x + rect.width).toDouble(),
                    (rect.y + rect.height).toDouble()
                ),
                Scalar(255.0, 0.0, 0.0),
                2
            )
        }
        numberOfFace = faces.toArray().size

        // 获取第一个检测到的人脸
        if (numberOfFace != 0) {
            val face = faces.toArray()[0]

            // 计算距离
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val focalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            var focalLength = 0.0f
            if (focalLengths != null && focalLengths.isNotEmpty()) {
                focalLength = focalLengths[0]
//                Log.d(TAG, "焦距：$focalLength")
            }
            val distance = (focalLength * 14)/(face.width.toDouble()) * 100 // face.width.toDouble()

//            Log.i(TAG, "${focalLength.toDouble()},${face.width.toDouble()},Distance: $distance cm")
            distanceFace = distance
            // 在图像上绘制人脸边界框和距离信息
//            Imgproc.rectangle(image, face.tl(), face.br(), Scalar(0.0, 255.0, 0.0), 2)
            Imgproc.putText(matRgb, "Distance: ${distance} cm", Point(face.x.toDouble(), face.y.toDouble() - 10),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, Scalar(0.0, 255.0, 0.0), 2)

            val centerX = (face.x + face.x + face.width) / 2.0
            val centerY = (face.y + face.y + face.height) / 2.0
            var temfaceCentrePoint = ArrayList<Double>()
            temfaceCentrePoint.add(centerX)
            temfaceCentrePoint.add(centerY)
            temfaceCentrePoint.add(image.height.toDouble())
            faceCentrePoint = temfaceCentrePoint
        }


        val bitmap = Bitmap.createBitmap(matRgb.cols(), matRgb.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(matRgb, bitmap)
        // Display the modified bitmap or do further processing
        val holder = cameraTextureView.holder
        runOnUiThread{
            holder.setFixedSize(bitmap.width, bitmap.height)
        }

        val canvas = holder.lockCanvas()
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        holder.unlockCanvasAndPost(canvas)

    }



    private fun imageToByteArray(image: Image): ByteArray {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize);
        return nv21
    }

    private fun getBackFacingCameraId(): String? {
        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
//                if (cameraDirection == CameraCharacteristics.LENS_FACING_BACK) {
                if (cameraDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                    return id
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return null
    }

    private fun areDimensionsSwapped(rotation: Int, sensorOrientation: Int): Boolean {
        var swappedDimensions = false
        when (rotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 ->
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }


            Surface.ROTATION_90, Surface.ROTATION_270 ->
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }

            else -> Log.e(TAG, "Unexpected rotation value")
        }
        return swappedDimensions
    }

    private fun getOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size
    ): Size {
        val bigEnough = ArrayList<Size>()
        val notBigEnough = ArrayList<Size>()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= width && option.height <= height &&
                option.height == option.width * h / w
            ) {
                if (option.width >= width && option.height >= height) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }
        return when {
            bigEnough.isNotEmpty() -> Collections.min(bigEnough, CompareSizesByArea())
            notBigEnough.isNotEmpty() -> Collections.max(notBigEnough, CompareSizesByArea())
            else -> choices[0]
        }
    }

    private inner class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height
            )
        }
    }

    companion object {
        private const val TAG = "UsedByStudentMainActivity"
        private const val REQUEST_CAMERA_PERMISSION = 200
        private const val absoluteFaceSize = 0
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
    }
    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
                return
            }
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
//        mBleService!!.disconnect()
//        socket!!.close()
        super.onDestroy()
    }

    private fun closeCamera() {
        try {
            if(this::captureSession.isInitialized) {
                captureSession.close()
            }
            cameraDevice.close()
            imageReader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



}