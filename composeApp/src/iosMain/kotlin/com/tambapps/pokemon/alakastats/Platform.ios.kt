package com.tambapps.pokemon.alakastats

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val type: PlatformType = PlatformType.Ios
}

actual fun getPlatform(): Platform = IOSPlatform()