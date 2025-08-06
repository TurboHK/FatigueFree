package com.cynosure.fatiguefree.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import com.cynosure.fatiguefree.metrics.EyeAspectRatio
import com.cynosure.fatiguefree.metrics.MouthAspectRatio
import com.cynosure.fatiguefree.utils.Config
import com.cynosure.fatiguefree.utils.HeadPoseEstimator
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import kotlin.math.pow
import kotlin.math.sqrt
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark


class CalibrationManager(
    private val context: Context,
    private val durationSec: Int = 10
) {
    private val faceMeshProcessor = FaceMeshProcessor(context)
    private val headPoseEstimator = HeadPoseEstimator()

    private val LEFT_EYE_IDX = listOf(33, 160, 158, 133, 153, 144)
    private val RIGHT_EYE_IDX = listOf(263, 387, 385, 362, 380, 373)

    private val MOUTH_IDX = mapOf(
        "upper" to 13,
        "lower" to 14,
        "left" to 78,
        "right" to 308
    )

    suspend fun runCalibration(frameProvider: suspend () -> Bitmap): JSONObject {
        val ears = mutableListOf<Double>()
        val pitchDeviations = mutableListOf<Double>()
        val blinkTimestamps = mutableListOf<Long>()

        val startTime = System.currentTimeMillis()
        val durationMillis = durationSec * 1000
        var closed = false

        while (System.currentTimeMillis() - startTime < durationMillis) {
            val bitmap = frameProvider()
            val landmarks: List<NormalizedLandmark> = faceMeshProcessor.detectSingleFace(bitmap) ?: continue


            val width = bitmap.width.toFloat()
            val height = bitmap.height.toFloat()

            // 👉 提取左眼和右眼 landmarks（PointF 列表）

            val leftEye = LEFT_EYE_IDX.map { idx ->
                val lm = landmarks[idx]
                PointF(lm.x * width, lm.y * height)
            }

            val rightEye = RIGHT_EYE_IDX.map { idx ->
                val lm = landmarks[idx]
                PointF(lm.x * width, lm.y * height)
            }

            val ear = (EyeAspectRatio.compute(leftEye) + EyeAspectRatio.compute(rightEye)) / 2.0
            ears.add(ear)

            // 👉 Blink detection
            val minEar = ears.minOrNull() ?: continue
            val maxEar = ears.maxOrNull() ?: continue
            val threshold = minEar + (maxEar - minEar) * 0.5

            if (ear < threshold && !closed) {
                blinkTimestamps.add(System.currentTimeMillis())
                closed = true
            } else if (ear >= threshold) {
                closed = false
            }

            // 👉 Head pose
            val pitch = headPoseEstimator.estimatePitch(landmarkList) ?: continue
            pitchDeviations.add(pitch.toDouble())
        }

        val avgEar = ears.average()
        val earStd = sqrt(ears.map { (it - avgEar).pow(2) }.average())
        val blinkRate = blinkTimestamps.size.toDouble() / (durationSec / 60.0)

        val avgPitch = pitchDeviations.average()
        val pitchStd = sqrt(pitchDeviations.map { (it - avgPitch).pow(2) }.average())

        // ✅ Build calibration profile
        val profile = JSONObject().apply {
            put("blink", JSONObject().apply {
                put("alpha", Config.EMA_ALPHA)
                put("threshold_delta", earStd * 0.5)
                put("hysteresis_frames", (earStd * 30).toInt().coerceAtLeast(3))
            })

            put("head_pose", JSONObject().apply {
                put("pitch_threshold", avgPitch + 2 * pitchStd)
                put("sustain_time", 3.0)
            })

            put("baseline", JSONObject().apply {
                put("avg_ear", avgEar)
                put("avg_pitch", avgPitch)
                put("blink_rate", blinkRate)
            })
        }

        val file = File(context.filesDir, "user_profile.json")
        FileWriter(file).use { it.write(profile.toString(2)) }

        return profile
    }

    fun loadOrRunCalibration(frameProvider: suspend () -> Bitmap): JSONObject {
        val file = File(context.filesDir, "user_profile.json")
        return if (file.exists()) {
            JSONObject(file.readText())
        } else {
            kotlinx.coroutines.runBlocking {
                runCalibration(frameProvider)
            }
        }
    }
}
