package org.sdu.smartdoorlock

import java.util.Timer
import kotlin.concurrent.fixedRateTimer

actual class KMMTimer actual constructor(
    actual val name: String?,
    actual val interval: Long,
    actual val delay: Long,
    action: () -> Unit
) {
    private var timer: Timer? = null
    private val action = action

    actual fun start() {
        if (!isRunning()) {
            timer = fixedRateTimer(
                name = name,
                initialDelay = delay,
                period = interval
            ) {
                action()
            }
        }
    }

    actual fun cancel() {
        timer?.cancel()
        timer = null
    }

    actual fun isRunning(): Boolean {
        return timer != null
    }
}