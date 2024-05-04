package mo.edu.kaoyip.kyrobot.futurecampus.view.activity

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mo.edu.kaoyip.kyrobot.futurecampus.R
import mo.edu.kaoyip.kyrobot.futurecampus.utils.MyCountDownTimer
import mo.edu.kaoyip.kyrobot.futurecampus.utils.network.NetworkMessage
import mo.edu.kaoyip.kyrobot.futurecampus.utils.network.PortListener
import mo.edu.kaoyip.kyrobot.futurecampus.utils.network.ScanDeviceTool
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private var DetermineMasterIsFound: Boolean = false

    var dialog: Dialog? = null

    var agreementOfPrivacy: Boolean = false

    var NetworkDetectionFailureThread: Job? = null

    var mTv_message: TextView? = null
    var mTv_textLocalIp: TextView? = null
    var mIv_CameraView: ImageView? = null

    private var mPermanentBackgroundOperation: Thread? = null
    var mPermanentBackgroundOperation_State: Boolean? = true

//    var mMastersIp: String? = null

    var mPortListener: PortListener = PortListener(14514)

    private var localIp: String by Delegates.observable(""){property, oldValue, newValue ->
        runOnUiThread(Thread{
            mTv_textLocalIp!!.text = "本机IP地址: " +  newValue
        })

    }


    // OpenCV 人脸检测
    private var openCvCameraView: CameraBridgeViewBase? = null
    private var cascadeClassifier: CascadeClassifier? = null
    private var mRgba: Mat? = null
    private var mOrigenalRgbaImage: Mat? = null
    private var mGray: Mat? = null
    private var absoluteFaceSize = 0

    private var bmap: Bitmap? = null

    private var numberOfFace: Int = 0
        set(value){
            if (field != value) {
                Log.i(TAG, "检测到人脸数量: " + value)
                field = value
            }
        }

    var isFaceDataPass: Boolean = false

    private var isViewStop: Boolean = false


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ScanDeviceTool.scan()?.let { Log.i(TAG, it.toString()) }

        NetworkDetectionFailureThread = GlobalScope.launch(context = Dispatchers.IO) {
            launch{
                delay(1 * 60 * 1000)

                // TODO: 开启摄像头录影, 等待到成功连接网络，然后上传视频
            }

        }

        mIv_CameraView = findViewById<ImageView>(R.id.Iv_CameraView)


        mTv_message = findViewById<TextView>(R.id.tv_information)
        mTv_textLocalIp = findViewById<TextView>(R.id.tv_localIp)
        mTv_textLocalIp!!.setOnClickListener {
            val intent = Intent(this, NoteActivity::class.java)
            startActivity(intent)
        }

        /*mPermanentBackgroundOperation = Thread{
            while (mPermanentBackgroundOperation_State!!){




                Thread.sleep(1000)
            }
        }
        mPermanentBackgroundOperation!!.start()*/


        // 加载OpenCV库
        if (!OpenCVLoader.initLocal()) {
            Log.e(TAG, "Unable to load OpenCV");
        } else {
            Log.i(TAG, "OpenCV loaded");
        }

        // 获取摄像头支持的分辨率
        val mCamera: Camera = Camera.open(CameraInfo.CAMERA_FACING_FRONT)
        val mCamSizeList = mCamera.getParameters().getSupportedPreviewSizes()
        mCamera.release()

        openCvCameraView = findViewById(R.id.JavaCameraViewOpencv);
        openCvCameraView!!.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT)
        openCvCameraView!!.visibility = SurfaceView.VISIBLE
        openCvCameraView!!.setCvCameraViewListener(this)
        openCvCameraView!!.setCameraPermissionGranted()
        // openCvCameraView!!.enableView()


        openCvCameraView!!.setMaxFrameSize(mCamSizeList[0].width, mCamSizeList[0].height)

        val permissions = arrayOf<String>(
            Manifest.permission.CAMERA
        )
        requestPermissions(permissions, 0)



        init()

//        countdownCaptureAndSend()


    }

    companion object{
        private const val TAG = "MainActivity"
        var mMastersIp: String? = null
        var userID: String? = null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun init(){
        mPortListener = PortListener(14514)
        mPortListener.start()

        Thread{
            while (true){

                localIp = ScanDeviceTool.locAddress

                if (mPortListener.result.message != ""){
                    Log.i(TAG, "接收到来自端口 14514 的消息: " + mPortListener.result.message)


                    if (mPortListener.result.message == "I am the master"){
                        DetermineMasterIsFound = true
                        Log.i(TAG, "找到主机")

                        mMastersIp = mPortListener.result.ip

                        mPortListener.stopListening()

                        break
                    }

                    mPortListener.result = NetworkMessage("", "")


                }
            }
        }.start() //  该线程是判断是否有找到主控

        Thread{
            while (true){
                if (DetermineMasterIsFound && agreementOfPrivacy){

                    if (!mTv_message!!.hasOnClickListeners()) {
                        Log.i(TAG, "mTv_message.hasOnLongClickListeners()")
                        // 设置 登录请按这
                        runOnUiThread {
                            mTv_message!!.text = "登录请按这"
                            mTv_message!!.setOnClickListener {
                                Log.i(TAG, "点击了登录")
                                countdownCaptureAndSend()
                            }
                        }

                    }

                    if (isFaceDataPass) {
                        // NetworkDetectionFailureThread!!.cancel()
                        val intent = Intent(this, UsedByStudentMainActivity::class.java)
                        startActivity(intent)
                        break
                    }
                }
            }
        }.start() //  该线程是判断是否有找到主控和是否同意隐私协议还有人脸，如果都满足则跳转到 UsedByStudentMainActivity

        showPrivacy()
    }

    // 显示倒数321，然后拍照，通过网络发送图片，等待返回结果
    //TODO
    @OptIn(ExperimentalEncodingApi::class)
    private fun countdownCaptureAndSend(){
        mTv_message!!.text = " 准备登录...请看摄像头"

        initializeOpenCVDependencies()


        val countDownTimer = object: MyCountDownTimer(4000, 1000){
            override fun onTick(millisUntilFinished: Long) {
                // 每次倒计时更新时调用
                // 更新UI显示
                mTv_message!!.text = "请看摄像头，面部获取倒计时: " + millisUntilFinished / 1000
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onFinish() {
                // runOnUiThread{ openCvCameraView!!.disableView()}

                // 倒计时结束时调用
                // 更新UI显示
                mTv_message!!.text = "登录检测ing... 请稍等"
                // 拍照
                // openCvCameraView!!.takePicture()
                isViewStop = true
                if (numberOfFace == 0){
                    mTv_message!!.text = "未检测到人脸，请重试"
                    /*runOnUiThread{

                    }*/
                    Thread{
                        runOnUiThread{ mTv_message!!.text = "未检测到人脸，请重试，将在3秒后重启" }
                        Thread.sleep(3000)
                        val intent = baseContext.packageManager.getLaunchIntentForPackage(
                            baseContext.packageName
                        )
                        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        this@MainActivity.finish()
                    }.start()
                    return
                }

                // TODO: 发送图片
                // mOrigenalRgbaImage
                // resize(mRgba, mRgba, Size(mRgba!!.width().toDouble(), mRgba!!.height().toDouble()))

                var bitpg = matToBitmap(mOrigenalRgbaImage!!)

                val directoryPath = this@MainActivity.filesDir.absolutePath + "/Images/"
                val directory = File(directoryPath)

                // 检查目录是否存在，如果不存在，则创建目录
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val filePath = directoryPath + "faceDetectImg.jpg"
                val outputStream = FileOutputStream(filePath)
                bitpg.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                val nomediaFilePath = "$directoryPath.nomedia"
                val nomediaFile = File(nomediaFilePath)

                // 检查.nomedia文件是否存在，如果不存在，则创建.nomedia文件
                if (!nomediaFile.exists()) {
                    nomediaFile.createNewFile()
                }

                Thread{
                    val uploadUrl: String = "http://${mMastersIp!!.split(":")[0]}:14515/faceDectection"
                    val okHttpClient = OkHttpClient()
                    Log.d("imagePath", filePath)
                    val file: File = File(filePath)
                    val image = RequestBody.create("image/jpg".toMediaTypeOrNull(), file)
                    val requestBody: RequestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", filePath, image)
                        .build()
                    val request: Request = Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    var jsonObject: JSONObject? = null
                    try {
                        jsonObject = JSONObject(response.body!!.string())
                    }catch (e: JSONException){
                        Log.e(TAG, "jsonObject: " + e.message)
                        Log.e(TAG, "ERRRR: " + response.body!!)

                        // return@Thread
                    }

                    Log.i(TAG, "stateCode: " + response.code)
                    Log.i(TAG, "jsonObject: " + jsonObject)
                    Log.i(TAG, "jsonObject: " + jsonObject!!.getString("result"))

                    try{
                        if (jsonObject!!.getString("result") == "sTu"){
                            isFaceDataPass = true
                            userID = jsonObject.getString("userId")
                            Log.i(TAG, "人脸数据通过")
                            return@Thread
                        }else{
                            Log.i(TAG, "人脸数据未通过")
                            runOnUiThread{ mTv_message!!.text = "人脸数据未通过，请重试，将在3秒后重启" }
                            Thread.sleep(3000)
                            val intent = baseContext.packageManager.getLaunchIntentForPackage(
                                baseContext.packageName
                            )
                            intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                            this@MainActivity.finish()
                        }

                    }catch (e: Exception){}


                }.start()

            }
        }

        countDownTimer.start()



    }



    /**
     * 显示隐私协议
     */
    private fun showPrivacy(){
        val inflate: View =
            LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_privacy_show, null)

        val tv_content = inflate.findViewById<View>(R.id.tv_content) as TextView
        tv_content.setText("一、 蒐集個人資料之目的\n本服務為執行資源利用與服務推廣相關業務所需，蒐集您的個人資料。\n二、 蒐集個人資料之類別\n本服務因執行業務蒐集您的個人資料包括中文姓名、公司名稱、連絡電話與電子郵件地址，詳如電子郵件訂閱表，電子資源廠商將使用 cookies 進行各項網路資源服務之管理及記錄，包括蒐集 IP 位址、瀏覽網頁、使用檔案及時間等軌跡資料。\n三、 個人資料利用之期間、地區、對象與方式\n本服務蒐集之存續期間或因執行業務所需保存期間內，得合理利用您的個人資料，利用地區不限；本服務利用您的個人資料於蒐集目的宣告之各項業務執行，包括因業務執行所必須之各項聯繫與通知；本司利用各項網路資源服務使用紀錄，進行總體流量、使用行為研究及加值應用，以提昇網站服務品質，不針對個別使用者分析。\n四、 個人資料之提供\n您可自由選擇是否提供相關個人資料，惟若拒絕提供個人資料，本司將無法提供相關服務，請依各項服務需求提供您本人正確、最新及完整的個人資料，若您的個人資料有任何異動，請主動向本司申請更正，若您提供錯誤、過時、不完整或具誤導性的資料，而損及您的相關權益，本司將不負相關賠償責任。")
        dialog = AlertDialog.Builder(this@MainActivity)
            .setView(inflate)
            .setCancelable(false)
            .show()
        // 通过WindowManager获取
        // 通过WindowManager获取
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        val params = dialog!!.window!!.attributes
        params.width = dm.widthPixels * 4 / 5
        params.height = dm.heightPixels * 1 / 2
        dialog!!.window!!.setAttributes(params)
        dialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)


    }


    /**
     * 点击按钮，同意或者不同意隐私协议
     */
    fun onBtnClickDialog(view: View){
        if (view.id == R.id.btn_agree){
            agreementOfPrivacy = true
            dialog!!.dismiss()
        }else if(view.id == R.id.btn_disagree){
            finish()
        }

    }

    private fun initializeOpenCVDependencies() {
        try {
            val `is` = getResources().openRawResource(R.raw.lbpcascade_frontalface)
            val cascadeDir = getDir("cascade", MODE_PRIVATE)
            val mCascadeFile = File(cascadeDir, "lbpcascade_frontalface_improved.xml")
            Log.i("OpenCVActivity", "cascadeDir " + cascadeDir)
            val os = FileOutputStream(mCascadeFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (`is`.read(buffer).also { bytesRead = it } != -1) {
                os.write(buffer, 0, bytesRead)
            }
            `is`.close()
            os.close()
            cascadeClassifier = CascadeClassifier(mCascadeFile.absolutePath)
        } catch (e: Exception) {
            Log.e("OpenCVActivity", "Error loading cascade", e)
        }

        openCvCameraView!!.enableView()


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    openCvCameraView!!.setCameraPermissionGranted() // <------ THIS!!!
                } else {
                    // permission denied
                }
                return
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        NetworkDetectionFailureThread!!.cancel()
        mPermanentBackgroundOperation_State = false
        if (mPortListener != null) {
            mPortListener.stopListening()
        }
        openCvCameraView!!.disableView()
    }

    override fun onStop() {
        super.onStop()
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
        }

        if (mPortListener != null) {
            mPortListener.stopListening()
        }
        openCvCameraView!!.disableView()
    }


    /*override fun onPause() {
        super.onPause()
        if (mPortListener.isRunning) {
            mPortListener.stopListening()
        }
    }*/

    override fun onStart() {
        super.onStart()
        if (!mPortListener.isRunning) {
            mPortListener = PortListener(14514)
            mPortListener.start()
        }
        if (dialog != null && !dialog!!.isShowing) {
            dialog!!.show()
        }

    }

    override fun onResume() {
        super.onResume()

    }


    protected fun getCameraViewList(): List<CameraBridgeViewBase> {
        return listOf<CameraBridgeViewBase>(openCvCameraView!!)
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
//        grayscaleImage = Mat(attr.height, attr.width, CvType.CV_8UC4)
        mGray = Mat(width, height, CvType.CV_8UC4)
//        absoluteFaceSize = (attr.height * 0.2).toInt()
        absoluteFaceSize = (width * 0.2).toInt()


    }

    override fun onCameraViewStopped() {
        mGray!!.release()
    }

    var isUsedCameraFrame = false
    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat? {

        mRgba = inputFrame!!.rgba()
        mGray = inputFrame.gray()
        if (mRgba!!.width() <= 0){ return mRgba }

        val faces = MatOfRect()
        isUsedCameraFrame = true
        Core.flip(mRgba, mRgba, 1);
        Core.flip(mGray, mGray, 1);
        mOrigenalRgbaImage = mRgba!!.clone()
        if (this.getResources().configuration.orientation == Configuration.ORIENTATION_PORTRAIT){

            Core.rotate(mRgba, mRgba, Core.ROTATE_90_CLOCKWISE)
            Core.rotate(mGray, mGray, Core.ROTATE_90_CLOCKWISE)
            mIv_CameraView!!.rotation = 90f
        }else{
            mIv_CameraView!!.rotation = 0f
        }




        if (cascadeClassifier != null) {
            cascadeClassifier!!.detectMultiScale(
                mGray, faces, 1.1, 3, 2,
                Size(absoluteFaceSize.toDouble(), absoluteFaceSize.toDouble()), Size()
            )
        }
        val facesArray: Array<Rect> = faces.toArray()
        for (i in facesArray.indices) {
            Imgproc.rectangle(
                mRgba,
                facesArray[i].tl(),
                facesArray[i].br(),
                Scalar(0.0, 255.0, 0.0, 255.0),
                3)
        }
        // if (facesArray.isNotEmpty()){ Log.i(TAG, "检测到人脸数量: " + facesArray.size)}

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Core.rotate(mRgba, mRgba, Core.ROTATE_90_COUNTERCLOCKWISE);
        }
        numberOfFace = facesArray.size
        runOnUiThread {
            if (mRgba!!.width() <= 0){ return@runOnUiThread }
            bmap = Bitmap.createBitmap(
                mRgba!!.width(),
                mRgba!!.height(),
                Bitmap.Config.ARGB_8888
            )
            // resize(mRgba, mRgba, Size(mRgba!!.width().toDouble(), mRgba!!.height().toDouble()))

            try {
                Utils.matToBitmap(mRgba, bmap)
            }catch (e: Exception) {
                // Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.message)
            }


            if (!isViewStop){ mIv_CameraView!!.setImageBitmap(bmap)}
            isUsedCameraFrame = false
        }
        while(isUsedCameraFrame){ Thread.sleep(1) }


        return mRgba
    }


    // 将Mat转换为Bitmap
    fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }


}