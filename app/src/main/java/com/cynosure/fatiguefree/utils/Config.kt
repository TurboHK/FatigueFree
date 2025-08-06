package com.cynosure.fatiguefree.utils

object Config {

    // EAR 参数
    const val EMA_ALPHA = 0.1
    const val THRESHOLD_DELTA = 0.02
    const val HYSTERESIS_FRAMES = 3
    const val MIN_BLINK_DURATION = 0.2
    const val BLINK_RATE_WINDOW = 60  // seconds

    // MAR 参数
    const val MOUTH_AR_THRESHOLD = 0.6

    // 姿态检测
    const val PITCH_THRESHOLD = 20f
    const val YAW_THRESHOLD = 20f
    const val ROLL_THRESHOLD = 20f
    const val SUSTAINED_TIME_THRESHOLD = 3.0f
    const val ALERT_DISPLAY_DURATION = 1.0f

    // EAR/MAR 人脸关键点索引（MediaPipe 468-landmark format）
    val LEFT_EYE_IDX = listOf(33, 160, 158, 133, 153, 144)
    val RIGHT_EYE_IDX = listOf(263, 387, 385, 362, 380, 373)

    val MOUTH_INDICES = mapOf(
        "upper" to 13,
        "lower" to 14,
        "left" to 78,
        "right" to 308
    )

    // solvePnP 姿态估计所需的 landmark 索引
    val LANDMARK_INDICES = listOf(1, 152, 33, 263, 61, 291)

    // solvePnP 所用的模型三维点位
    val MODEL_POINTS = arrayOf(
        doubleArrayOf(0.0,   0.0,    0.0),     // nose
        doubleArrayOf(0.0, -63.6, -12.5),      // chin
        doubleArrayOf(-43.3, 32.7, -26.0),     // left eye
        doubleArrayOf(43.3,  32.7, -26.0),     // right eye
        doubleArrayOf(-28.9,-28.9, -24.1),     // left mouth
        doubleArrayOf(28.9, -28.9, -24.1)      // right mouth
    )

    // 配置文件名（可用于存储用户校准数据）
    const val PROFILE_FILENAME = "user_profile.json"
}
