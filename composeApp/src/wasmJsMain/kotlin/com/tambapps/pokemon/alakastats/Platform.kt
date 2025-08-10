package com.tambapps.pokemon.alakastats

class WasmPlatform : Platform {
    override val name = "WebAssembly (WASM)"
    override val type = PlatformType.Web
}

actual fun getPlatform(): Platform = WasmPlatform()