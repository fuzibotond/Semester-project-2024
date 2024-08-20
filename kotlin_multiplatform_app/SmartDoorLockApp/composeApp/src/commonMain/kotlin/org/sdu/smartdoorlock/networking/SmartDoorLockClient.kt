package networking

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.SerializationException
import org.sdu.smartdoorlock.networking.ChangeLockStateRequestBody
import org.sdu.smartdoorlock.networking.SmartDoorLockStateLog
import util.NetworkError
import util.Result

class SmartDoorLockClient(
    private val httpClient: HttpClient
) {
    suspend fun getHeartBeatLogs(): Result<HeartBeatLog?, NetworkError> {
        val response = try {
            httpClient.get(
                urlString = BASE_URL.plus("/heartbeatLogs")
            )
        } catch (e: UnresolvedAddressException) {
            return Result.Error(NetworkError.NO_INTERNET)
        } catch (e: SerializationException) {
            return Result.Error(NetworkError.SERIALIZATION)
        }

        return when (response.status.value) {
            in 200..299 -> {
                val smartDoorLockState = response.body<List<HeartBeatLog?>?>()
                Result.Success(smartDoorLockState?.lastOrNull())
            }

            401 -> Result.Error(NetworkError.UNAUTHORIZED)
            409 -> Result.Error(NetworkError.CONFLICT)
            408 -> Result.Error(NetworkError.REQUEST_TIMEOUT)
            413 -> Result.Error(NetworkError.PAYLOAD_TOO_LARGE)
            in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
            else -> Result.Error(NetworkError.UNKNOWN)
        }
    }

    suspend fun getCurrentState(): Result<SmartDoorLockStateLog?, NetworkError> {
        val response = try {
            httpClient.get(
                urlString = BASE_URL.plus("/stateLogs")
            )
        } catch (e: UnresolvedAddressException) {
            return Result.Error(NetworkError.NO_INTERNET)
        } catch (e: SerializationException) {
            return Result.Error(NetworkError.SERIALIZATION)
        }

        return when (response.status.value) {
            in 200..299 -> {
                val smartDoorLockState = response.body<List<SmartDoorLockStateLog?>?>()
                Result.Success(smartDoorLockState?.lastOrNull())
            }

            401 -> Result.Error(NetworkError.UNAUTHORIZED)
            409 -> Result.Error(NetworkError.CONFLICT)
            408 -> Result.Error(NetworkError.REQUEST_TIMEOUT)
            413 -> Result.Error(NetworkError.PAYLOAD_TOO_LARGE)
            in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
            else -> Result.Error(NetworkError.UNKNOWN)
        }
    }

    suspend fun changeLockState(command: String): Result<String, NetworkError> {
        val response = try {
            httpClient.post(
                urlString = BASE_URL.plus("/sendCommand")
            ) {
                contentType(ContentType.Application.Json)
                setBody(body = ChangeLockStateRequestBody(pin = "1234", command = command))
            }
        } catch (e: UnresolvedAddressException) {
            return Result.Error(NetworkError.NO_INTERNET)
        } catch (e: SerializationException) {
            return Result.Error(NetworkError.SERIALIZATION)
        }
        return when (response.status.value) {
            in 200..299 -> {
                val smartDoorLockState = response.body<String>()
                Result.Success(smartDoorLockState)
            }

            401 -> Result.Error(NetworkError.UNAUTHORIZED)
            409 -> Result.Error(NetworkError.CONFLICT)
            408 -> Result.Error(NetworkError.REQUEST_TIMEOUT)
            413 -> Result.Error(NetworkError.PAYLOAD_TOO_LARGE)
            in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
            else -> Result.Error(NetworkError.UNKNOWN)
        }
    }
}

const val BASE_URL = "https://aa9c-86-52-3-28.ngrok-free.app"
