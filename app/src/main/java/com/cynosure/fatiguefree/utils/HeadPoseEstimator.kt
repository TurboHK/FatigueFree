package com.cynosure.fatiguefree.utils


import com.google.mediapipe.formats.proto.LandmarkProto
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class HeadPoseEstimator {

    private val modelPoints: Mat = MatOfPoint3f(
        Point3(0.0, 0.0, 0.0),           // Nose tip
        Point3(0.0, -63.6, -12.5),       // Chin
        Point3(-43.3, 32.7, -26.0),      // Left eye left corner
        Point3(43.3, 32.7, -26.0),       // Right eye right corner
        Point3(-28.9, -28.9, -24.1),     // Left mouth corner
        Point3(28.9, -28.9, -24.1)       // Right mouth corner
    )

    private val landmarkIndices = listOf(1, 152, 33, 263, 61, 291)

    fun estimateAngles(landmarks: LandmarkProto.NormalizedLandmarkList, imageWidth: Int, imageHeight: Int): Triple<Float, Float, Float>? {
        val imagePoints = MatOfPoint2f()
        val pointsList = landmarkIndices.map { idx ->
            val lm = landmarks.getLandmark(idx)
            Point(lm.x * imageWidth, lm.y * imageHeight)
        }
        imagePoints.fromList(pointsList)

        val focalLength = imageWidth.toDouble()
        val center = Point(imageWidth / 2.0, imageHeight / 2.0)

        val cameraMatrix = Mat(3, 3, CvType.CV_64FC1).apply {
            put(0, 0, focalLength, 0.0, center.x)
            put(1, 0, 0.0, focalLength, center.y)
            put(2, 0, 0.0, 0.0, 1.0)
        }

        val distCoeffs = Mat.zeros(4, 1, CvType.CV_64FC1)
        val rvec = Mat()
        val tvec = Mat()

        val success = Calib3d.solvePnP(modelPoints, imagePoints, cameraMatrix, distCoeffs, rvec, tvec)
        if (!success) return null

        val rotMat = Mat()
        Calib3d.Rodrigues(rvec, rotMat)

        val projMatrix = Mat(3, 4, CvType.CV_64FC1)
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                projMatrix.put(i, j, rotMat.get(i, j)[0])
            }
            projMatrix.put(i, 3, tvec.get(i, 0)[0])
        }

        val outEuler = Mat()
        Calib3d.decomposeProjectionMatrix(projMatrix, Mat(), Mat(), Mat(), Mat(), Mat(), outEuler)

        val pitch = outEuler.get(0, 0)[0].toFloat()
        val yaw   = outEuler.get(1, 0)[0].toFloat()
        val roll  = outEuler.get(2, 0)[0].toFloat()

        return Triple(pitch, yaw, roll)
    }

    fun estimatePitch(landmarks: LandmarkProto.NormalizedLandmarkList): Float? {
        return estimateAngles(landmarks, 640, 480)?.first
    }

    fun estimateYaw(landmarks: LandmarkProto.NormalizedLandmarkList): Float? {
        return estimateAngles(landmarks, 640, 480)?.second
    }

    fun estimateRoll(landmarks: LandmarkProto.NormalizedLandmarkList): Float? {
        return estimateAngles(landmarks, 640, 480)?.third
    }
}
