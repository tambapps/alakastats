package com.tambapps.pokemon.alakastats

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val type: PlatformType = PlatformType.Jvm
}

actual fun getPlatform(): Platform = AndroidPlatform()