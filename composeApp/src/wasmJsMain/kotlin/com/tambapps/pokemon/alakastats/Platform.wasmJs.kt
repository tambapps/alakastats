package com.tambapps.pokemon.alakastats

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val type: PlatformType = PlatformType.Web
}

actual fun getPlatform(): Platform = WasmPlatform()