package org.sdu.smartdoorlock.networking

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SmartDoorLockStateLog (
    val timestamp: Instant,
    val message: String
)