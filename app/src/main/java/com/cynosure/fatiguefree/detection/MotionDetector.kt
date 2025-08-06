package com.cynosure.fatiguefree.detection

import com.cynosure.fatiguefree.utils.Config
import kotlin.math.abs

class MotionDetector(
    private val threshold: Float,
    private val sustainTimeSec: Float = Config.SUSTAINED_TIME_THRESHOLD,
    private val displayTimeSec: Float = Config.ALERT_DISPLAY_DURATION
) {

    private var startTimeMs: Long? = null
    private var triggered = false
    private var triggerTimeMs: Long? = null

    /**
     * @param value 当前姿态偏移（pitch/yaw/roll）
     * @param nowMs 当前时间戳，单位：毫秒
     * @return 是否触发持续偏移警报（true: 显示警告）
     */
    fun update(value: Float, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (abs(value) > threshold) {
            if (startTimeMs == null) {
                startTimeMs = nowMs
            } else if ((nowMs - startTimeMs!!) >= sustainTimeSec * 1000 && !triggered) {
                triggered = true
                triggerTimeMs = nowMs
            }
        } else {
            startTimeMs = null
            triggered = false
        }

        return if (triggered) {
            if ((nowMs - (triggerTimeMs ?: nowMs)) <= displayTimeSec * 1000) {
                true
            } else {
                triggered = false
                false
            }
        } else {
            false
        }
    }
}
