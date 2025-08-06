package com.cynosure.fatiguefree.detection

import com.cynosure.fatiguefree.utils.Config
import kotlin.math.roundToInt

class BlinkDetector(
    private val emaAlpha: Double = Config.EMA_ALPHA,
    private val thresholdDelta: Double = Config.THRESHOLD_DELTA,
    private val hysteresisFrames: Int = Config.HYSTERESIS_FRAMES,
    private val minBlinkDuration: Double = Config.MIN_BLINK_DURATION,
    private val blinkRateWindowSec: Int = Config.BLINK_RATE_WINDOW
) {

    private var currentEma: Double? = null
    private var closedFrames = 0
    var blinkStart: Long? = null
        private set
    private val blinkTimestamps = mutableListOf<Long>()
    var blinkCount = 0
        private set

    /**
     * @param ear 当前帧的 EAR 值
     * @param nowMs 当前时间戳（可选，单位毫秒）
     * @return Triple<阈值, 当前 blink 频率, 警告信息列表>
     */
    fun update(ear: Double, nowMs: Long = System.currentTimeMillis()): Triple<Double, Double, List<String>> {
        val messages = mutableListOf<String>()

        // 1. 初始化 EMA
        if (currentEma == null) {
            currentEma = ear
        }

        val provisionalThresh = currentEma!! - thresholdDelta

        // 2. 仅在眼睛睁开时更新 EMA
        if (ear >= provisionalThresh) {
            currentEma = emaAlpha * ear + (1.0 - emaAlpha) * currentEma!!
        }

        val dynamicThresh = currentEma!! - thresholdDelta

        // 3. Blink 检测（带迟滞）
        if (ear < dynamicThresh) {
            // 眼睛闭合中
            if (blinkStart == null) {
                blinkStart = nowMs
            }
            closedFrames++
        } else {
            // 眼睛睁开了
            if (closedFrames >= hysteresisFrames) {
                val durationSec = (nowMs - (blinkStart ?: nowMs)) / 1000.0
                blinkCount++
                blinkTimestamps.add(nowMs)

                var msg = "Blink detected! Duration: %.3f sec | Total Blinks: $blinkCount".format(durationSec)
                if (durationSec >= minBlinkDuration) {
                    msg += " - Fatigue Alert: Prolonged Blink!"
                }
                messages.add(msg)
            }
            closedFrames = 0
            blinkStart = null
        }

        // 4. Blink 频率（滑动时间窗）
        val cutoff = nowMs - blinkRateWindowSec * 1000
        blinkTimestamps.removeAll { it < cutoff }
        val rate = blinkTimestamps.size * (60.0 / blinkRateWindowSec)
        messages.add("Blink Rate: %.1f blinks/min".format(rate))

        if (rate > 19.0) {
            messages.add("Fatigue Alert: High Blink Rate!")
        }

        return Triple(dynamicThresh, rate, messages)
    }
}
