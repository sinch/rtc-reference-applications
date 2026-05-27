package com.sinch.rtc.vvc.reference.app.domain.push

import com.sinch.rtc.vvc.reference.app.BuildConfig

enum class PushProvider(val displayName: String) {
    FCM("FCM (Firebase)"),
    HMS("HMS (Huawei)");

    companion object {
        fun availableProviders(): List<PushProvider> {
            val providers = mutableListOf<PushProvider>()
            if (BuildConfig.FCM_AVAILABLE) {
                providers.add(FCM)
            }
            if (BuildConfig.HMS_AVAILABLE) {
                providers.add(HMS)
            }
            return providers
        }

        fun defaultProvider(): PushProvider? = availableProviders().firstOrNull()

        fun fromName(name: String): PushProvider? = entries.firstOrNull { it.name == name }
    }
}
