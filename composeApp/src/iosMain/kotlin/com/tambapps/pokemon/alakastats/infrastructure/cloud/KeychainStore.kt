package com.tambapps.pokemon.alakastats.infrastructure.cloud

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Stores small secrets (the OAuth refresh token) in the iOS Keychain
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainStore(private val service: String) {

    fun get(key: String): String? = withCfValues(key) { account, serviceRef ->
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceRef,
            kSecAttrAccount to account,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        try {
            memScoped {
                val result = alloc<CFTypeRefVar>()
                if (SecItemCopyMatching(query, result.ptr) != errSecSuccess) return@memScoped null
                val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
                NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
            }
        } finally {
            CFRelease(query)
        }
    }

    fun set(key: String, value: String) {
        delete(key)
        @Suppress("CAST_NEVER_SUCCEEDS")
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val dataRef = CFBridgingRetain(data)
        withCfValues(key) { account, serviceRef ->
            val query = cfDictionaryOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceRef,
                kSecAttrAccount to account,
                kSecValueData to dataRef,
                kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock,
            )
            SecItemAdd(query, null)
            CFRelease(query)
        }
        CFRelease(dataRef)
    }

    fun delete(key: String) = withCfValues(key) { account, serviceRef ->
        val query = cfDictionaryOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceRef,
            kSecAttrAccount to account,
        )
        SecItemDelete(query)
        CFRelease(query)
    }

    private inline fun <T> withCfValues(key: String, block: (account: CFTypeRef?, service: CFTypeRef?) -> T): T {
        val account = CFBridgingRetain(key)
        val serviceRef = CFBridgingRetain(service)
        try {
            return block(account, serviceRef)
        } finally {
            CFRelease(account)
            CFRelease(serviceRef)
        }
    }

    private fun cfDictionaryOf(vararg items: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
        val dictionary = CFDictionaryCreateMutable(
            null,
            items.size.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        )
        items.forEach { (key, value) -> CFDictionaryAddValue(dictionary, key, value) }
        return dictionary
    }
}
