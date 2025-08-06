package com.cynosure.fatiguefree.detection

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facemesh.FaceMesh
import com.google.mediapipe.tasks.vision.facemesh.FaceMeshResult
import com.google.mediapipe.tasks.vision.facemesh.FaceMeshOptions
import java.io.Closeable

/**
 * FaceMeshProcessor
 * 封装 MediaPipe FaceMesh 处理流程
 */
class FaceMeshProcessor(private val context: Context) : Closeable {

    private val faceMesh: FaceMesh

    init {
        val baseOptions = FaceMeshOptions.builder()
            .setRunningMode(RunningMode.IMAGE)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFaceTrackingConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setNumFaces(1)
            .build()

        faceMesh = FaceMesh.createFromOptions(context, baseOptions)
    }

    /**
     * 处理单帧图片，返回 FaceMeshResult
     */
    fun processFrame(bitmap: Bitmap): FaceMeshResult? {
        return try {
            faceMesh.detect(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 返回第一个检测到的人脸关键点数据
     */
    fun detectSingleFace(bitmap: Bitmap): LandmarkProto.NormalizedLandmarkList? {
        val result = processFrame(bitmap)
        return if (!result?.multiFaceLandmarksList.isNullOrEmpty()) {
            result!!.multiFaceLandmarksList[0]
        } else {
            null
        }
    }

    override fun close() {
        try {
            faceMesh.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
