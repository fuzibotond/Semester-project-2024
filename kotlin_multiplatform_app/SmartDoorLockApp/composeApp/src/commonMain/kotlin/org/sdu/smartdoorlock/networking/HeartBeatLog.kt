package networking

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class HeartBeatLog(
    val timestamp: Instant,
    val message: String
)
