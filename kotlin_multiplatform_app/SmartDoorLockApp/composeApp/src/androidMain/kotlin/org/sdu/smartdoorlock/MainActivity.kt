package org.sdu.smartdoorlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import io.ktor.client.engine.okhttp.OkHttp
import networking.SmartDoorLockClient
import networking.createHttpClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App(
                smartDoorLockClient = remember {
                    SmartDoorLockClient(
                        createHttpClient(
                            engine = OkHttp.create()
                        )
                    )
                }
            )
        }
    }
}