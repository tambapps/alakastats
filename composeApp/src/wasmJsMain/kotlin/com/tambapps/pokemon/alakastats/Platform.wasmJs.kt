package com.tambapps.pokemon.alakastats

import kotlinx.browser.window

object WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val type: PlatformType = PlatformType.Web
    override val deviceType: DeviceType = detectDeviceCategory()
}

private fun detectDeviceCategory(): DeviceType {
    val userAgent = window.navigator.userAgent
    return when {
        userAgent.contains("Android", ignoreCase = true) -> DeviceType.Android
        userAgent.contains("iPhone", ignoreCase = true) ||
        userAgent.contains("iPad", ignoreCase = true) -> DeviceType.Ios
        else -> DeviceType.Desktop
    }
}

actual val platform: Platform = WasmPlatform
