package org.sdu.smartdoorlock

import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSTimer

actual class KMMTimer actual constructor(
    actual val name: String?,
    actual val interval: Long,
    actual val delay: Long,
    action: () -> Unit
) {
    private var timer: NSTimer? = null
    private var action = action

    actual fun start() {
        if (!isRunning()) {
            timer = NSTimer(
                fireDate = NSDate(
                    NSDate().timeIntervalSinceReferenceDate + (delay.toDouble() / 1000)
                ),
                interval = (interval.toDouble() / 1000),
                repeats = true,
                block = {
                    action()
                }
            )
            timer?.let {
                NSRunLoop.currentRunLoop().addTimer(it, NSRunLoopCommonModes)
            }
        }
    }

    actual fun cancel() {
        timer?.invalidate()
        timer = null
    }

    actual fun isRunning(): Boolean {
        return timer != null
    }
}