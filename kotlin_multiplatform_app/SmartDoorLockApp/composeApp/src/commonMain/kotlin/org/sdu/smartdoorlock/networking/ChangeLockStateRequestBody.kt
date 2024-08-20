package org.sdu.smartdoorlock.networking

import kotlinx.serialization.Serializable

@Serializable
data class ChangeLockStateRequestBody(
    val pin: String,
    val command: String
)