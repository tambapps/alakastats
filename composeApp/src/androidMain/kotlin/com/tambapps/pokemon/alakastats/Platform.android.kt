package com.tambapps.pokemon.alakastats

import android.os.Build

object AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val type: PlatformType = PlatformType.Android
    override val deviceType: DeviceType = DeviceType.Android
}

actual fun getPlatform(): Platform = AndroidPlatform