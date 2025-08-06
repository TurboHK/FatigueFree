package com.cynosure.fatiguefree.detection

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import com.cynosure.fatiguefree.R
import com.cynosure.fatiguefree.detection.FaceMeshProcessor
import com.cynosure.fatiguefree.detection.BlinkDetector
import com.cynosure.fatiguefree.detection.YawnDetector
import com.cynosure.fatiguefree.detection.MotionDetector
import com.cynosure.fatiguefree.metrics.EyeAspectRatio
import com.cynosure.fatiguefree.metrics.MouthAspectRatio
import com.cynosure.fatiguefree.utils.HeadPoseEstimator
import com.cynosure.fatiguefree.utils.Config
import kotlinx.coroutines.*

class FatigueDetector(private val context: Context) {

    private val faceMeshProcessor = FaceMeshProcessor(context)
    private val headPoseEstimator = HeadPoseEstimator()
    private val blinkDetector = BlinkDetector()
    private val yawnDetector = YawnDetector()
    private val pitchDetector = MotionDetector(Config.PITCH_THRESHOLD)
    private val yawDetector = MotionDetector(Config.YAW_THRESHOLD)
    private val rollDetector = MotionDetector(Config.ROLL_THRESHOLD)

    private var pitchBase: Float? = null
    private var lastAlertTime: Long = 0
    private val soundCooldown = 5000L // 5 seconds

    private val mediaPlayer: MediaPlayer by lazy {
        MediaPlayer.create(context, R.raw.alert)
    }

    fun processFrame(bitmap: Bitmap): List<String> {
        val alerts = mutableListOf<String>()
        val face = faceMeshProcessor.detectSingleFace(bitmap) ?: return alerts

        val w = bitmap.width
        val h = bitmap.height

        // EAR
        val ear = (EyeAspectRatio.compute(face.leftEye) + EyeAspectRatio.compute(face.rightEye)) / 2.0
        val (blinkAlert, blinkRate, blinkMessages) = blinkDetector.update(ear)
        alerts.addAll(blinkMessages)
        if (blinkAlert) alerts.add("Fatigue Alert: Prolonged Blink!")
        if (blinkRate > 19) alerts.add("Fatigue Alert: High Blink Rate!")

        // MAR
        val mar = MouthAspectRatio.compute(face.mouth)
        if (yawnDetector.update(mar)) alerts.add("Fatigue Alert: Yawning Detected!")

        // Head Pose
        val pitch = headPoseEstimator.estimatePitch(face) ?: 0f
        val yaw   = headPoseEstimator.estimateYaw(face) ?: 0f
        val roll  = headPoseEstimator.estimateRoll(face) ?: 0f

        if (pitchBase == null) pitchBase = pitch
        pitchBase = Config.EMA_ALPHA * pitch + (1 - Config.EMA_ALPHA) * (pitchBase ?: pitch)
        val pitchDev = pitch - (pitchBase ?: pitch)

        val now = System.currentTimeMillis()
        if (pitchDetector.update(pitchDev)) alerts.add("Fatigue Alert: Prolonged Head Tilt!")
        if (yawDetector.update(yaw)) alerts.add("Fatigue Alert: Prolonged Yaw!")
        if (rollDetector.update(roll)) alerts.add("Fatigue Alert: Prolonged Roll!")

        if (alerts.any { it.startsWith("Fatigue Alert") } && now - lastAlertTime > soundCooldown) {
            playAlertSound()
            lastAlertTime = now
        }

        return alerts
    }

    private fun playAlertSound() {
        if (mediaPlayer.isPlaying) return
        mediaPlayer.start()
    }

    fun release() {
        mediaPlayer.release()
        faceMeshProcessor.close()
    }
}