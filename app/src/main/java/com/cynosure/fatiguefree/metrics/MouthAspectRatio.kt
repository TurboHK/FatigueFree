package com.cynosure.fatiguefree.metrics

import android.graphics.PointF
import kotlin.math.hypot

object MouthAspectRatio {

    fun compute(mouthLandmarks: Map<String, PointF>): Double {
        val upper = mouthLandmarks["upper"] ?: return 0.0
        val lower = mouthLandmarks["lower"] ?: return 0.0
        val left = mouthLandmarks["left"] ?: return 0.0
        val right = mouthLandmarks["right"] ?: return 0.0

        val vertical = euclideanDistance(upper, lower)
        val horizontal = euclideanDistance(left, right)

        return if (horizontal != 0.0) vertical / horizontal else 0.0
    }

    private fun euclideanDistance(p1: PointF, p2: PointF): Double {
        return hypot((p1.x - p2.x).toDouble(), (p1.y - p2.y).toDouble())
    }
}
