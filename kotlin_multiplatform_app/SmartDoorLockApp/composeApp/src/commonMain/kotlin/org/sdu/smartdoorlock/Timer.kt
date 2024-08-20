package org.sdu.smartdoorlock

expect class KMMTimer(
    name: String? = null,
    interval: Long,
    delay: Long,
    action: () -> Unit
) {
    val name: String?
    val interval: Long
    val delay: Long

    fun start()
    fun cancel()
    fun isRunning(): Boolean
}