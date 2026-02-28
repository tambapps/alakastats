package com.tambapps.pokemon.alakastats

import platform.UIKit.UIDevice

object IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val type: PlatformType = PlatformType.Ios
    override val deviceType: DeviceType = DeviceType.Ios
}

actual fun getPlatform(): Platform = IOSPlatform