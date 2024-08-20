package org.sdu.smartdoorlock

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import io.ktor.client.engine.darwin.Darwin
import networking.SmartDoorLockClient
import networking.createHttpClient

fun MainViewController() = ComposeUIViewController {
    App(
        smartDoorLockClient = remember {
            SmartDoorLockClient(createHttpClient(Darwin.create()))
        }
    )
}