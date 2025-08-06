package com.cynosure.fatiguefree

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cynosure.fatiguefree.detection.CalibrationManager
import com.cynosure.fatiguefree.detection.FatigueDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var fatigueDetector: FatigueDetector? = null
    private var calibrationManager: CalibrationManager? = null

    // 如果要切换到 IP 摄像头模式，这里改为 IP 地址
    private val useIpCamera = false
    private val ipCamUrl = "http://192.168.38.5:8080/video"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startApp()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun startApp() {
        CoroutineScope(Dispatchers.Main).launch {
            calibrationManager = CalibrationManager(this@MainActivity, durationSec = 30)
            val profile = calibrationManager!!.loadOrRunCalibration {
                // 这里提供帧捕获逻辑（例如从 CameraX 获取单帧 Bitmap）
                // 暂时返回空的 Bitmap，占位
                // TODO: 实现单帧捕获
                throw NotImplementedError("Frame provider not implemented for calibration.")
            }

            Log.d("Calibration", "User profile: $profile")

            fatigueDetector = FatigueDetector(this@MainActivity)

            if (useIpCamera) {
                startIpCameraStream(ipCamUrl)
            } else {
                startLocalCamera()
            }
        }
    }

    private fun startLocalCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val analysisUseCase = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA, analysisUseCase
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startIpCameraStream(url: String) {
        // TODO: 用 OpenCV VideoCapture 或 ExoPlayer 从 IP 摄像头读取帧
        // 然后调用 fatigueDetector?.processFrame(bitmap)
        throw NotImplementedError("IP camera mode not yet implemented.")
    }

    private fun processImage(imageProxy: ImageProxy) {
        // TODO: 将 imageProxy 转换为 Bitmap 再传给 fatigueDetector
        // val bitmap = imageProxy.toBitmap()
        // val alerts = fatigueDetector?.processFrame(bitmap)
        imageProxy.close()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startApp()
        } else {
            Log.e("Permissions", "Camera permission denied.")
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        fatigueDetector?.release()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
