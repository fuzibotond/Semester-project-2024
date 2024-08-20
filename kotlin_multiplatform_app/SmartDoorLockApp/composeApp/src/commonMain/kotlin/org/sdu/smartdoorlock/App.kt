package org.sdu.smartdoorlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import networking.HeartBeatLog
import networking.SmartDoorLockClient
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.sdu.smartdoorlock.networking.SmartDoorLockStateLog

import util.NetworkError
import util.onError
import util.onSuccess

@OptIn(FormatStringsInDatetimeFormats::class)
@Composable
@Preview
fun App(smartDoorLockClient: SmartDoorLockClient) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var lastHeartBeatLog by remember {
            mutableStateOf<HeartBeatLog?>(null)
        }
        var lastSmartDoorLockStateLog by remember {
            mutableStateOf<SmartDoorLockStateLog?>(null)
        }
        var currentSmartDoorLockState by remember {
            mutableStateOf(
                SmartDoorLockState(
                    doorState = DoorState.OPEN,
                    lockState = LockState.UNLOCKED
                )
            )
        }
        var commandText by remember {
            mutableStateOf<String?>(null)
        }
        var isLoading by remember {
            mutableStateOf(true)
        }
        var isChangingState by remember {
            mutableStateOf(false)
        }
        var errorMessage by remember {
            mutableStateOf<NetworkError?>(null)
        }
        LaunchedEffect(Unit) {
            isLoading = true
            errorMessage = null
            KMMTimer(
                name = "State update timer",
                interval = 3000L,
                delay = 0L
            ) {
                scope.launch {
                    smartDoorLockClient.getHeartBeatLogs().onSuccess {
                        lastHeartBeatLog = it
                    }.onError {
                        errorMessage = it
                    }
                    smartDoorLockClient.getCurrentState().onSuccess {
                        lastSmartDoorLockStateLog = it
                        currentSmartDoorLockState = it?.message?.getSmartDoorLockState() ?: SmartDoorLockState(DoorState.OPEN, LockState.UNLOCKED)
                    }.onError {
                        errorMessage = it
                    }
                    isLoading = false
                }
            }.start()
        }
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Current door state:",
                                fontSize = 32.sp,
                            )
                            Text(
                                text = if (currentSmartDoorLockState.doorState == DoorState.OPEN) {
                                    "OPEN"
                                } else {
                                    "CLOSED"
                                },
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentSmartDoorLockState.doorState == DoorState.OPEN) {
                                    Color.Green
                                } else {
                                    Color.Red
                                }
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Current lock state:",
                                fontSize = 32.sp,
                            )
                            Text(
                                text = if (currentSmartDoorLockState.lockState == LockState.UNLOCKED) {
                                    "UNLOCKED"
                                } else {
                                    "LOCKED"
                                },
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentSmartDoorLockState.lockState == LockState.UNLOCKED) {
                                    Color.Green
                                } else {
                                    Color.Red
                                }
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(2f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Button(
                                shape = CircleShape,
                                modifier = Modifier
                                    .height(60.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 64.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (currentSmartDoorLockState.lockState == LockState.UNLOCKED) {
                                        Color.Red
                                    } else {
                                        Color.Green
                                    },
                                    contentColor = Color.White
                                ),
                                enabled = lastHeartBeatLog?.message?.contains("OK") ?: false,
                                onClick = {
                                    scope.launch {
                                        isChangingState = true
                                        errorMessage = null

                                        val command = if (currentSmartDoorLockState.lockState == LockState.UNLOCKED) {
                                            "lock"
                                        } else {
                                            "unlock"
                                        }
                                        smartDoorLockClient.apply {
                                            changeLockState(
                                                command = command
                                            )
                                                .onSuccess { commandResponseText ->
                                                    commandText = "'$command' $commandResponseText"
                                                    getCurrentState()
                                                        .onSuccess {
                                                            lastSmartDoorLockStateLog = it
                                                            currentSmartDoorLockState = it?.message?.getSmartDoorLockState() ?: SmartDoorLockState(DoorState.OPEN, LockState.UNLOCKED)
                                                        }
                                                        .onError {
                                                            errorMessage = it
                                                        }
                                                }
                                                .onError {
                                                    commandText = "Error while sending the command!"
                                                    errorMessage = it
                                                }

                                        }
                                        isChangingState = false
                                    }
                                }
                            ) {
                                if (isChangingState) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(24.dp),
                                        strokeWidth = 3.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = if (currentSmartDoorLockState.lockState == LockState.UNLOCKED) {
                                            "LOCK"
                                        } else {
                                            "UNLOCK"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            commandText?.let {
                                Text(text = it)
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    lastHeartBeatLog?.let {
                        val formatPattern = "dd.MM.yyyy - HH:mm:ss"
                        val dateTimeFormat = LocalDateTime.Format {
                            byUnicodePattern(formatPattern)
                        }
                        val formattedDate = dateTimeFormat.format(it.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (errorMessage == null) {
                                Text(
                                    text = "ESP32 status: ${it.message}",
                                    fontStyle = FontStyle.Italic
                                )
                                Text(
                                    text = "Last check: $formattedDate",
                                    fontStyle = FontStyle.Italic
                                )
                            } else {
                                Text(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    text = "Error while communicating the server and the ESP, cannot send commands",
                                    textAlign = TextAlign.Center,
                                    color = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class DoorState {
    OPEN,
    CLOSED
}

enum class LockState {
    UNLOCKED,
    LOCKED
}

data class SmartDoorLockState(
    val doorState: DoorState,
    val lockState: LockState
)

private fun String.getSmartDoorLockState(): SmartDoorLockState {
    println("SmartDoorLockState: $this")
    return SmartDoorLockState(
        doorState = if (this.contains("door:true")) {
            DoorState.OPEN
        } else {
            DoorState.CLOSED
        }, lockState = if (this.contains("lock:true")) {
            LockState.UNLOCKED
        } else {
            LockState.LOCKED
        }
    )
}