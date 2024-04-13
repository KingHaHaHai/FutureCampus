package mo.edu.kaoyip.kyrobot.futurecampus.view.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import mo.edu.kaoyip.kyrobot.futurecampus.R
import mo.edu.kaoyip.kyrobot.futurecampus.utils.DensityUtils.Companion.pxWidthToMm
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.round

class testActivity : AppCompatActivity() {

    // OpenCV
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
        /*set(value){
            if (field != value) {
                Log.i(TAG, "检测到人脸数量: " + value)
                field = value
            }
        }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        cameraTextureView = findViewById(R.id.surface_view_camera)
        initClassifier()
        initCamera()
    }

    // Initialize the face cascade classifier
    private fun initClassifier() {
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
    }

    private fun initCamera() {
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
        /*cameraTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                setupCamera(width, height)
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                // Handle surface size change
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                // Handle surface update
            }
        }*/
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

    private var previewSurface: Surface? = null
    private fun createPreviewSession() {
        try { // previewSurface,
            /*val surfaceTexture = cameraTextureView.surfaceTexture
            surfaceTexture!!.setDefaultBufferSize(cameraTextureView.width, cameraTextureView.height)
            previewSurface = Surface(surfaceTexture)
            val captureRequestBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(previewSurface!!)
            captureRequestBuilder.addTarget(imageReader.surface)

            cameraDevice.createCaptureSession(
                Arrays.asList(previewSurface,imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        captureRequestBuilder.set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        )
                        /*captureSession.setRepeatingRequest(
                            captureRequestBuilder.build(),
                            null,
                            backgroundHandler
                        )*/
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure camera capture session")
                    }
                },
                backgroundHandler
            )*/



            // 相机打开成功后，创建相机会话
            val surfaces = listOf(imageReader.surface)
            cameraDevice.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    // 配置相机会话
                    try {
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
            val distance = (focalLength * 14)/(face.width.toDouble()) * 100 / 5 * 3// face.width.toDouble()

            Log.i(TAG, "${focalLength.toDouble()},${face.width.toDouble()},Distance: $distance cm")
            // 在图像上绘制人脸边界框和距离信息
//            Imgproc.rectangle(image, face.tl(), face.br(), Scalar(0.0, 255.0, 0.0), 2)
            Imgproc.putText(matRgb, "Distance: ${distance} cm", Point(face.x.toDouble(), face.y.toDouble() - 10),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, Scalar(0.0, 255.0, 0.0), 2)
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
        private const val TAG = "TestActivity"
        private const val REQUEST_CAMERA_PERMISSION = 200
        private const val absoluteFaceSize = 0
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        /*if (cameraTextureView.isAvailable) {
            setupCamera(cameraTextureView.width, cameraTextureView.height)
            openCamera()
        } else {
            cameraTextureView.surfaceTextureListener =
                object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surfaceTexture: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        setupCamera(width, height)
                        openCamera()
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surfaceTexture: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        // Handle surface size change
                    }

                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                        return false
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                        // Handle surface update
                    }
                }
        }*/
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