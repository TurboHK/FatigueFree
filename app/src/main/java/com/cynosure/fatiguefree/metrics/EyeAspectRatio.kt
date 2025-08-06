package com.cynosure.fatiguefree.metrics

import android.graphics.PointF
import kotlin.math.hypot

object EyeAspectRatio {
    fun compute(eyeLandmarks: List<PointF>): Float {
        require(eyeLandmarks.size >= 6) { "Need 6 eye landmarks" }

        val p1 = eyeLandmarks[0]
        val p2 = eyeLandmarks[1]
        val p3 = eyeLandmarks[2]
        val p4 = eyeLandmarks[3]
        val p5 = eyeLandmarks[4]
        val p6 = eyeLandmarks[5]

        val vertical1 = euclideanDistance(p2, p6)
        val vertical2 = euclideanDistance(p3, p5)
        val horizontal = euclideanDistance(p1, p4)

        return (vertical1 + vertical2) / (2.0f * horizontal)
    }

    private fun euclideanDistance(p1: PointF, p2: PointF): Float {
        return hypot(p1.x - p2.x, p1.y - p2.y)
    }
}
