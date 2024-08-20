package org.sdu.smartdoorlock

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform