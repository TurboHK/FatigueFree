package com.cynosure.fatiguefree.detection

import com.cynosure.fatiguefree.utils.Config

class YawnDetector(
    private val threshold: Double = Config.MOUTH_AR_THRESHOLD,
    private val sustainTimeSec: Float = Config.SUSTAINED_TIME_THRESHOLD,
    private val displayTimeSec: Float = Config.ALERT_DISPLAY_DURATION
) {

    private var startTimeMs: Long? = null
    private var alerted = false
    private var alertTimeMs: Long? = null

    /**
     * @param mar 当前帧的 MAR 值
     * @param nowMs 当前时间戳（单位：毫秒）
     * @return 是否显示 yawn 警告（true = 是）
     */
    fun update(mar: Double, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (mar > threshold) {
            if (startTimeMs == null) {
                startTimeMs = nowMs
            } else if ((nowMs - startTimeMs!!) >= sustainTimeSec * 1000 && !alerted) {
                alerted = true
                alertTimeMs = nowMs
            }
        } else {
            startTimeMs = null
            alerted = false
            alertTimeMs = null
        }

        return if (alerted) {
            if ((nowMs - (alertTimeMs ?: nowMs)) <= displayTimeSec * 1000) {
                true
            } else {
                alerted = false
                false
            }
        } else {
            false
        }
    }
}
